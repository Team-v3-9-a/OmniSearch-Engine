from fastapi import APIRouter, BackgroundTasks, Depends
import uuid
from qdrant_client.http.models import PointStruct
import asyncio
import httpx
import os

from app.models.schemas import AudioProcessRequest, SearchRequest, SearchResponse, SearchResultItem
from app.services.inference import MLService
from app.services.vector_store import QdrantService
from app.core.dependencies import get_ml_service, get_qdrant_service

router = APIRouter()

def process_audio_task(
    video_id: str, 
    audio_path: str, 
    ml: MLService, 
    qdrant: QdrantService
):
    try:
        chunks = ml.process_audio_to_chunks(audio_path)

        points = []
        for i, chunk in enumerate(chunks):
            # uuid5 (детерменированный)
            unique_string = f"{video_id}_{i}"
            point_id = str(uuid.uuid5(uuid.NAMESPACE_DNS, unique_string))
            vector = ml.get_embedding(chunk["text"], is_query=False)
            points.append(
                PointStruct(
                    id=point_id,
                    vector=vector,
                    payload={
                        "video_id": video_id,
                        "text": chunk["text"],
                        "start_time": chunk["start_time"],
                        "end_time": chunk["end_time"]
                    }
                )
            )

        # Пакетная вставка в Qdrant
        if points:
            qdrant.client.upsert(collection_name=qdrant.collection_name, points=points)
            print(f"Video {video_id} processed. {len(points)} chunks saved.")

        _send_status_callback(video_id, {"status": "READY"})
    except Exception as e:
        print(f"Error processing video {video_id}: {e}")
        _send_status_callback(video_id, {"status": "ERROR", "error": str(e)})
        raise

def _send_status_callback(video_id: str, payload: dict):
    backend_url = os.getenv("BACKEND_URL", "http://backend:8000")
    url = f"{backend_url}/api/v1/internal/videos/{video_id}/status"
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

# Обработка аудио
@router.post("/process", status_code=202)
async def process_audio(
    request: AudioProcessRequest, 
    background_tasks: BackgroundTasks,
    ml_service: MLService = Depends(get_ml_service),
    qdrant_service: QdrantService = Depends(get_qdrant_service)
):
    background_tasks.add_task(
        process_audio_task, 
        request.video_id, 
        request.audio_path, 
        ml_service, 
        qdrant_service
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