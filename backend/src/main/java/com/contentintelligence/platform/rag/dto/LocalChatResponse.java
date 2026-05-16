package com.contentintelligence.platform.rag.dto;

import java.util.List;

public record LocalChatResponse(
        String videoId,
        String answer,
        String provider,
        String model,
        List<RagChatMessageResponse> messages,
        List<RagSourceResponse> sources
) {
}
