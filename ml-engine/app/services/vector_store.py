from qdrant_client import QdrantClient
from qdrant_client.http.models import Distance, VectorParams, PointStruct
from typing import List
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

    def upsert_chunks(self, video_id: str, chunks: List[dict], vectors: List[list]) -> int:
        points = [
            PointStruct(
                id=str(uuid.uuid5(uuid.NAMESPACE_DNS, f"{video_id}_{i}")),
                vector=vector,
                payload={
                    "video_id": video_id,
                    "text": chunk["text"],
                    "start_time": chunk["start_time"],
                    "end_time": chunk["end_time"]
                }
            )
            for i, (chunk, vector) in enumerate(zip(chunks, vectors))
        ]

        if points:
            self.client.upsert(collection_name=self.collection_name, points=points)

        return len(points)

    # Поиск видео по эмбеддингу
    def search(self, query_embedding: list, top_k: int = 10):
        search_result = self.client.query_points(
            collection_name=self.collection_name,
            query=query_embedding,
            limit=top_k
        )
        return search_result