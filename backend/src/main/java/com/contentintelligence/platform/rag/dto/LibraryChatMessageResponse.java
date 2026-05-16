package com.contentintelligence.platform.rag.dto;

import com.contentintelligence.platform.rag.LibraryChatMessage;

import java.time.Instant;
import java.util.UUID;

public record LibraryChatMessageResponse(
        UUID id,
        String role,
        String content,
        String provider,
        String model,
        Instant createdAt
) {

    public static LibraryChatMessageResponse from(LibraryChatMessage message) {
        return new LibraryChatMessageResponse(
                message.getId(),
                message.getRole().name(),
                message.getContent(),
                message.getProvider(),
                message.getModel(),
                message.getCreatedAt()
        );
    }
}
