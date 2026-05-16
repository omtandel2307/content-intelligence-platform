package com.contentintelligence.platform.knowledge;

import com.contentintelligence.platform.knowledge.dto.KnowledgeMapResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts/{accountId}/knowledge-map")
public class KnowledgeMapController {

    private final KnowledgeMapService knowledgeMapService;

    public KnowledgeMapController(KnowledgeMapService knowledgeMapService) {
        this.knowledgeMapService = knowledgeMapService;
    }

    @PostMapping
    public KnowledgeMapResponse generateKnowledgeMap(@PathVariable String accountId) {
        return knowledgeMapService.generateKnowledgeMap(accountId);
    }
}
