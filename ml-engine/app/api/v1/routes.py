from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException
import asyncio
import httpx
import os
import time
import tempfile
import shutil

from app.models.schemas import AudioProcessRequest, SearchRequest, SearchResponse, SearchResultItem
from app.services.inference import MLService
from app.services.vector_store import QdrantService
from app.services.s3_service import S3Service
from app.core.dependencies import get_ml_service, get_qdrant_service, get_s3_service

router = APIRouter()


def process_media_task(
    video_id: str,
    audio_key: str,
    frames_prefix: str,
    bucket_name: str,
    ml: MLService,
    qdrant: QdrantService,
    s3: S3Service
):
    """
    Обработка полного медиа-пайплайна:
    1) Скачать и транскрибировать аудио (Whisper) → сохранить эмбеддинги в audio_collection.
    2) Скачать кадры по frames_prefix из MinIO → получить CLIP-эмбеддинги → сохранить в frames_collection.
    """
    temp_dir = tempfile.mkdtemp(prefix=f"omnisearch_{video_id}_")
    temp_audio_path = os.path.join(temp_dir, "audio.wav")

    try:
        # Аудио
        print(f"[{video_id}] Downloading audio: {audio_key}")
        s3.download_file(bucket_name=bucket_name, object_key=audio_key, local_path=temp_audio_path)

        print(f"[{video_id}] Transcribing audio with Whisper...")
        chunks = ml.process_audio_to_chunks(temp_audio_path)

        print(f"[{video_id}] Generating text embeddings for {len(chunks)} chunks...")
        vectors = [ml.get_embedding(chunk["text"], is_query=False) for chunk in chunks]
        saved_audio = qdrant.upsert_chunks(video_id=video_id, chunks=chunks, vectors=vectors)
        print(f"[{video_id}] Audio processed: {saved_audio} chunks saved.")

        # Кадры (CLIP)
        print(f"[{video_id}] Listing frames with prefix: {frames_prefix}")
        frame_keys = s3.list_objects(bucket_name=bucket_name, prefix=frames_prefix)
        # только .jpg
        frame_keys = [k for k in frame_keys if k.lower().endswith(".jpg")]
        print(f"[{video_id}] Found {len(frame_keys)} frame(s).")

        if frame_keys:
            _process_frames(video_id, frame_keys, bucket_name, temp_dir, ml, qdrant, s3)

        _send_status_callback(video_id, {"status": "READY"})

    except Exception as e:
        print(f"[{video_id}] Error processing media: {e}")
        _send_status_callback(video_id, {"status": "ERROR", "error": str(e)})
        raise
    finally:
        # Очистка временных файлов
        shutil.rmtree(temp_dir, ignore_errors=True)


def _process_frames(
    video_id: str,
    frame_keys: list[str],
    bucket_name: str,
    temp_dir: str,
    ml: MLService,
    qdrant: QdrantService,
    s3: S3Service,
):
    """Стриминговая обработка кадров батчами."""
    BATCH_SIZE = 64  # подобрано под 16GB VRAM

    frames_dir = os.path.join(temp_dir, "frames")
    os.makedirs(frames_dir, exist_ok=True)

    batch_meta: list[dict] = []
    batch_paths: list[str] = []
    total_saved = 0
    skipped = 0

    def flush_batch():
        nonlocal total_saved
        if not batch_paths:
            return
        vectors = ml.get_image_embeddings_batch(batch_paths)
        saved = qdrant.upsert_frames(video_id=video_id, frames=batch_meta, vectors=vectors)
        total_saved += saved
        for p in batch_paths:
            try:
                os.remove(p)
            except OSError as e:
                print(f"[{video_id}] Failed to remove {p}: {e}")
        batch_meta.clear()
        batch_paths.clear()

    try:
        for key in sorted(frame_keys):
            filename = os.path.basename(key)
            timecodes = MLService.parse_frame_timecodes(filename)
            if timecodes is None:
                print(f"[{video_id}] Skipping frame with unparseable name: {filename}")
                skipped += 1
                continue

            local_path = os.path.join(frames_dir, filename)
            s3.download_file(bucket_name=bucket_name, object_key=key, local_path=local_path)

            batch_meta.append({
                "frame_index": timecodes["index"],
                "start_time": timecodes["start_time"],
                "end_time": timecodes["end_time"],
                "frame_key": key,
            })
            batch_paths.append(local_path)

            if len(batch_paths) >= BATCH_SIZE:
                print(f"[{video_id}] Flushing batch ({len(batch_paths)} frames)...")
                flush_batch()

        # Хвост последнего неполного батча.
        if batch_paths:
            print(f"[{video_id}] Flushing final batch ({len(batch_paths)} frames)...")
            flush_batch()

    finally:
        batch_meta.clear()
        batch_paths.clear()

    if total_saved == 0 and skipped == len(frame_keys):
        print(f"[{video_id}] No valid frames to process.")
        return

    print(f"[{video_id}] Frames processed: {total_saved} embeddings saved "
          f"({skipped} skipped).")


