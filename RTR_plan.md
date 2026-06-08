# 🚀 OmniSearch Engine — Road To Release 1.0.0

**Текущая версия:** 0.2.0 (MVP)  
**Целевая версия:** 1.0.0 (Production BaaS/SaaS)  
**Методология:** Kanban (без спринтов)  
**Бизнес-модель:** SaaS (загрузка видео и поиск) + BaaS (готовое решение компаниям)

---

## Команда

| Роль | Зона ответственности |
|------|---------------------|
| 👑 TL / SA / Video Engine | Архитектура, Video Engine (Go), координация |
| ⚙️ Backend | Kotlin / Ktor — Control Plane |
| 💻 Frontend | React / TypeScript — SPA |
| 🧠 ML Engine | Python / FastAPI — AI-ядро |
| 🐳 DevOps | CI/CD, инфраструктура, деплой |

---

## Архитектурная стратегия мультитенантности

> **Shared Compute + Isolated Data**
>
> - **Compute (общий):** Ktor, ML Engine, Video Engine — единый экземпляр для всех тенантов
> - **Data (изолированный):** Per-tenant PostgreSQL schema + Per-tenant MinIO bucket + Qdrant payload-фильтрация по `tenant_id`

---

## Версионная карта

| Версия | Название эпика | Фокус |
|--------|---------------|-------|
| **0.3.0** | Завершение ядра движка | CRUD, качество поиска, пагинация |
| **0.4.0** | Качество и стабильность | Тесты, логирование, error recovery |
| **0.5.0** | Auth и мультитенантность | JWT, tenant isolation, data separation |
| **0.6.0** | Production Readiness | Мониторинг, безопасность, деплой |
| **1.0.0** | Release | Документация, полировка, релиз |

---

---

# Завершение ядра движка — (v0.3.0)

> Цель: система покрывает все базовые CRUD-операции, а качество поиска доведено до приемлемого уровня за счёт дедупликации кадров.

---

## 👑 TL / Video Engine

#### Дедупликация кадров через perceptual hash
**Описание:** Реализовать фильтрацию «застойных» (визуально идентичных) кадров при сэмплировании. Использовать perceptual hash (pHash) через GoCV для сравнения соседних кадров. Если хэш-расстояние ниже порога — кадр отбрасывается. Это критически влияет на качество RRF-ранкинга в поиске.

**AC:**
- pHash вычисляется для каждого сэмплированного кадра
- Кадр отбрасывается, если Hamming distance с предыдущим сохранённым кадром < порога (настраиваемый через ENV)
- Логируется количество отброшенных vs сохранённых кадров
- На тестовом видео с 30сек статичной сцены количество кадров этой сцены сокращается на 80%+

---

#### Метаданные видео в callback
**Описание:** Отправлять `fps`, `resolution`, `frameCount` в callback к Backend после нарезки.

**AC:**
- Callback payload содержит поля `fps` (float), `resolution` (string "WxH"), `frameCount` (int)
- Backend принимает и сохраняет эти поля в PostgreSQL

---

#### Документация: расширить FR и обновить ingestion pipeline (SA)
**Описание:** Расширить `docs/requirements/fr.md` до полноценных функциональных требований (user stories или развёрнутые FR). Обновить `docs/architecture/ingestion-pipeline.md` — добавить текстовое описание к mermaid-диаграмме.

**AC:**
- `fr.md` содержит минимум 10 функциональных требований, сгруппированных по доменам (Upload, Processing, Search, Management)
- `ingestion-pipeline.md` содержит текстовое описание каждого этапа + mermaid-диаграмму
- Документы ревьюнуты командой

---

## ⚙️ Backend

#### Каскадное удаление видео
**Описание:** Реализовать `DELETE /api/v1/videos/{id}` — полное удаление видео из всех хранилищ.

**AC:**
- Удаление записи из PostgreSQL
- Удаление всех объектов из MinIO (исходное видео + `media/{id}/*`)
- Отправка запроса в ML Engine на удаление векторов из Qdrant
- Если видео в статусе `PROCESSING_*` — возвращает 409 Conflict
- Возвращает 204 No Content при успехе

---

#### Пагинация списка видео
**Описание:** `GET /api/v1/videos` с поддержкой cursor-based пагинации.

