# Retrieval Pipeline — Гибридный семантический поиск

Пайплайн поиска выполняет **мультимодальный семантический поиск** по загруженным видео — одновременно по аудио-транскрипции (E5) и визуальному контенту кадров (CLIP). Результаты объединяются через **RRF (Reciprocal Rank Fusion)**.

## Sequence Diagram

```mermaid
---
config:
  theme: redux-dark-color
---
sequenceDiagram
    participant U as User (UI)
    participant K as Backend (Ktor)
    participant ML as ML Engine (FastAPI)
    participant QD as Qdrant (Vector DB)
    participant DB as PostgreSQL
    participant S3 as MinIO (S3)

    U->>K: GET /api/v1/videos/search?query="кикфлип на рыбе"
    K->>ML: POST /api/v1/search {query, top_k}

    par Параллельная векторизация запроса
        ML->>ML: E5 text embedding (query)
        ML->>ML: CLIP text embedding (query)
    end

    par Параллельный поиск по двум коллекциям
        ML->>QD: Search audio_collection (E5 vector, top_k×5)
        QD-->>ML: Audio hits [{video_id, score, text, start_time, end_time}]
        ML->>QD: Search frames_collection (CLIP vector, top_k×5)
        QD-->>ML: Frame hits [{video_id, score, start_time, end_time}]
    end

    ML->>ML: RRF ранкинг (слияние audio + frames hits)
    ML-->>K: SearchResponse {results: [{video_id, score, text_snippet, start_time, end_time, source}]}

    K->>DB: Запрос метаданных (title, duration, status, thumbnailPath) для video_ids
    K->>K: Фильтрация: только status=READY
    K->>K: Группировка сегментов по video_id
    K->>S3: Presigned URL для thumbnails
    K-->>U: JSON [{video_id, title, thumbnail_url, duration, score, segments[]}]

    Note over U, S3: Прямое взаимодействие браузера и хранилища
    U->>S3: GET thumbnail по presigned URL
    S3-->>U: Изображение для карточки результата
```

## Ключевые детали

### RRF (Reciprocal Rank Fusion)
- **Формула:** `score = Σ 1 / (K + rank)`, где `K = 60` (каноническое значение)
- Хиты из audio и frames каналов сливаются в **бакеты** по `(video_id, overlapping time interval)`
- Если аудио-хит и frame-хит пересекаются по времени → они попадают в один бакет, и RRF-скор суммируется
- Поле `source` в ответе: `"audio"`, `"frames"`, `"both"` — показывает, из каких каналов пришёл результат

### Обогащение на Backend
- Backend получает плоский список `[{video_id, score, text_snippet, start_time, end_time}]` от ML Engine
- Группирует по `video_id` → собирает `segments[]` с лучшим скором как основным
- Фильтрует: только видео со статусом `READY` (игнорирует `PROCESSING_*`, `ERROR`)
- Генерирует presigned URL для thumbnails через MinIO SDK