from fastapi import FastAPI
from contextlib import asynccontextmanager

from app.api.v1.routes import router as v1_router
from app.core.dependencies import ml_service_instance, qdrant_service_instance

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