def _send_status_callback(video_id: str, payload: dict):
    """Отправляет статус обработки видео обратно в бэкенд с несколькими попытками при неудаче."""
    backend_url = os.getenv("BACKEND_URL", "http://backend:8000")
    url = f"{backend_url}/api/v1/internal/videos/{video_id}"
    for attempt in range(1, 4):
        try:
            with httpx.Client(timeout=10.0) as client:
                response = client.patch(url, json=payload, headers={"X-Internal-Secret": str(os.getenv("INTERNAL_API_SECRET"))})
                response.raise_for_status()
                print(f"Callback sent for video {video_id}: {payload}")
                return
        except httpx.HTTPError as e:
            print(f"Callback attempt {attempt}/3 failed for video {video_id}: {e}")
            if attempt == 3:
                print(f"All callback attempts exhausted for video {video_id}.")
            time.sleep(2 ** attempt)


# Обработка медиа (аудио + кадры)
@router.post("/process", status_code=202)
async def process_media(
    request: AudioProcessRequest,
    background_tasks: BackgroundTasks,
    ml_service: MLService = Depends(get_ml_service),
    qdrant_service: QdrantService = Depends(get_qdrant_service),
    s3_service: S3Service = Depends(get_s3_service)
):
    background_tasks.add_task(
        asyncio.to_thread,
        process_media_task,
        request.video_id,
        request.audio_key,
        request.frames_prefix,
        request.bucket_name,
        ml_service,
        qdrant_service,
        s3_service
    )
    return {"status": "accepted", "video_id": request.video_id}


RRF_K = 60  # каноническое значение коэффициента сглаживания для RRF


def _intervals_overlap(a_start: float, a_end: float,
                       b_start: float, b_end: float) -> bool:
    """True, если интервалы пересекаются."""
    return a_start <= b_end and b_start <= a_end


def _merge_hit_into_buckets(
    buckets: list[dict],
    video_id: str,
    start_time: float,
    end_time: float,
    rank: int,
    source: str,
    text_snippet: str | None,
):
    """Найти существующий бакет с пересекающимся интервалом по тому же видео
    и добавить туда вклад этого хита; иначе — создать новый бакет.
    """
    for bucket in buckets:
        if bucket["video_id"] != video_id:
            continue
        if not _intervals_overlap(bucket["start_time"], bucket["end_time"], start_time, end_time):
            continue
        # Сливаем хит в существующий бакет.
        bucket["rrf_score"] += 1.0 / (RRF_K + rank)
        bucket["start_time"] = min(bucket["start_time"], start_time)
        bucket["end_time"] = max(bucket["end_time"], end_time)
        bucket["sources"].add(source)
        # Текстовый сниппет берём только из аудио-хита и только если его ещё нет.
        if text_snippet and not bucket["text_snippet"]:
            bucket["text_snippet"] = text_snippet
        return

    buckets.append({
        "video_id": video_id,
        "start_time": start_time,
        "end_time": end_time,
        "rrf_score": 1.0 / (RRF_K + rank),
        "sources": {source},
        "text_snippet": text_snippet,
    })


