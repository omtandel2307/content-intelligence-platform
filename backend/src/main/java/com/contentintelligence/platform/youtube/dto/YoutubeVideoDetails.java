package com.contentintelligence.platform.youtube.dto;

import java.io.Serializable;

public record YoutubeVideoDetails(
        String videoId,
        String title,
        String description,
        String channelTitle,
        String thumbnailUrl,
        String publishedAt,
        String duration,
        String viewCount,
        String likeCount
) implements Serializable {
}
