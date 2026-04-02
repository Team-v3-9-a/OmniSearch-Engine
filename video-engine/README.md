# OmniSearch-Engine
AI-powered semantic search system for unstructured video content
```
video-engine/
├── cmd/
│   └── video-engine/
│       └── main.go             # Точка входа в приложение (парсинг флагов, старт пайплайна)
├── internal/
│   ├── config/
│   │   └── config.go           # Валидация входных аргументов и путей
│   ├── audio/
│   │   └── extractor.go        # Логика вызова FFmpeg через os/exec
│   ├── video/
│   │   └── sampler.go          # Логика работы с GoCV (нарезка кадров)
│   └── pipeline/
│       └── orchestrator.go     # Управление горутинами (параллельный запуск audio и video)
├── Dockerfile                  # Инструкции для сборки контейнера Максимом
├── Makefile                    # Удобные алиасы для локальной сборки и тестов
├── go.mod                      # Описание модуля и зависимостей (gocv)
└── go.sum                      # Контрольные суммы зависимостей
```