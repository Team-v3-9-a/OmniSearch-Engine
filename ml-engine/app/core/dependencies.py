from app.services.inference import MLService
from app.services.vector_store import QdrantService

ml_service_instance = MLService()
qdrant_service_instance = QdrantService()

def get_ml_service() -> MLService:
    return ml_service_instance

def get_qdrant_service() -> QdrantService:
    return qdrant_service_instance