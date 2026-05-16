package com.contentintelligence.platform.rag;

import com.contentintelligence.platform.rag.dto.LocalChatRequest;
import com.contentintelligence.platform.rag.dto.LocalChatResponse;
import com.contentintelligence.platform.rag.dto.RagChatMessageResponse;
import com.contentintelligence.platform.rag.dto.RagIndexResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/content/{videoId}/rag")
public class LocalRagController {

    private final LocalRagService localRagService;

    public LocalRagController(LocalRagService localRagService) {
        this.localRagService = localRagService;
    }

    @GetMapping("/index")
    public RagIndexResponse getIndexStatus(@PathVariable String videoId) {
        return localRagService.getIndexStatus(videoId);
    }

    @PostMapping("/index")
    public RagIndexResponse indexTranscript(@PathVariable String videoId) {
        return localRagService.indexTranscript(videoId);
    }

    @PostMapping("/chat")
    public LocalChatResponse chatWithVideo(
            @RequestHeader("X-Account-Id") String accountId,
            @PathVariable String videoId,
            @Valid @RequestBody LocalChatRequest request
    ) {
        return localRagService.chatWithVideo(accountId, videoId, request.message());
    }

    @GetMapping("/chat")
    public List<RagChatMessageResponse> getChatHistory(
            @RequestHeader("X-Account-Id") String accountId,
            @PathVariable String videoId
    ) {
        return localRagService.getChatHistory(accountId, videoId);
    }
}