def rrf_rank(audio_points, frame_points, top_k: int) -> list[SearchResultItem]:
    """Слить хиты из аудио- и кадрового каналов в единый ранкинг через RRF."""
    buckets: list[dict] = []

    for rank, hit in enumerate(audio_points, start=1):
        payload = hit.payload or {}
        video_id = payload.get("video_id")
        start_time = payload.get("start_time")
        end_time = payload.get("end_time")
        if video_id is None or start_time is None or end_time is None:
            continue
        _merge_hit_into_buckets(
            buckets,
            video_id=video_id,
            start_time=float(start_time),
            end_time=float(end_time),
            rank=rank,
            source="audio",
            text_snippet=payload.get("text"),
        )

    for rank, hit in enumerate(frame_points, start=1):
        payload = hit.payload or {}
        video_id = payload.get("video_id")
        start_time = payload.get("start_time")
        end_time = payload.get("end_time")
        if video_id is None or start_time is None or end_time is None:
            continue
        _merge_hit_into_buckets(
            buckets,
            video_id=video_id,
            start_time=float(start_time),
            end_time=float(end_time),
            rank=rank,
            source="frames",
            text_snippet=None,
        )

    buckets.sort(key=lambda b: b["rrf_score"], reverse=True)

    results: list[SearchResultItem] = []
    
    for bucket in buckets[:top_k]:
        if bucket["sources"] == {"audio"}:
            source = "audio"
        elif bucket["sources"] == {"frames"}:
            source = "frames"
        else:
            source = "both"

        results.append(SearchResultItem(
            video_id=bucket["video_id"],
            score=bucket["rrf_score"],
            text_snippet=bucket["text_snippet"],
            start_time=bucket["start_time"],
            end_time=bucket["end_time"],
            source=source,
        ))

    return results


# Поиск по аудио и кадрам
@router.post("/search", response_model=SearchResponse)
async def search(
    request: SearchRequest,
    ml_service: MLService = Depends(get_ml_service),
    qdrant_service: QdrantService = Depends(get_qdrant_service)
):
    # Параллельный поиск по обеим коллекциям
    audio_embedding_future = asyncio.to_thread(ml_service.get_embedding, request.query, True)
    clip_text_embedding_future = asyncio.to_thread(ml_service.get_vision_text_embedding, request.query)

    audio_embedding, clip_text_embedding = await asyncio.gather(
        audio_embedding_future,
        clip_text_embedding_future
    )

    # Берём с запасом по каждому каналу — даём RRF-слиянию материал для работы.
    per_channel_k = max(request.top_k * 5, 20)

    audio_hits_future = asyncio.to_thread(qdrant_service.search_audio, audio_embedding, per_channel_k)
    frame_hits_future = asyncio.to_thread(qdrant_service.search_frames, clip_text_embedding, per_channel_k)

    audio_hits, frame_hits = await asyncio.gather(audio_hits_future, frame_hits_future)

    results = rrf_rank(audio_hits.points, frame_hits.points, top_k=request.top_k)

    return SearchResponse(results=results)

@router.delete("/videos/{video_id}", status_code=204)
async def delete_video(
    video_id: str,
    qdrant_service: QdrantService = Depends(get_qdrant_service),
):
    """
    Удаляет аудио-чанки и кадры видео из Qdrant.

    - 204 + счётчики удалённого, если что-то нашлось.
    - 404, если не было ни одной точки ни в одной коллекции.
    - 500, если Qdrant вернул ошибку.
    """
    try:
        deleted = await asyncio.to_thread(qdrant_service.delete_video, video_id)
    except Exception as e:
        print(f"[delete_video {video_id}] Qdrant error: {e}")
        raise HTTPException(status_code=500, detail=f"Qdrant delete failed: {e}")

    if deleted is None:
        raise HTTPException(
            status_code=404,
            detail=f"No vectors found for video_id={video_id}",
        )

    print(f"[delete_video {video_id}] deleted: {deleted}")
    return {"status": "deleted", "video_id": video_id, "deleted": deleted}