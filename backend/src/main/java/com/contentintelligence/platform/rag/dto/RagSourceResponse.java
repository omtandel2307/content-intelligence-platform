package com.contentintelligence.platform.rag.dto;

public record RagSourceResponse(
        int chunkIndex,
        double score,
        String textPreview
) {
}