**AC:**
- Поддержка query-параметров `limit` (default=20, max=100) и `cursor` (UUID последнего видео)
- Response содержит `items[]`, `next_cursor`, `has_more`
- Сортировка по `createdAt DESC`

---

#### Повторная обработка видео
**Описание:** `POST /api/v1/videos/{id}/reprocess` — перезапуск обработки для видео в статусе ERROR.

**AC:**
- Доступно только для видео со статусом `ERROR`
- Сбрасывает статус на `UPLOADED` и запускает pipeline заново
- Для других статусов — 409 Conflict

---

## 💻 Frontend

#### UI удаления видео
**Описание:** Кнопка удаления на карточке видео и на странице видео с подтверждением.

**AC:**
- Кнопка-иконка «Удалить» на карточке в "Мои видео"
- Модальное окно подтверждения: «Вы уверены? Это действие необратимо»
- После удаления — карточка исчезает с анимацией, toast-уведомление
- Оптимистичный UI: карточка скрывается сразу, откатывается при ошибке

---

#### Пагинация "Мои видео"
**Описание:** Бесконечный скролл или кнопка "Загрузить ещё" для списка видео.

**AC:**
- Подгрузка следующей страницы при скролле или по кнопке
- Skeleton-лоадер при загрузке следующей страницы
- Корректная работа с cursor-based API

---

#### Retry для видео в ERROR
**Описание:** Кнопка «Повторить обработку» на карточках видео со статусом ERROR.

**AC:**
- Кнопка "Повторить" видна только для статуса ERROR
- Клик → `POST /api/v1/videos/{id}/reprocess` → статус переключается на UPLOADED
- Polling статуса возобновляется автоматически

---

## 🧠 ML Engine

#### API удаления векторов
**Описание:** Эндпоинт `DELETE /api/v1/vectors/{video_id}` для удаления всех векторов видео из Qdrant.

**AC:**
- Удаляет все точки из `audio_collection` где `payload.video_id == video_id`
- Удаляет все точки из `frames_collection` где `payload.video_id == video_id`
- Возвращает количество удалённых точек
- Идемпотентный — повторный вызов не даёт ошибку

---

#### Тюнинг RRF-параметров
**Описание:** Сделать параметры RRF-ранкинга настраиваемыми и протестировать оптимальные значения.

**AC:**
- `RRF_K` и `per_channel_k` конфигурируются через ENV
- Документирован baseline качества поиска на тестовом наборе (минимум 5 видео, 10 запросов)

---

## 🐳 DevOps

#### Multi-stage Dockerfiles
**Описание:** Оптимизировать Dockerfile для каждого сервиса через multi-stage builds.

**AC:**
- Все сервисы используют multi-stage build (builder → runtime)
- Суммарный размер образов уменьшен минимум на 30%
- `docker compose build` проходит без ошибок

---

#### Docker Compose profiles
**Описание:** Разделить конфигурацию на `dev` и `prod` профили.

**AC:**
- `docker compose --profile dev up` — поднимает всё с hot-reload, debug логами
- `docker compose --profile prod up` — production-оптимизированный запуск
- Единый `docker-compose.yml` вместо трёх отдельных файлов

---

---

# Качество и стабильность — (v0.4.0)

> Цель: система покрыта тестами, ошибки обрабатываются gracefully, логи структурированы для отладки.

---

## 👑 TL / Video Engine

#### Unit-тесты Video Engine
**Описание:** Покрыть тестами ключевые модули: pipeline, sampler, s3 client, audio extractor.

**AC:**
- Тесты для `pipeline.Process()` с mock S3 и mock файлами
- Тесты для `sampler` (pHash дедупликация, формат именования кадров)
- Тесты для `s3.Client` (upload/download mock)
- Code coverage ≥ 60% для `internal/`
- `go test ./...` проходит в CI

---

#### Structured logging
**Описание:** Перевести Video Engine на структурированное JSON-логирование с request ID.

**AC:**
- Все логи в формате JSON (`level`, `msg`, `video_id`, `timestamp`, `request_id`)
- Request ID генерируется при получении `/process` и прокидывается через весь pipeline
- Уровни логирования: DEBUG, INFO, WARN, ERROR

