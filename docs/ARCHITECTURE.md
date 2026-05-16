# Architecture

## Initial direction

- `frontend`: Next.js application for search, discovery, dashboard, and chat UX
- `backend`: Spring Boot API for YouTube search, ingestion jobs, summaries, and saved content
- `postgres`: primary application database
- `redis`: cache, job state, and rate limiting

## Near-term modules

- `youtube`: search and metadata lookup
- `content`: saved content records
- `summary`: summary generation pipeline
- `chat`: content-aware chat
- `jobs`: asynchronous processing

## API first milestone

`GET /api/youtube/search?q=spring+boot`

Response shape:

```json
{
  "query": "spring boot",
  "items": [
    {
      "videoId": "abc123",
      "title": "Spring Boot Tutorial",
      "description": "Learn Spring Boot",
      "channelTitle": "Example Channel",
      "thumbnailUrl": "https://...",
      "publishedAt": "2026-04-16T00:00:00Z"
    }
  ]
}
```
