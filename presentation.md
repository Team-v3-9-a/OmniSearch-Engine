# 🔍 Презентация проекта OmniSearch Engine

## 1. Концепция проекта
**OmniSearch** — это on-premise система семантического поиска по видео. 
Она использует мультимодальный RAG подход, позволяя находить точные моменты в видеоархивах по смысловому текстовому запросу. Система "понимает" и то, что говорят на видео (с помощью транскрибации), и то, что происходит в кадре (благодаря компьютерному зрению).

## 2. Наша Команда
- **Денис (Тимлид, SA, Go VE)** — архитектура системы, проектирование API (API-First), разработка Video Engine на Go (интеграция с CGO и OpenCV).
- **Паша (Backend Ktor)** — оркестратор пайплайнов загрузки, Ktor API, взаимодействие с базами данных и хранилищем, работа с корутинами.
- **Миша (ML)** — Python/FastAPI сервис, интеграция нейросетей Whisper (Speech-to-Text) и CLIP (Vision), генерация и работа с эмбеддингами.
- **Кирилл (Frontend)** — UI/UX приложения, React/TS, загрузка тяжелых файлов, механизмы поллинга статусов обработки, отображение медиаплеера.
- **Максим (DevOps и CI/CD)** — контейнеризация (Multi-stage сборки Docker), docker-compose, настройка пайплайнов (self-hosted GitHub Actions), деплой на удаленный сервер.

## 3. Технологический стек
- **Frontend:** React, TypeScript, Vite.
- **Backend:** Kotlin, Ktor, PostgreSQL, MinIO (S3).
- **Video Engine:** Golang, GoCV (OpenCV bindings), FFmpeg.
- **ML Engine:** Python, FastAPI, PyTorch, Whisper, CLIP, Qdrant / ChromaDB.
- **Инфраструктура:** Docker, GitHub Actions, Nginx.

---

## 4. Архитектурная диаграмма
Взаимодействие компонентов системы и потоки данных:

```mermaid
%%{init: {'theme': 'default', 'themeVariables': { 'background': 'transparent' }}}%%
graph TD
    classDef user fill:#e3f2fd,stroke:#1565c0,stroke-width:2px,color:#000
    classDef frontend fill:#bbdefb,stroke:#1976d2,stroke-width:2px,color:#000
    classDef backend fill:#c8e6c9,stroke:#388e3c,stroke-width:2px,color:#000
    classDef storage fill:#ffe0b2,stroke:#f57c00,stroke-width:2px,color:#000
    classDef worker fill:#e1bee7,stroke:#7b1fa2,stroke-width:2px,color:#000

    User([Пользователь]):::user -->|HTTP / UI| Frontend:::frontend
    
    subgraph "OmniSearch System"
        Frontend[Frontend<br>React/TS]:::frontend
        Backend[Backend API<br>Ktor/Kotlin]:::backend
        VideoEngine[Video Engine<br>Go/GoCV]:::worker
        MLEngine[ML Engine<br>Python/FastAPI]:::worker
    end

    subgraph "Data Storage"
        PostgreSQL[(PostgreSQL<br>Метаданные)]:::storage
        MinIO[(MinIO / S3<br>Медиафайлы)]:::storage
        VectorDB[(Vector DB<br>Векторы)]:::storage
    end

    Frontend <-->|REST API| Backend
    Backend <--> PostgreSQL
    Backend <--> MinIO
    
    Backend -->|Spawn Process / API| VideoEngine
    VideoEngine -->|Чтение/Запись| MinIO
    
    Backend <-->|HTTP REST| MLEngine
    MLEngine -->|Чтение Медиа| MinIO
    MLEngine <--> VectorDB
```

---

## 5. Пайплайн обработки видео (Ingestion Flow)
Описывает процесс загрузки видео, его нарезку на кадры/аудио и прогон через нейросети для сохранения векторов:

```mermaid
%%{init: {'theme': 'default', 'themeVariables': { 'background': 'transparent', 'primaryColor': '#e3f2fd', 'primaryBorderColor': '#1565c0', 'actorBkg': '#bbdefb', 'actorBorder': '#1976d2', 'participantBkg': '#c8e6c9', 'participantBorder': '#388e3c', 'noteBkg': '#fff9c4', 'noteBorder': '#fbc02d', 'textColor': '#000', 'lineColor': '#333' }}}%%
sequenceDiagram
    actor User
    participant UI as Frontend
    participant API as Backend
    participant DB as PostgreSQL
    participant S3 as MinIO
    participant VE as Video Engine
    participant ML as ML Engine
    participant VDB as Vector DB

    User->>UI: Загрузка видео (.mp4)
    UI->>API: Multipart Upload
    API->>S3: Сохранение исходного видео
    API->>DB: Запись статуса (PROCESSING)
    API-->>UI: Возврат Video ID
    
    Note over UI, API: Frontend опрашивает статус (Polling)
    
    API->>VE: Вызов извлечения
    activate VE
    VE->>S3: Чтение видео
    VE->>VE: FFmpeg: аудио (.wav)
    VE->>VE: GoCV: кадры (.jpg)
    VE->>S3: Сохранение медиа-артефактов
    VE-->>API: Завершено
    deactivate VE
    
    API->>ML: POST /process
    activate ML
    ML->>S3: Чтение артефактов
    ML->>ML: Транскрибация (Whisper)
    ML->>ML: Векторизация (CLIP)
    ML->>VDB: Сохранение в Vector DB
    ML-->>API: Готово
    deactivate ML
    
    API->>DB: Статус READY
    UI->>API: GET /status
    API-->>UI: READY
    UI-->>User: Видео доступно для поиска
```

---

## 6. Пайплайн поиска (Retrieval Flow)
Описывает процесс, когда пользователь вводит текстовый запрос, и система мгновенно возвращает нужный видеофрагмент:

```mermaid
sequenceDiagram
    actor User
    participant UI as Frontend
    participant API as Backend
    participant ML as ML Engine
    participant VDB as Vector DB

    User->>UI: Ввод запроса "человек с собакой"
    UI->>API: GET /search?query=...
    API->>ML: HTTP: vectorize(query)
    ML->>ML: Перевод текста в вектор
    ML-->>API: Вектор запроса
    API->>VDB: Поиск по вектору (HNSW)
    VDB-->>API: Топ совпадений (Timestamps)
    API-->>UI: Результаты поиска (JSON)
    UI-->>User: Показ фрагментов видео
```
