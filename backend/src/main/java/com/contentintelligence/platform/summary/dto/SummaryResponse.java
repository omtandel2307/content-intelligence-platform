package com.contentintelligence.platform.summary.dto;

import com.contentintelligence.platform.summary.ContentSummary;
import com.contentintelligence.platform.summary.SummaryStatus;

import java.time.Instant;

public record SummaryResponse(
        String videoId,
        String summaryText,
        String model,
        SummaryStatus status,
        String failureReason,
        Instant generatedAt
) {
    public static SummaryResponse from(ContentSummary summary) {
        return new SummaryResponse(
                summary.getVideoId(),
                summary.getSummaryText(),
                summary.getModel(),
                summary.getStatus(),
                summary.getFailureReason(),
                summary.getGeneratedAt()
        );
    }
}
