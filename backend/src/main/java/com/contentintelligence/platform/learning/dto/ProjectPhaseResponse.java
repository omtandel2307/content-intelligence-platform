package com.contentintelligence.platform.learning.dto;

import java.util.List;

public record ProjectPhaseResponse(
        String title,
        String outcome,
        List<String> tasks
) {
}
