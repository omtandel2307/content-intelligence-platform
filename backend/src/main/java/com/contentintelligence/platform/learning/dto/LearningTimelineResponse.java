package com.contentintelligence.platform.learning.dto;

import java.util.List;

public record LearningTimelineResponse(
        String accountId,
        List<LearningTimelineEventResponse> events
) {
}
