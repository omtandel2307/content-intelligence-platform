package com.contentintelligence.platform.rag.dto;

import com.contentintelligence.platform.rag.RagChatMessage;
import com.contentintelligence.platform.rag.RagChatRole;

import java.time.Instant;
import java.util.UUID;

public record RagChatMessageResponse(
        UUID id,
        String videoId,
        RagChatRole role,
        String content,
        String provider,
        String model,
        Instant createdAt
) {

    public static RagChatMessageResponse from(RagChatMessage message) {
        return new RagChatMessageResponse(
                message.getId(),
                message.getVideoId(),
                message.getRole(),
                message.getContent(),
                message.getProvider(),
                message.getModel(),
                message.getCreatedAt()
        );
    }
}
