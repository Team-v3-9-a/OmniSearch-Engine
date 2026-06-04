from fastapi import APIRouter, BackgroundTasks, Depends
import uuid
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


# ────────────────────────────────────────────
#  Фоновая задача: обработка аудио + кадров
# ────────────────────────────────────────────

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
        # ── Этап 1: Аудио ──
        print(f"[{video_id}] Downloading audio: {audio_key}")
        s3.download_file(bucket_name=bucket_name, object_key=audio_key, local_path=temp_audio_path)

        print(f"[{video_id}] Transcribing audio with Whisper...")
        chunks = ml.process_audio_to_chunks(temp_audio_path)

        print(f"[{video_id}] Generating text embeddings for {len(chunks)} chunks...")
        vectors = [ml.get_embedding(chunk["text"], is_query=False) for chunk in chunks]
        saved_audio = qdrant.upsert_chunks(video_id=video_id, chunks=chunks, vectors=vectors)
        print(f"[{video_id}] Audio processed: {saved_audio} chunks saved.")

        # ── Этап 2: Кадры (CLIP) ──
        print(f"[{video_id}] Listing frames with prefix: {frames_prefix}")
        frame_keys = s3.list_objects(bucket_name=bucket_name, prefix=frames_prefix)
        # Фильтруем только .jpg файлы
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
    """Скачивание кадров, парсинг таймкодов, создание CLIP-эмбеддингов и сохранение в Qdrant."""
    frames_dir = os.path.join(temp_dir, "frames")
    os.makedirs(frames_dir, exist_ok=True)

    frame_metadata = []
    local_paths = []

    for key in sorted(frame_keys):
        filename = os.path.basename(key)
        timecodes = MLService.parse_frame_timecodes(filename)
        if timecodes is None:
            print(f"[{video_id}] Skipping frame with unparseable name: {filename}")
            continue

        local_path = os.path.join(frames_dir, filename)
        s3.download_file(bucket_name=bucket_name, object_key=key, local_path=local_path)

        frame_metadata.append({
            "frame_index": timecodes["index"],
            "start_time": timecodes["start_time"],
            "end_time": timecodes["end_time"],
            "frame_key": key,
        })
        local_paths.append(local_path)

    if not local_paths:
        print(f"[{video_id}] No valid frames to process.")
        return

    print(f"[{video_id}] Generating CLIP embeddings for {len(local_paths)} frames...")
    frame_vectors = ml.get_image_embeddings_batch(local_paths)

    saved_frames = qdrant.upsert_frames(video_id=video_id, frames=frame_metadata, vectors=frame_vectors)
    print(f"[{video_id}] Frames processed: {saved_frames} frame embeddings saved.")


# ────────────────────────────────────────────
#  Callback к Backend
# ────────────────────────────────────────────

def _send_status_callback(video_id: str, payload: dict):
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


# ────────────────────────────────────────────
#  Эндпоинты
# ────────────────────────────────────────────

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

    audio_hits_future = asyncio.to_thread(qdrant_service.search, audio_embedding, request.top_k)
    frame_hits_future = asyncio.to_thread(qdrant_service.search_frames, clip_text_embedding, request.top_k)

    audio_hits, frame_hits = await asyncio.gather(audio_hits_future, frame_hits_future)

    results: list[SearchResultItem] = []

    # Результаты из аудио
    for hit in audio_hits.points:
        results.append(SearchResultItem(
            video_id=hit.payload.get("video_id"),
            score=hit.score,
            text_snippet=hit.payload.get("text"),
            start_time=hit.payload.get("start_time"),
            end_time=hit.payload.get("end_time"),
            source="audio",
        ))

    # Результаты из кадров
    for hit in frame_hits.points:
        results.append(SearchResultItem(
            video_id=hit.payload.get("video_id"),
            score=hit.score,
            start_time=hit.payload.get("start_time"),
            end_time=hit.payload.get("end_time"),
            source="frames",
        ))

    # Сортировка по score (лучшие сверху)
    results.sort(key=lambda r: r.score, reverse=True)

    return SearchResponse(results=results)