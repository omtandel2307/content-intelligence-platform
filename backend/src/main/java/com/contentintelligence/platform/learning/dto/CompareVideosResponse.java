package com.contentintelligence.platform.learning.dto;

import java.time.Instant;
import java.util.List;

public record CompareVideosResponse(
        String provider,
        String model,
        Instant generatedAt,
        VideoBriefResponse firstVideo,
        VideoBriefResponse secondVideo,
        List<String> commonGround,
        List<String> firstVideoStrengths,
        List<String> secondVideoStrengths,
        List<String> disagreementsOrGaps,
        String bestForBeginners,
        String bestForDepth,
        String learningRecommendation
) {
}
