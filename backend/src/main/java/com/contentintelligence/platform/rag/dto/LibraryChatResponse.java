package com.contentintelligence.platform.rag.dto;

import java.util.List;

public record LibraryChatResponse(
        String answer,
        String provider,
        String model,
        List<LibraryChatMessageResponse> messages,
        List<LibraryRagSourceResponse> sources
) {
}
