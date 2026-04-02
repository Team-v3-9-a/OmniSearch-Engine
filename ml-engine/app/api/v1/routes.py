from fastapi import APIRouter, BackgroundTasks, Depends
import uuid
from qdrant_client.http.models import PointStruct
import asyncio

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
    chunks = ml.process_audio_to_chunks(audio_path)
    
    points = []
    for i, chunk in enumerate(chunks):
        # Генерация детерминированного ID, чтобы избежать дубликатов при ретраях
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