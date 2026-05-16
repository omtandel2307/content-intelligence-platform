package com.contentintelligence.platform.youtube.dto;

public record YoutubeSearchItem(
        String videoId,
        String title,
        String description,
        String channelTitle,
        String thumbnailUrl,
        String publishedAt
) {
}
