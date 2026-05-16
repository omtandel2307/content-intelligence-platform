package com.contentintelligence.platform.account.dto;

import java.time.Instant;

public record WorkspaceChatResponse(
        String videoId,
        String title,
        String role,
        String contentPreview,
        Instant createdAt
) {
}