---

#### Документация: data flow, state machine, runbook (SA)
**Описание:** Создать три документа: `docs/architecture/data-flow.md` (S3 paths, формат данных), `docs/architecture/state-machine.md` (статусы видео, правила переходов), `docs/runbook.md` (операционный гайд: рестарт, логи, частые проблемы).

**AC:**
- `data-flow.md` описывает S3 path convention (`media/{id}/audio.wav`, `media/{id}/frames/frame_*.jpg`), формат payload в Qdrant, схему PostgreSQL
- `state-machine.md` содержит mermaid stateDiagram + текстовые правила каждого перехода + кто инициирует
- `runbook.md` содержит минимум 10 операционных сценариев (зависшее видео, очистка S3, рестарт сервиса)
- Все документы в doc-as-code формате (markdown + mermaid)

---

## ⚙️ Backend

#### Unit и интеграционные тесты
**Описание:** Покрыть тестами UseCases и API endpoints.

**AC:**
- Unit-тесты для `SearchVideosUseCase`, `UploadVideoUseCase`, `UpdateVideoMetaUseCase`
- Интеграционные тесты для REST API (Ktor testApplication)
- Mock-и для внешних зависимостей (S3, ML Engine, Video Engine)
- `./gradlew test` проходит в CI

---

#### Database migrations (Flyway)
**Описание:** Внедрить Flyway для управления миграциями БД вместо auto-create.

**AC:**
- Все существующие таблицы описаны в `V1__init.sql`
- Новые изменения схемы — через новые миграции
- Flyway запускается при старте приложения
- Rollback-миграции для каждой версионной миграции

---

#### Structured logging
**Описание:** Перевести Backend на структурированное JSON-логирование.

**AC:**
- Logback в JSON-формате (`logstash-logback-encoder`)
- MDC: `request_id`, `video_id` в контексте корутин
- Корреляция `request_id` с Video Engine и ML Engine

---

## 💻 Frontend

#### Компонентные тесты
**Описание:** Покрыть тестами ключевые компоненты и страницы.

**AC:**
- Vitest + React Testing Library
- Тесты для `VideoCard`, `Search`, `StatusLabel`
- Тесты для страниц: `SearchResultsPage`, `MyVideosPage`
- `npm run test` проходит в CI

---

#### Code splitting и lazy loading
**Описание:** Оптимизировать загрузку SPA через React.lazy и dynamic imports.

**AC:**
- Страницы загружаются через `React.lazy()` + `Suspense`
- Размер initial bundle уменьшен минимум на 30%
- Spinner/skeleton при загрузке lazy-компонентов

---

## 🧠 ML Engine

#### Unit-тесты ML Engine
**Описание:** Покрыть тестами сервисы и API.

**AC:**
- Тесты для `QdrantService` (upsert, search, delete) с mock Qdrant
- Тесты для `S3Service` (download, list) с mock MinIO
- Тесты для RRF-ранкинга (`rrf_rank` function)
- `pytest` проходит в CI

---

#### Оптимизация загрузки моделей
**Описание:** Lazy loading моделей и оптимизация потребления памяти.

**AC:**
- Модели загружаются при первом запросе, а не при старте (опционально через ENV)
- `/health` отдаёт статус `warming_up` до полной загрузки моделей
- Пиковое потребление RAM снижено минимум на 15%

---

## 🐳 DevOps

#### CI: тестовые стейджи
**Описание:** Добавить этапы тестирования в CI pipeline для всех сервисов.

**AC:**
- CI запускает `go test`, `./gradlew test`, `npm run test`, `pytest`
- Pipeline падает при провале любого теста
- Отчёты о покрытии как артефакты CI

---

#### Docker image optimization
**Описание:** Минимизировать размер production-образов.

**AC:**
- Backend: Alpine-based JRE runtime
- Frontend: Nginx Alpine с собранным bundle
- ML Engine: slim Python base + только production зависимости
- Документирован размер каждого образа до и после оптимизации

---

---

# Auth и мультитенантность — (v0.5.0)

> Цель: система поддерживает регистрацию пользователей, аутентификацию через JWT и изоляцию данных между тенантами. Это фундамент для SaaS и BaaS модели.

