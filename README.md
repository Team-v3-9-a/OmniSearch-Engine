# OmniSearch Engine

OmniSearch Engine is an On-Premise multimodal Retrieval-Augmented Generation (RAG) system designed for semantic video search. 
It allows users to upload raw video archives (knowledge bases) and perform natural language queries to retrieve highly relevant video segments.

## Architecture Overview

The system is built on a microservices architecture, strictly separating I/O-bound tasks from Compute-bound tasks. It utilizes two distinct pipelines:

1. **Ingestion Pipeline (Asynchronous):** 
   Handles heavy media processing. Videos are uploaded to an S3-compatible storage. The Control Plane orchestrates the Video Engine to extract audio streams and sample frames, followed by the ML Engine which transcribes audio, chunks the text (Sliding Window), generates vector embeddings, and stores them in a Vector Database.
2. **Retrieval Pipeline (Synchronous):** 
   Optimized for low latency (p95 < 500ms). User queries are vectorized in real-time and matched against the Vector Database using HNSW indexes to return exact timestamps and video metadata.

## Technology Stack

* **Control Plane (Backend):** Kotlin, Ktor, Exposed ORM.
* **Video Engine:** Golang, GoCV (OpenCV bindings), FFmpeg (Subprocess execution).
* **ML Engine:** Python, PyTorch, Whisper (Transcription), Sentence-Transformers (Text/Vision Embeddings).
* **Frontend:** React / Svelte.
* **Storage & Databases:** 
  * PostgreSQL (Relational metadata and processing states).
  * Qdrant / ChromaDB (Vector indices).
  * MinIO / S3 (Single source of truth for raw `.mp4`, `.wav`, and `.jpg` artifacts).
* **Infrastructure:** Docker, Docker Compose, GitHub Actions.
* **Documentation:** Docs-as-Code (Markdown, OpenAPI 3.0, Mermaid.js).

## Repository Structure

## Repository Structure

```text
├── backend/           # Ktor Control Plane and REST API
├── video-engine/      # Golang CLI for media extraction (Audio/Frames)
├── ml-engine/         # Python workers for transcription and vectorization
├── frontend/          # Web UI for ingestion and search
├── docs/              # Architecture Decision Records (ADR), NFRs, and OpenAPI specs
└── docker-compose.yml # Unified local development environment
```

## Current Status (MVP Phase)

* **Sprint 1 Completed:** Base infrastructure is containerized. ML models (Whisper/Transformers) are validated. Ktor base is initialized. API contracts (OpenAPI) and non-functional requirements (NFRs) are formalized. The Video Engine is currently being migrated to Golang for better memory safety and deployment simplicity.

## Getting Started

*(Detailed setup instructions for the local Docker environment will be added here once the IPC mechanisms are finalized).*

For local development, ensure you have the following installed:
* Docker & Docker Compose
* Go 1.21+ (with GCC and OpenCV 4.x for the Video Engine)
* Java 17
* Python 3.10+