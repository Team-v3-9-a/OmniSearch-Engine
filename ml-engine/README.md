# 🧠 ML Engine

Микросервис машинного обучения на Python/FastAPI. Выполняет compute-тяжёлые операции: транскрибацию аудио (Whisper), генерацию текстовых (E5) и визуальных (CLIP) эмбеддингов, хранение и поиск в Qdrant.

## Технологический стек

- **Python 3.11** + **FastAPI** — API-сервер
- **faster-whisper** (large-v3, INT8) — транскрибация аудио
- **SentenceTransformers** — эмбеддинги:
  - `intfloat/multilingual-e5-base` (768d) — текстовые эмбеддинги аудио-чанков
  - `clip-ViT-B-32` (512d) — визуальные эмбеддинги кадров
  - `clip-ViT-B-32-multilingual-v1` (512d) — текстовые эмбеддинги для поиска по кадрам
- **Qdrant** — векторная БД (2 коллекции: `audio_collection`, `frames_collection`)
- **boto3** — S3 клиент для MinIO

## Архитектура

```
ml-engine/
├── app/
│   ├── main.py                  # FastAPI app, lifespan, health check
│   ├── api/v1/
│   │   └── routes.py            # Эндпоинты: /process, /search + RRF логика
│   ├── models/
│   │   └── schemas.py           # Pydantic схемы (request/response)
│   ├── services/
│   │   ├── inference.py         # MLService: Whisper, E5, CLIP inference
│   │   ├── vector_store.py      # QdrantService: upsert, search по коллекциям
│   │   └── s3_service.py        # S3Service: download, list objects
│   └── core/
│       └── dependencies.py      # FastAPI DI (singleton сервисы)
├── requirements.txt
└── Dockerfile
```

## API

### `GET /health`
Health check. Возвращает `{"status": "ready"}`.

### `POST /api/v1/process`
Фоновая обработка аудио + кадров. Возвращает `202 Accepted`.

**Request:**
```json
{
  "video_id": "uuid-string",
  "bucket_name": "videos",
  "audio_key": "media/{id}/audio.wav",
  "frames_prefix": "media/{id}/frames/"
}
```

**Pipeline:**
1. Скачать `audio.wav` из S3
2. Whisper транскрибация → текстовые чанки `[{text, start_time, end_time}]`
3. E5 эмбеддинги для каждого чанка → upsert в `audio_collection` (Qdrant)
4. Скачать кадры по `frames_prefix` из S3
5. CLIP Vision эмбеддинги для кадров → upsert в `frames_collection` (Qdrant)
6. Callback к Backend: `PATCH /api/v1/internal/videos/{id}` — статус `READY` или `ERROR`

### `POST /api/v1/search`
Гибридный семантический поиск по аудио и кадрам.

**Request:**
```json
{
  "query": "текстовый запрос",
  "top_k": 5
}
```

**Response:**
```json
{
  "results": [
    {
      "video_id": "uuid",
      "score": 0.0163,
      "text_snippet": "...фрагмент транскрипции...",
      "start_time": 45.5,
      "end_time": 52.3,
      "source": "both"
    }
  ]
}
```

**Алгоритм поиска:**
1. Параллельно: E5 text embedding + CLIP text embedding
2. Параллельно: search `audio_collection` + search `frames_collection` (по `top_k × 5` хитов)
3. RRF ранкинг: слияние хитов по пересекающимся временным интервалам
4. Возврат top_k результатов с полем `source` (`audio` / `frames` / `both`)

## Qdrant коллекции

| Коллекция | Размер вектора | Модель | Payload |
|-----------|---------------|--------|---------|
| `audio_collection` | 768 | E5-base | `video_id`, `text`, `start_time`, `end_time` |
| `frames_collection` | 512 | CLIP ViT-B-32 | `video_id`, `start_time`, `end_time`, `frame_index`, `frame_key` |

## Переменные окружения

| Переменная | Описание | Default |
|-----------|----------|---------|
| `QDRANT_HOST` | Хост Qdrant | `localhost` |
| `QDRANT_PORT` | Порт Qdrant | `6333` |
| `S3_ENDPOINT` | URL MinIO | `http://minio:9000` |
| `S3_ACCESS_KEY` | MinIO access key | — |
| `S3_SECRET_KEY` | MinIO secret key | — |
| `INTERNAL_API_SECRET` | Токен для callback к Backend | — |
| `BACKEND_URL` | URL Backend | `http://backend:8000` |

## Локальная разработка

```bash
# Установка зависимостей
pip install -r requirements.txt

# Запуск (модели скачиваются при первом запуске ~10-15 мин)
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# Линтер
ruff check .

# Тесты
pytest
```

> ⚠️ При первом запуске загружаются ~5 GB ML-моделей в `/models/`.  
> 💡 GPU (CUDA 12.x) значительно ускоряет inference, но CPU тоже поддерживается.
