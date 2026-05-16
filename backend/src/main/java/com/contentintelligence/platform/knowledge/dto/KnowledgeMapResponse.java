package com.contentintelligence.platform.knowledge.dto;

import java.time.Instant;
import java.util.List;

public record KnowledgeMapResponse(
        String accountId,
        String provider,
        String model,
        Instant generatedAt,
        List<KnowledgeMapNodeResponse> nodes,
        List<KnowledgeMapLinkResponse> links
) {
}
