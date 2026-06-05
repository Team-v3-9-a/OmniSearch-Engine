from qdrant_client import QdrantClient
from qdrant_client.http.models import Distance, VectorParams, PointStruct
from typing import List
import uuid
import os

CLIP_VECTOR_SIZE = 512  # CLIP ViT-B-32 output dimension

class QdrantService:
    def __init__(self, vector_size: int = 768):
        host = os.getenv("QDRANT_HOST", "localhost")
        port = int(os.getenv("QDRANT_PORT", 6333))
        self.client = QdrantClient(host=host, port=port)
        self.audio_collection = "audio_collection"
        self.frames_collection = "frames_collection"
        self.vector_size = vector_size
        self._ensure_collections()

    def _ensure_collections(self):
        """Создание коллекций для аудио и кадров, если они ещё не существуют."""
        if not self.client.collection_exists(self.audio_collection):
            self.client.create_collection(
                collection_name=self.audio_collection,
                vectors_config=VectorParams(size=self.vector_size, distance=Distance.COSINE),
            )

        if not self.client.collection_exists(self.frames_collection):
            self.client.create_collection(
                collection_name=self.frames_collection,
                vectors_config=VectorParams(size=CLIP_VECTOR_SIZE, distance=Distance.COSINE),
            )

    # --- Аудио ---

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
            self.client.upsert(collection_name=self.audio_collection, points=points)

        return len(points)

    # Поиск видео по эмбеддингу (аудио)
    def search(self, query_embedding: list, top_k: int = 10):
        search_result = self.client.query_points(
            collection_name=self.audio_collection,
            query=query_embedding,
            limit=top_k
        )
        return search_result

    # --- Кадры (Frames) ---

    def upsert_frames(self, video_id: str, frames: List[dict], vectors: List[list]) -> int:
        """
        Сохранение эмбеддингов кадров в коллекцию frames_collection.
        Каждый frame содержит: start_time, end_time, frame_index, frame_key.
        """
        points = [
            PointStruct(
                id=str(uuid.uuid5(uuid.NAMESPACE_DNS, f"{video_id}_frame_{frame['frame_index']}")),
                vector=vector,
                payload={
                    "video_id": video_id,
                    "start_time": frame["start_time"],
                    "end_time": frame["end_time"],
                    "frame_index": frame["frame_index"],
                    "frame_key": frame.get("frame_key", ""),
                }
            )
            for frame, vector in zip(frames, vectors)
        ]

        if points:
            self.client.upsert(collection_name=self.frames_collection, points=points)

        return len(points)

    # Поиск по кадрам (frames)
    def search_frames(self, query_embedding: list, top_k: int = 10):
        search_result = self.client.query_points(
            collection_name=self.frames_collection,
            query=query_embedding,
            limit=top_k
        )
        return search_result