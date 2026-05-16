package com.contentintelligence.platform.learning.dto;

import java.time.Instant;

public record LearningTimelineEventResponse(
        String type,
        String title,
        String description,
        String videoId,
        Instant occurredAt
) {
}
