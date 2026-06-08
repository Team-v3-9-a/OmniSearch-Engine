from qdrant_client import QdrantClient
from qdrant_client.http.models import (
    Distance,
    VectorParams,
    PointStruct,
    Filter,
    FieldCondition,
    MatchValue,
    FilterSelector,
    PayloadSchemaType,
)
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

        # Payload-индекс по video_id для быстрых фильтр-запросов (delete, count по video_id).
        for collection in (self.audio_collection, self.frames_collection):
            try:
                self.client.create_payload_index(
                    collection_name=collection,
                    field_name="video_id",
                    field_schema=PayloadSchemaType.KEYWORD,
                )
            except Exception:
                # Индекс уже существует — норма.
                pass


    def upsert_chunks(self, video_id: str, chunks: List[dict], vectors: List[list]) -> int:
        """Сохранение эмбеддингов аудио-сегментов в коллекцию audio_collection."""
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

    def search_audio(self, query_embedding: list, top_k: int = 10):
        """Поиск в коллекции аудио по эмбеддингу запроса."""
        search_result = self.client.query_points(
            collection_name=self.audio_collection,
            query=query_embedding,
            limit=top_k
        )
        return search_result

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

    def search_frames(self, query_embedding: list, top_k: int = 10):
        """Поиск в коллекции кадров по эмбеддингу запроса."""
        search_result = self.client.query_points(
            collection_name=self.frames_collection,
            query=query_embedding,
            limit=top_k
        )
        return search_result

    def _video_filter(self, video_id: str) -> Filter:
        """Фильтр Qdrant по video_id"""
        return Filter(
            must=[FieldCondition(key="video_id", match=MatchValue(value=video_id))]
        )

    def _count_by_video(self, collection: str, video_id: str) -> int:
        """Точное число точек по video_id в коллекции."""
        result = self.client.count(
            collection_name=collection,
            count_filter=self._video_filter(video_id),
            exact=True,
        )
        return result.count

    def delete_video(self, video_id: str) -> dict | None:
        """
        Удалить все векторы видео из обеих коллекций.

        Возвращает {"audio": N, "frames": M} с числом удалённых точек.
        Если видео не было ни в одной коллекции — возвращает None.
        """
        audio_count = self._count_by_video(self.audio_collection, video_id)
        frames_count = self._count_by_video(self.frames_collection, video_id)

        if audio_count == 0 and frames_count == 0:
            return None

        selector = FilterSelector(filter=self._video_filter(video_id))

        # wait=True — гарантируем, что к моменту ответа точки реально удалены
        if audio_count > 0:
            self.client.delete(
                collection_name=self.audio_collection,
                points_selector=selector,
                wait=True,
            )
        if frames_count > 0:
            self.client.delete(
                collection_name=self.frames_collection,
                points_selector=selector,
                wait=True,
            )

        return {"audio": audio_count, "frames": frames_count}