---

## 👑 TL / SA / Video Engine

#### Архитектура мультитенантности (SA)
**Описание:** Спроектировать и задокументировать архитектуру tenant isolation: маршрутизация запросов, provisioning, lifecycle.

**AC:**
- ADR (Architecture Decision Record) в `docs/architecture/multi-tenancy.md`
- Описан flow: регистрация организации → создание schema + bucket → выдача API key
- Описана маршрутизация: JWT → `tenant_id` → routing к нужной schema/bucket
- Описан Qdrant filtering: `tenant_id` в payload каждого вектора

---

#### Tenant context в Video Engine
**Описание:** Video Engine принимает `tenant_id` в запросе `/process` и маршрутизирует S3-операции в tenant-specific bucket.

**AC:**
- `POST /process` принимает дополнительное поле `tenant_id`
- Download/upload S3 операции используют bucket `{tenant_id}-videos` (или переданный bucket name)
- Callback к Backend содержит `tenant_id`
- Обратная совместимость: если `tenant_id` не передан, используется default bucket

---

## ⚙️ Backend

#### Модуль аутентификации
**Описание:** Реализовать регистрацию, логин и JWT-аутентификацию пользователей.

**AC:**
- `POST /api/v1/auth/register` — регистрация (email, password, org_name)
- `POST /api/v1/auth/login` — логин → JWT access token + refresh token
- `POST /api/v1/auth/refresh` — обновление access token
- Пароли хранятся в bcrypt-хэше
- JWT содержит `user_id`, `tenant_id`, `role`
- Все `/api/v1/videos/*` эндпоинты защищены JWT (кроме auth)

---

#### Tenant model и provisioning
**Описание:** Модель тенанта (организации) и автоматический provisioning при регистрации.

**AC:**
- Таблица `tenants` (id, name, created_at, plan, status)
- Таблица `users` (id, email, password_hash, tenant_id, role)
- При регистрации автоматически создаётся: PostgreSQL schema `tenant_{id}`, MinIO bucket `tenant-{id}-videos`
- Миграция Flyway для tenant-specific таблиц выполняется при provisioning

---

#### Tenant middleware
**Описание:** Middleware, который извлекает `tenant_id` из JWT и устанавливает контекст для всех последующих операций.

**AC:**
- Каждый запрос проходит через tenant middleware
- `tenant_id` доступен во всех UseCases через контекст
- Все запросы к PostgreSQL маршрутизируются в `SET search_path TO tenant_{id}`
- Все запросы к MinIO используют bucket `tenant-{id}-videos`
- Попытка доступа к данным чужого тенанта → 403 Forbidden

---

## 💻 Frontend

#### Страницы регистрации и логина
**Описание:** UI для регистрации новой организации и входа в систему.

**AC:**
- Страница `/login` — email + password, кнопка "Войти"
- Страница `/register` — email + password + название организации
- Валидация форм (email format, password strength)
- Redirect на `/` после успешного логина
- Показ ошибок (неверный пароль, email занят)

---

#### Auth state management
**Описание:** Управление состоянием аутентификации в приложении.

**AC:**
- JWT хранится в httpOnly cookie или localStorage (обсудить с Backend)
- Zustand store для auth state (isAuthenticated, user, tenant)
- Axios interceptor: автоматически добавляет Authorization header
- Автоматический refresh token при 401
- Redirect на `/login` при истёкшей сессии

---

#### Protected routes
**Описание:** Защита маршрутов от неавторизованных пользователей.

**AC:**
- Все страницы кроме `/login` и `/register` требуют авторизации
- Неавторизованный пользователь → redirect на `/login`
- После логина → redirect на изначально запрошенную страницу

---

## 🧠 ML Engine

#### Tenant ID в Qdrant payload
**Описание:** Все векторы в Qdrant содержат `tenant_id` в payload. Поиск фильтруется по `tenant_id`.

**AC:**
- `POST /api/v1/process` принимает `tenant_id`
- Каждая точка в Qdrant содержит `tenant_id` в payload
- `POST /api/v1/search` принимает `tenant_id` и фильтрует результаты
- `DELETE /api/v1/vectors/{video_id}` также учитывает `tenant_id`
- Создан payload-index по `tenant_id` в обеих коллекциях

