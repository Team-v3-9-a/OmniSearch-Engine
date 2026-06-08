# 🎬 Video Engine

Микросервис обработки медиа на Go. Выполняет I/O-тяжёлые операции: скачивание видео из S3, извлечение аудиодорожки (FFmpeg), сэмплирование кадров (GoCV/OpenCV), загрузку результатов обратно в S3.

## Технологический стек

- **Go 1.25** — основной язык
- **GoCV** (CGO-биндинги к OpenCV 4.x) — нарезка кадров
- **FFmpeg** — извлечение аудио (`stream copy` → WAV)
- **MinIO Go SDK** — S3 download/upload
- **Docker** — gocv/opencv:4.13.0 base image

## Архитектура

```
video-engine/
├── cmd/video-engine/
│   └── main.go              # Точка входа, signal handling, graceful shutdown
├── internal/
│   ├── api/
│   │   └── server.go        # HTTP-сервер (POST /process, GET /health)
│   ├── audio/
│   │   └── extractor.go     # FFmpeg: видео → audio.wav
│   ├── video/
│   │   └── sampler.go       # GoCV: видео → frames/*.jpg (1-2 FPS)
│   ├── ml/
│   │   └── client.go        # HTTP-клиент к ML Engine (POST /api/v1/process)
│   ├── s3/
│   │   └── client.go        # MinIO SDK: download video, upload audio + frames
│   └── pipeline/
│       └── orchestrator.go  # Параллельный запуск audio + video через goroutines
├── Dockerfile
├── go.mod
└── makefile
```

## API

### `GET /health`
Health check. Возвращает `200 OK` с телом `ok`.

### `POST /process`
Асинхронная обработка видео. Отвечает `202 Accepted` и запускает pipeline в goroutine.

**Request:**
```json
{
  "video_id": "uuid-string",
  "s3_path": "videos/filename.mp4"
}
```

**Pipeline:**
1. Скачать видео из S3 (`s3_path`)
2. Параллельно: извлечь аудио (FFmpeg) + сэмплировать кадры (GoCV)
3. Загрузить артефакты в S3: `media/{video_id}/audio.wav`, `media/{video_id}/frames/frame_*.jpg`, `media/{video_id}/thumbnail.jpg`
4. Callback к Backend: `PATCH /api/v1/internal/videos/{video_id}` со статусом `PROCESSING_ML` + метаданные
5. Trigger ML Engine: `POST /api/v1/process` с `audio_key` и `frames_prefix`

## Переменные окружения

| Переменная | Описание | Default |
|-----------|----------|---------|
| `PORT` | Порт HTTP-сервера | `8081` |
| `S3_ENDPOINT` | URL MinIO | `http://minio:9000` |
| `S3_ACCESS_KEY` | MinIO access key | — |
| `S3_SECRET_KEY` | MinIO secret key | — |
| `BACKEND_API_URL` | URL Backend для callbacks | `http://backend:8080` |
| `INTERNAL_API_SECRET` | Токен для `X-Internal-Secret` | — |
| `SHARED_MEDIA_PATH` | Временная директория для файлов | `/app/shared_media` |

## Локальная разработка

> ⚠️ Требует GCC, OpenCV 4.x и FFmpeg установленных в системе.

```bash
# Сборка
go build -o video-engine ./cmd/video-engine

# Запуск
./video-engine

# Тесты
go test ./...

# Через Make
make build
make test
```

## Формат именования кадров

Кадры сохраняются с таймкодами в имени для парсинга в ML Engine:
```
frame_{index}_{startTime}_{endTime}.jpg
```
Пример: `frame_042_21.5_22.5.jpg` — кадр #42, соответствует интервалу 21.5–22.5 секунд.