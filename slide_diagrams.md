# 📊 Диаграммы для слайдов OmniSearch

Эти диаграммы специально подготовлены для презентации: они светлые, с прозрачным фоном и цветовым кодированием компонентов (синий — вход/клиенты, зеленый — ядро, желтый — хранилища, фиолетовый — модели/внешние сервисы).

## 1. Оркестрация: Backend на Ktor
Слайд для Паши. Фокус на том, как Ktor выступает центральным узлом связи.

```mermaid
%%{init: {'theme': 'default', 'themeVariables': { 'background': 'transparent' }}}%%
graph LR
    classDef client fill:#e3f2fd,stroke:#1565c0,stroke-width:2px,color:#000
    classDef core fill:#c8e6c9,stroke:#388e3c,stroke-width:2px,color:#000
    classDef storage fill:#ffe0b2,stroke:#f57c00,stroke-width:2px,color:#000
    classDef ext fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px,color:#000

    Client([Frontend / API Client]):::client -->|HTTP Requests| API[Ktor REST API<br>Routing & Auth]:::core
    API --> DB[(PostgreSQL<br>Метаданные)]:::storage
    API --> S3[(MinIO S3<br>Хранилище видео)]:::storage
    
    API -.->|Асинхронный вызов| VE[Video Engine<br>Go]:::ext
    API -.->|HTTP POST /process| ML[ML Engine<br>FastAPI]:::ext
```

## 2. Интеллектуальное ядро: ML Engine
Слайд для Миши. Фокус на двух моделях: Whisper (текст) и CLIP (компьютерное зрение).

```mermaid
%%{init: {'theme': 'default', 'themeVariables': { 'background': 'transparent' }}}%%
graph TD
    classDef input fill:#e3f2fd,stroke:#1565c0,stroke-width:2px,color:#000
    classDef process fill:#e1bee7,stroke:#7b1fa2,stroke-width:2px,color:#000
    classDef model fill:#f8bbd0,stroke:#c2185b,stroke-width:2px,color:#000
    classDef storage fill:#ffe0b2,stroke:#f57c00,stroke-width:2px,color:#000

    Audio([Аудио .wav]):::input --> Whisper[Модель Whisper<br>Speech-to-Text]:::model
    Frames([Кадры .jpg]):::input --> CLIP_Vision[Модель CLIP<br>Vision Encoder]:::model
    
    Whisper --> Text[Транскрипция]:::process
    Text --> CLIP_Text[Модель CLIP<br>Text Encoder]:::model
    
    CLIP_Vision --> Emb1([Видео-эмбеддинги]):::process
    CLIP_Text --> Emb2([Текстовые эмбеддинги]):::process
    
    Emb1 --> VDB[(Vector DB<br>Qdrant / Chroma)]:::storage
    Emb2 --> VDB
```

## 3. Медиа-движок: Video Engine
Слайд для Дениса. Как Go параллельно извлекает медиа-данные с помощью системных утилит.

```mermaid
%%{init: {'theme': 'default', 'themeVariables': { 'background': 'transparent' }}}%%
graph LR
    classDef input fill:#e3f2fd,stroke:#1565c0,stroke-width:2px,color:#000
    classDef go fill:#bbdefb,stroke:#1976d2,stroke-width:2px,color:#000
    classDef ext fill:#c8e6c9,stroke:#388e3c,stroke-width:2px,color:#000
    classDef output fill:#ffe0b2,stroke:#f57c00,stroke-width:2px,color:#000

    Video([Сырое видео .mp4]):::input --> CLI[Go Worker<br>Goroutines]:::go
    
    CLI -->|Извлечение аудио| FFmpeg[FFmpeg<br>Subprocess]:::ext
    CLI -->|Нарезка кадров| GoCV[GoCV / OpenCV<br>CGO Bindings]:::ext
    
    FFmpeg --> OutAudio([Аудио дорожка<br>.wav]):::output
    GoCV --> OutFrames([Набор кадров<br>.jpg]):::output
    
    OutAudio --> S3[(MinIO S3)]:::output
    OutFrames --> S3
```

## 4. Инфраструктура и CI/CD
Слайд для Максима. Контейнеризация и процесс доставки на удаленный сервер.

```mermaid
%%{init: {'theme': 'default', 'themeVariables': { 'background': 'transparent' }}}%%
graph TD
    classDef dev fill:#e3f2fd,stroke:#1565c0,stroke-width:2px,color:#000
    classDef ci fill:#fff9c4,stroke:#fbc02d,stroke-width:2px,color:#000
    classDef docker fill:#bbdefb,stroke:#1976d2,stroke-width:2px,color:#000
    classDef server fill:#c8e6c9,stroke:#388e3c,stroke-width:2px,color:#000

    Dev([Разработчик]):::dev -->|Git Push| GH[GitHub Repository]:::dev
    GH --> Actions[GitHub Actions<br>CI/CD Pipeline]:::ci
    
    subgraph CI Pipeline
        Actions --> Lint[Линтеры & Тесты]:::ci
        Actions --> Build[Multi-stage Docker Build]:::docker
        Build --> Registry[(Docker Registry)]:::docker
    end
    
    Registry -->|Pull Images| Server[Удаленный Сервер<br>Docker Compose]:::server
    Server --> Nginx[Nginx Reverse Proxy]:::server
```
