package com.contentintelligence.platform.knowledge.dto;

import java.util.List;

public record KnowledgeMapNodeResponse(
        String id,
        String label,
        String summary,
        int importance,
        List<String> videoIds
) {
}
