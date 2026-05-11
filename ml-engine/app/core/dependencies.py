from app.services.inference import MLService
from app.services.vector_store import QdrantService
from app.services.s3_service import S3Service

ml_service_instance = MLService()
qdrant_service_instance = QdrantService()
s3_service_instance = S3Service()

def get_ml_service() -> MLService:
    return ml_service_instance

def get_qdrant_service() -> QdrantService:
    return qdrant_service_instance

def get_s3_service() -> S3Service:
    return s3_service_instance