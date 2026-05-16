package com.contentintelligence.platform.knowledge.dto;

public record KnowledgeMapLinkResponse(
        String source,
        String target,
        String relation,
        int strength
) {
}
