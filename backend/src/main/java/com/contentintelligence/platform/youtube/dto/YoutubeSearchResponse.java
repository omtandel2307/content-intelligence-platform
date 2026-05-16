package com.contentintelligence.platform.youtube.dto;

import java.util.List;

public record YoutubeSearchResponse(
        String query,
        List<YoutubeSearchItem> items
) {
}
