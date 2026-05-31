from fastapi import APIRouter, BackgroundTasks, Depends
import uuid
import asyncio
import httpx
import os
import time


from app.models.schemas import AudioProcessRequest, SearchRequest, SearchResponse, SearchResultItem
from app.services.inference import MLService
from app.services.vector_store import QdrantService
from app.services.s3_service import S3Service
from app.core.dependencies import get_ml_service, get_qdrant_service, get_s3_service

router = APIRouter()

def process_audio_task(
    video_id: str, 
    object_key: str, 
    bucket_name: str,
    ml: MLService, 
    qdrant: QdrantService, 
    s3: S3Service
):
    temp_local_path = f"/tmp/{uuid.uuid4()}_audio.wav"
    try:
        s3.download_file(bucket_name=bucket_name, object_key=object_key, local_path=temp_local_path)
        chunks = ml.process_audio_to_chunks(temp_local_path)

        vectors = [ml.get_embedding(chunk["text"], is_query=False) for chunk in chunks]
        saved = qdrant.upsert_chunks(video_id=video_id, chunks=chunks, vectors=vectors)
        print(f"Video {video_id} processed. {saved} chunks saved.")

        _send_status_callback(video_id, {"status": "READY"})
    except Exception as e:
        print(f"Error processing video {video_id}: {e}")
        _send_status_callback(video_id, {"status": "ERROR", "error": str(e)})
        raise
    
    finally:
        if os.path.exists(temp_local_path):
            os.remove(temp_local_path)

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

# Обработка аудио
@router.post("/process", status_code=202)
async def process_audio(
    request: AudioProcessRequest, 
    background_tasks: BackgroundTasks,
    ml_service: MLService = Depends(get_ml_service),
    qdrant_service: QdrantService = Depends(get_qdrant_service),
    s3_service: S3Service = Depends(get_s3_service)
):
    background_tasks.add_task(
        asyncio.to_thread,
        process_audio_task,
        request.video_id,
        request.object_key,
        request.bucket_name,
        ml_service,
        qdrant_service,
        s3_service
    )
    return {"status": "accepted", "video_id": request.video_id}

# Поиск
@router.post("/search", response_model=SearchResponse)
async def search(
    request: SearchRequest,
    ml_service: MLService = Depends(get_ml_service),
    qdrant_service: QdrantService = Depends(get_qdrant_service)
):
    query_embedding = await asyncio.to_thread(ml_service.get_embedding, request.query, is_query=True)
    hits = await asyncio.to_thread(qdrant_service.search, query_embedding, top_k=request.top_k)
        
    results = [
        SearchResultItem(
            video_id=hit.payload.get("video_id"),
            score=hit.score,
            text_snippet=hit.payload.get("text"),
            start_time=hit.payload.get("start_time"),
            end_time=hit.payload.get("end_time")
        ) for hit in hits.points
    ]
    return SearchResponse(results=results)