---

## 🐳 DevOps

#### Secrets management
**Описание:** Организовать безопасное хранение секретов (JWT secret, DB passwords, S3 keys).

**AC:**
- Секреты не хранятся в `.env` в plain text для prod
- Docker secrets или environment injection из защищённого хранилища
- Документирован процесс ротации секретов

---

---

# Production Readiness — (v0.6.0)

> Цель: система готова к работе в production — мониторинг, безопасность, бэкапы, rate limiting.

---

## 👑 TL / SA

#### Rate limiting design
**Описание:** Спроектировать стратегию rate limiting для API.

**AC:**
- Документирован план лимитов: upload (X req/min), search (Y req/min), общий (Z req/min)
- Лимиты привязаны к tenant/plan
- Определён механизм (in-memory counter или Redis)

---

#### Performance testing
**Описание:** Провести нагрузочное тестирование основных flow.

**AC:**
- Тест загрузки: 10 одновременных uploads
- Тест поиска: 50 одновременных search запросов
- Документированы p50, p95, p99 latency
- Выявлены и задокументированы bottleneck-и

---

## ⚙️ Backend

#### Rate limiting middleware
**Описание:** Реализовать rate limiting на уровне Backend API.

**AC:**
- Лимиты по tenant: upload, search, общий
- HTTP 429 Too Many Requests при превышении
- Заголовки: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`

---

#### API keys для BaaS
**Описание:** Альтернативная аутентификация через API key для программного доступа (BaaS клиенты).

**AC:**
- `POST /api/v1/settings/api-keys` — генерация API key привязанного к tenant
- Аутентификация через `Authorization: Bearer {api_key}` или `X-API-Key: {key}`
- Revoke API key
- Rate limiting применяется к API key так же, как к JWT

---

#### Webhooks для статусов видео
**Описание:** Отправка webhook-уведомлений при изменении статуса видео.

**AC:**
- Tenant может настроить webhook URL в settings
- POST на webhook URL с payload: `{video_id, status, timestamp}`
- Retry 3 попытки с exponential backoff
- HMAC подпись для верификации

---

## 💻 Frontend

#### Страница настроек
**Описание:** Страница `/settings` для управления организацией и API keys.

**AC:**
- Просмотр и редактирование названия организации
- Генерация и отзыв API keys (маскированное отображение)
- Настройка webhook URL
- Информация о текущем плане и использовании (кол-во видео, хранилище)

---

#### Responsive design и адаптивность
**Описание:** Полная адаптивность SPA для мобильных устройств.

**AC:**
- Корректное отображение на экранах 320px–1920px+
- Мобильное меню (hamburger)
- Touch-friendly элементы управления
- Тестирование на iOS Safari и Android Chrome

---

## 🧠 ML Engine

#### Health check с детализацией
**Описание:** Расширенный health endpoint с информацией о состоянии моделей и зависимостей.

**AC:**
- `GET /health` возвращает статус каждой модели (loaded/loading/error)
- Статус подключения к Qdrant и MinIO
- Версии загруженных моделей
- Uptime и последний обработанный запрос

---

#### Memory optimization
**Описание:** Оптимизировать потребление памяти ML Engine для работы на слабом сервере.

**AC:**
- Профилирование памяти с `memory_profiler`
- Выгрузка неиспользуемых моделей при простое (configurable timeout)
- FP16 inference по умолчанию на GPU
- Документировано потребление RAM для каждой модели

---

## 🐳 DevOps

#### Мониторинг (Prometheus + Grafana)
**Описание:** Развернуть стек мониторинга для всех сервисов.

**AC:**
- Prometheus собирает метрики со всех сервисов
- Grafana dashboard: CPU, RAM, request latency, error rate, queue length
- Базовые алерты: сервис down, error rate > 5%, disk space < 10%
- Метрики доступны через `/metrics` endpoint (каждый сервис)

---

#### Backup strategy
**Описание:** Автоматизированные бэкапы данных.

**AC:**
- PostgreSQL: ежедневный `pg_dump` с ротацией (7 дней)
- MinIO: mc mirror для зеркалирования (или snapshot)
- Qdrant: snapshot через API
- Скрипт восстановления из бэкапа протестирован

---

#### TLS / HTTPS
**Описание:** Настроить HTTPS для production.

**AC:**
- Let's Encrypt сертификат через Nginx или Certbot
- HTTP → HTTPS redirect
- HSTS заголовки
- Все inter-service коммуникации внутри Docker network (не требуют TLS)

---

---

# Release — (v1.0.0)

> Цель: продукт задокументирован, отполирован и готов к продаже. Первый стабильный релиз.

---

## 👑 TL / SA

#### API документация
**Описание:** Полная публичная API документация для клиентов.

**AC:**
- OpenAPI 3.1 спецификация покрывает все публичные endpoints
- Swagger UI доступен по `/docs`
- Примеры запросов и ответов для каждого endpoint
- Описание ошибок и кодов состояний

---

#### Deployment guide
**Описание:** Гайд по развертыванию OmniSearch для клиентов (self-hosted).

**AC:**
- Пошаговая инструкция: от клонирования до первого поиска
- Требования к серверу (CPU, RAM, GPU, диск)
- Конфигурация через `.env` задокументирована
- Troubleshooting секция (частые проблемы)

---

#### Release notes
**Описание:** Подготовить release notes для v1.0.0.

**AC:**
- Changelog от 0.2.0 до 1.0.0
- Список ключевых features
- Known limitations
- Upgrade path (если клиент уже на 0.x)

---

## ⚙️ Backend

#### Database migrations tooling
**Описание:** Финализировать систему миграций для безопасных обновлений в production.

**AC:**
- Все миграции идемпотентны
- `./gradlew flywayMigrate` работает на пустой и существующей БД
- Документирован процесс обновления версии в production

---

#### Финальный API polish
**Описание:** Ревизия и унификация всех API endpoints.

**AC:**
- Единообразные коды ошибок и формат ответов во всех endpoints
- Валидация всех входных данных с информативными сообщениями
- OpenAPI спецификация генерируется из кода (или синхронизирована)
- Deprecation headers для устаревших endpoints (если есть)

---

## 💻 Frontend

#### Onboarding flow
**Описание:** Интерактивный onboarding для новых пользователей.

**AC:**
- При первом входе — приветственный экран с кратким гайдом
- Шаги: загрузите видео → дождитесь обработки → попробуйте поиск
- Можно пропустить ("Больше не показывать")

---

#### UI/UX финальная полировка
**Описание:** Финальный проход по всему интерфейсу.

**AC:**
- Консистентные отступы, типографика, цвета
- Все loading/error/empty states реализованы на каждой странице
- Анимации и переходы между страницами (Framer Motion или CSS transitions)
- Favicon, мета-теги, title для каждой страницы

---

## 🧠 ML Engine

#### Search quality validation
**Описание:** Формальная валидация качества поиска перед релизом.

**AC:**
- Тестовый набор: минимум 20 видео, 50 поисковых запросов
- Метрики: Precision@5, MRR (Mean Reciprocal Rank)
- Baseline задокументирован и включён в release notes
- Регрессионный тест: `pytest` валидирует что метрики не упали ниже порога

---

#### Model versioning
**Описание:** Зафиксировать и задокументировать версии всех ML-моделей.

**AC:**
- `models.yaml` — реестр используемых моделей (name, version, source, hash)
- Модели скачиваются из зафиксированных источников (pinned versions)
- Инструкция по обновлению модели без даунтайма

---

## 🐳 DevOps

#### Deployment automation
**Описание:** Автоматизированный деплой на production-сервер.

**AC:**
- `deploy-prod.yml` — GitHub Actions workflow для деплоя
- Zero-downtime deploy через rolling update (docker compose pull + up)
- Smoke test после деплоя (health check всех сервисов)
- Rollback: `git revert` + `docker compose pull` + re-deploy

---

#### Production monitoring dashboards
**Описание:** Grafana dashboards для операционного мониторинга.

**AC:**
- Dashboard "System Overview": все сервисы, uptime, error rate
- Dashboard "Video Pipeline": очередь, время обработки, success/error rate
- Dashboard "Search": latency, requests/sec, top queries
- Alerting rules настроены и работают
