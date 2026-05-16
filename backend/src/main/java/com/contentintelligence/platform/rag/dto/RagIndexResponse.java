package com.contentintelligence.platform.rag.dto;

import java.time.Instant;

public record RagIndexResponse(
        String videoId,
        int chunkCount,
        String embeddingModel,
        Instant indexedAt
) {
}
