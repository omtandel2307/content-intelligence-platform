package com.contentintelligence.platform.summary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "content_summaries")
public class ContentSummary {

    @Id
    private String videoId;

    @Column(columnDefinition = "text")
    private String summaryText;

    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SummaryStatus status;

    private String failureReason;

    @Column(nullable = false)
    private Instant generatedAt;

    protected ContentSummary() {
    }

    public ContentSummary(
            String videoId,
            String summaryText,
            String model,
            SummaryStatus status,
            String failureReason
    ) {
        this.videoId = videoId;
        this.summaryText = summaryText;
        this.model = model;
        this.status = status;
        this.failureReason = failureReason;
        this.generatedAt = Instant.now();
    }

    public String getVideoId() {
        return videoId;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public String getModel() {
        return model;
    }

    public SummaryStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void replaceWith(
            String summaryText,
            String model,
            SummaryStatus status,
            String failureReason
    ) {
        this.summaryText = summaryText;
        this.model = model;
        this.status = status;
        this.failureReason = failureReason;
        this.generatedAt = Instant.now();
    }
}
