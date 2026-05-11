from pydantic import BaseModel
from typing import List, Optional

# Валидация данных для запроса на обработку аудио
class AudioProcessRequest(BaseModel):
    video_id: str
    bucket_name: str = "video"
    object_key: str

# Валидация данных для запроса на поиск
class SearchRequest(BaseModel):
    query: str
    top_k: int = 5

# Валидация данных для ответа на поиск
class SearchResultItem(BaseModel):
    video_id: str
    score: float
    start_time: Optional[float] = None
    end_time: Optional[float] = None
    text_snippet: Optional[str] = None

class SearchResponse(BaseModel):
    results: List[SearchResultItem]