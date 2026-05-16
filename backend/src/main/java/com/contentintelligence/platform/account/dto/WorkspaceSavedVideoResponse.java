package com.contentintelligence.platform.account.dto;

import java.time.Instant;

public record WorkspaceSavedVideoResponse(
        String videoId,
        String title,
        String channelTitle,
        String thumbnailUrl,
        Instant savedAt
) {
}
