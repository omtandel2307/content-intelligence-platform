package com.contentintelligence.platform.learning.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record CompareVideosRequest(
        @Size(min = 2, max = 2, message = "Choose exactly two videos to compare.")
        List<String> videoIds
) {
}
