package com.contentintelligence.platform.rag.dto;

public record LibraryRagSourceResponse(
        String videoId,
        String title,
        String channelTitle,
        int chunkIndex,
        double score,
        String textPreview
) {
}
