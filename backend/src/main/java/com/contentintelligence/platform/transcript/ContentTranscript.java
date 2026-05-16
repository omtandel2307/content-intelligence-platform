package com.contentintelligence.platform.transcript;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "content_transcripts")
public class ContentTranscript {

    @Id
    private String videoId;

    @Column(columnDefinition = "text")
    private String transcriptText;

    private String languageCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TranscriptStatus status;

    private String failureReason;

    @Column(nullable = false)
    private Instant fetchedAt;

    protected ContentTranscript() {
    }

    public ContentTranscript(
            String videoId,
            String transcriptText,
            String languageCode,
            TranscriptStatus status,
            String failureReason
    ) {
        this.videoId = videoId;
        this.transcriptText = transcriptText;
        this.languageCode = languageCode;
        this.status = status;
        this.failureReason = failureReason;
        this.fetchedAt = Instant.now();
    }

    public String getVideoId() {
        return videoId;
    }

    public String getTranscriptText() {
        return transcriptText;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public TranscriptStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void replaceWith(
            String transcriptText,
            String languageCode,
            TranscriptStatus status,
            String failureReason
    ) {
        this.transcriptText = transcriptText;
        this.languageCode = languageCode;
        this.status = status;
        this.failureReason = failureReason;
        this.fetchedAt = Instant.now();
    }
}
