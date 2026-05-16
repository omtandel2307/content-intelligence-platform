package com.contentintelligence.platform.transcript.dto;

import com.contentintelligence.platform.transcript.ContentTranscript;
import com.contentintelligence.platform.transcript.TranscriptStatus;

import java.time.Instant;

public record TranscriptResponse(
        String videoId,
        String transcriptText,
        String languageCode,
        TranscriptStatus status,
        String failureReason,
        Instant fetchedAt
) {
    public static TranscriptResponse from(ContentTranscript transcript) {
        return new TranscriptResponse(
                transcript.getVideoId(),
                transcript.getTranscriptText(),
                transcript.getLanguageCode(),
                transcript.getStatus(),
                transcript.getFailureReason(),
                transcript.getFetchedAt()
        );
    }
}
