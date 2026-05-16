package com.contentintelligence.platform.learning.dto;

import java.time.Instant;
import java.util.List;

public record ProjectPlanResponse(
        String provider,
        String model,
        Instant generatedAt,
        String title,
        String objective,
        List<String> stack,
        List<ProjectPhaseResponse> phases,
        List<String> stretchGoals
) {
}
