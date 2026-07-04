from fastapi import FastAPI, Response
from contextlib import asynccontextmanager
from prometheus_client import generate_latest, CONTENT_TYPE_LATEST, Gauge

from app.api.v1.routes import router as v1_router
from app.core.dependencies import get_qdrant_service

# Метрики
VECTORS_TOTAL = Gauge(
    "omnisearch_vectors_total", 
    "Total vectors in Qdrant collections", 
    ["collection"]
)

# Инициализация сервисов
@asynccontextmanager
async def lifespan(app: FastAPI):
    print("Инициализация OmniSearch ML Engine завершена. Модели загружены в VRAM.")
    
    yield
    print("Остановка сервиса. Очистка ресурсов...")

app = FastAPI(
    title="OmniSearch ML Engine",
    description="Микросервис транскрибации (Whisper) и векторизации (E5) для Retrieval-Augmented Generation",
    version="1.0.0",
    lifespan=lifespan
)

# Подключение роутеров
app.include_router(v1_router, prefix="/api/v1", tags=["ML Pipeline"])

@app.get("/health", tags=["System"])
def health_check():
    return {"status": "ready"}

@app.get("/metrics", tags=["System"])
def metrics():
    try:
        qdrant = get_qdrant_service()
        if qdrant.client.collection_exists(qdrant.audio_collection):
            audio_info = qdrant.client.get_collection(qdrant.audio_collection)
            VECTORS_TOTAL.labels(collection="audio").set(audio_info.points_count)
        else:
            VECTORS_TOTAL.labels(collection="audio").set(0)
            
        if qdrant.client.collection_exists(qdrant.frames_collection):
            frames_info = qdrant.client.get_collection(qdrant.frames_collection)
            VECTORS_TOTAL.labels(collection="frames").set(frames_info.points_count)
        else:
            VECTORS_TOTAL.labels(collection="frames").set(0)
    except Exception as e:
        print(f"Error updating vector metrics: {e}")
        
    return Response(content=generate_latest(), media_type=CONTENT_TYPE_LATEST)