package com.contentintelligence.platform.rag.dto;

import jakarta.validation.constraints.NotBlank;

public record LocalChatRequest(
        @NotBlank(message = "Message is required.")
        String message
) {
}
