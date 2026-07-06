# Content Intelligence Platform

A full-stack AI learning workspace that turns YouTube videos into searchable, summarized, quiz-ready, and chat-ready study material.

The app lets a user search YouTube, save videos to an account-specific library, fetch transcripts, generate summaries, chat with individual videos, chat across the entire saved library, build knowledge maps, compare videos, generate project plans, and track learning activity over time.

## Highlights

- **YouTube search inside the app** using the YouTube Data API.
- **Account-based workspaces** so each user keeps their own saved videos and chat history.
- **Saved video library** with concise descriptions and direct video workspaces.
- **Transcript ingestion** using a Java YouTube transcript library.
- **OpenAI summaries** for clean study summaries from transcripts.
- **OpenAI RAG chat** for video-level questions over transcript chunks.
- **Ask across all saved videos** with account-level RAG.
- **AI quizzes** generated from transcripts and graded in-app.
- **AI knowledge map** that extracts connected learning topics from saved videos.
- **Compare videos** to identify overlap, gaps, strengths, and recommended watch order.
- **Project builder** that converts saved video knowledge into a practical build plan.
- **Learning timeline** showing saves, transcripts, summaries, quizzes, and AI chats.

## Tech Stack

| Layer | Technology |
| --- | --- |
| Frontend | Next.js 15, React 19, TypeScript |
| Backend | Spring Boot 4, Java 21, Maven |
| Database | PostgreSQL |
| AI | OpenAI Responses API + embeddings |
| Infra | Docker Compose for Postgres and Redis |

## Architecture

```text
content-intelligence-platform/
  frontend/   Next.js UI for search, library, video workspace, and AI dashboard
  backend/    Spring Boot API for YouTube, content, transcripts, summaries, RAG, quizzes, and learning tools
  infra/      Docker Compose services for Postgres and Redis
  docs/       Architecture notes
```

High-level flow:

```text
YouTube API -> Spring Boot -> PostgreSQL -> Next.js UI
                         -> OpenAI summaries, embeddings, RAG, and learning tools
```

## Core Features

### Search And Save

Users can search YouTube directly from the homepage, open a video workspace, and save useful videos into their account library.

### Video Workspace

Each video has a workspace for:

- metadata and thumbnail
- transcript fetching
- OpenAI summary generation
- OpenAI RAG chat
- true/false quiz generation and grading

### My Workspace

The workspace dashboard includes:

- recent saved videos
- recent chats
- library-wide AI chat
- knowledge map generation
- video comparison
- project plan generation
- learning timeline

## Prerequisites

Install these before running locally:

- Java 21+
- Maven
- Node.js 20+
- Docker Desktop

## Environment Variables

Copy `.env.example` and create:

- `backend/.env`
- `frontend/.env.local`

Example backend values:

```bash
YOUTUBE_API_KEY=your_youtube_api_key
OPENAI_API_KEY=your_openai_api_key
OPENAI_MODEL=gpt-5.4-mini
OPENAI_EMBEDDING_MODEL=text-embedding-3-small
DATABASE_URL=jdbc:postgresql://localhost:55432/content_platform
DATABASE_USERNAME=content_platform
DATABASE_PASSWORD=content_platform
```

Example frontend value:

```bash
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

> Do not commit real API keys. `.env`, `backend/.env`, and `frontend/.env.local` are ignored by Git.

## Running Locally

Start infrastructure:

```bash
cd infra
docker compose up -d
```

Start the backend:

```bash
cd backend
mvn spring-boot:run
```

Start the frontend:

```bash
cd frontend
npm install
npm run dev
```

Open:

```text
http://localhost:3000
```

## Useful Routes

| Route | Purpose |
| --- | --- |
| `/` | Search-first homepage |
| `/library` | Saved videos |
| `/workspace` | Account AI dashboard |
| `/video/{videoId}` | Individual video workspace |

## Key API Areas

| Area | Example |
| --- | --- |
| YouTube search | `GET /api/youtube/search?q=spring+boot` |
| Saved content | `GET /api/content` |
| Transcript | `POST /api/content/{videoId}/transcript` |
| Summary | `POST /api/content/{videoId}/summary` |
| Video RAG chat | `POST /api/content/{videoId}/rag/chat` |
| Library RAG chat | `POST /api/accounts/{accountId}/library-rag/chat` |
| Knowledge map | `POST /api/accounts/{accountId}/knowledge-map` |
| Compare videos | `POST /api/accounts/{accountId}/learning/compare` |
| Project plan | `POST /api/accounts/{accountId}/learning/project-plan` |
| Timeline | `GET /api/accounts/{accountId}/learning/timeline` |

Account-scoped endpoints expect the selected account id through the UI or `X-Account-Id` where applicable.

## Verification

Backend compile:

```bash
cd backend
mvn -DskipTests compile
```

Frontend type check:

```bash
cd frontend
npx tsc --noEmit
```

## Current Limitations

- Account switching is lightweight and does not yet include password-based authentication.
- Transcript availability depends on YouTube captions being available for a video.
- AI feature availability depends on the configured OpenAI API key and model access.
- Redis is included in local infrastructure but is not deeply used yet.

## Roadmap

- Real authentication and protected accounts.
- Persistent generated knowledge maps and project plans.
- Quiz attempt history and weak-area detection.
- Better async job handling for long-running AI tasks.
- Deployment profile for cloud hosting.

## License

No license has been selected yet.
