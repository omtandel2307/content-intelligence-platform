# Content Intelligence Platform

AI-powered platform for discovering, ingesting, summarizing, and chatting with long-form content.

## Stack

- `frontend/`: Next.js app
- `backend/`: Spring Boot API
- `infra/`: local infrastructure with Postgres and Redis
- `docs/`: architecture and planning notes

## First milestone

- Search YouTube videos inside the app
- Select a video
- Prepare the project for transcript, summary, and chat pipelines

## Local structure

```text
content-intelligence-platform/
  frontend/
  backend/
  infra/
  docs/
```

## Environment

Copy `.env.example` into the env files you want to use later:

- `frontend/.env.local`
- `.env` at the project root for backend keys and database settings

Example backend `.env`:

```bash
YOUTUBE_API_KEY=your_youtube_api_key
OPENAI_API_KEY=your_openai_api_key
OPENAI_MODEL=gpt-5.4-mini
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_CHAT_MODEL=llama3.2:3b
OLLAMA_EMBEDDING_MODEL=embeddinggemma
DATABASE_URL=jdbc:postgresql://localhost:55432/content_platform
DATABASE_USERNAME=content_platform
DATABASE_PASSWORD=content_platform
```

For local RAG, install Ollama and pull the starter models:

```bash
ollama pull llama3.2:3b
ollama pull embeddinggemma
```

## Getting started

### Frontend

```bash
cd frontend
npm install
npm run dev
```

### Backend

```bash
cd backend
mvn spring-boot:run
```

### Infrastructure

```bash
cd infra
docker compose up -d
```

## Notes

- The backend YouTube search endpoint is scaffolded at `/api/youtube/search`.
- Add `YOUTUBE_API_KEY` before expecting live search results.
- Local RAG uses `/api/content/{videoId}/rag/index` and `/api/content/{videoId}/rag/chat`.
- OpenAI summaries still work separately; local RAG answers use Ollama.
