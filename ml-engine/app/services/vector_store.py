from qdrant_client import QdrantClient
from qdrant_client.http.models import Distance, VectorParams, PointStruct
import uuid
import os

class QdrantService:
    def __init__(self, vector_size: int = 768):
        host = os.getenv("QDRANT_HOST", "localhost")
        port = int(os.getenv("QDRANT_PORT", 6333))
        self.client = QdrantClient(host=host, port=port)
        self.collection_name = "audio_collection"
        self.vector_size = vector_size
        self._ensure_collection()

    def _ensure_collection(self):
        if not self.client.collection_exists(self.collection_name):
            self.client.create_collection(
                collection_name=self.collection_name,
                vectors_config=VectorParams(size=self.vector_size, distance=Distance.COSINE),
            )

    # Добавление эмбеддинга видео в Qdrant
    def upsert_video_embedding(self, video_id: str, embedding: list, text: str, chunk_index: int = 0):
        # uuid5 (детерменированный)
        unique_string = f"{video_id}_{chunk_index}"
        point_id = str(uuid.uuid5(uuid.NAMESPACE_DNS, unique_string))
        point = PointStruct(
            id=point_id,
            vector=embedding,
            payload={"video_id": video_id, "text": text}
        )
        self.client.upsert(
            collection_name=self.collection_name,
            points=[point]
        )

    # Поиск видео по эмбеддингу
    def search(self, query_embedding: list, top_k: int = 5):
        search_result = self.client.query_points(
            collection_name=self.collection_name,
            query=query_embedding,
            limit=top_k
        )
        return search_result