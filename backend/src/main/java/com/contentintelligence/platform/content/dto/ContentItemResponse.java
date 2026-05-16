package com.contentintelligence.platform.content.dto;

import com.contentintelligence.platform.content.ContentItem;
import com.contentintelligence.platform.transcript.ContentTranscript;
import com.contentintelligence.platform.transcript.TranscriptStatus;

import java.time.Instant;

public record ContentItemResponse(
        String videoId,
        String title,
        String description,
        String channelTitle,
        String thumbnailUrl,
        String publishedAt,
        String duration,
        String viewCount,
        String likeCount,
        Instant savedAt,
        String transcriptStatus,
        boolean transcriptReady
) {
    public static ContentItemResponse from(ContentItem item) {
        return from(item, null);
    }

    public static ContentItemResponse from(
            ContentItem item,
            ContentTranscript transcript
    ) {
        String transcriptStatus = transcript == null
                ? "NOT_FETCHED"
                : transcript.getStatus().name();

        return new ContentItemResponse(
                item.getVideoId(),
                item.getTitle(),
                item.getDescription(),
                item.getChannelTitle(),
                item.getThumbnailUrl(),
                item.getPublishedAt(),
                item.getDuration(),
                item.getViewCount(),
                item.getLikeCount(),
                item.getSavedAt(),
                transcriptStatus,
                transcript != null && transcript.getStatus() == TranscriptStatus.READY
        );
    }
}
