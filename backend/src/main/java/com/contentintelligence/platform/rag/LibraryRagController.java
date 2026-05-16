package com.contentintelligence.platform.rag;

import com.contentintelligence.platform.rag.dto.LibraryChatMessageResponse;
import com.contentintelligence.platform.rag.dto.LibraryChatResponse;
import com.contentintelligence.platform.rag.dto.LocalChatRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts/{accountId}/library-rag")
public class LibraryRagController {

    private final LocalRagService localRagService;

    public LibraryRagController(LocalRagService localRagService) {
        this.localRagService = localRagService;
    }

    @GetMapping("/chat")
    public List<LibraryChatMessageResponse> getLibraryChatHistory(
            @PathVariable String accountId
    ) {
        return localRagService.getLibraryChatHistory(accountId);
    }

    @PostMapping("/chat")
    public LibraryChatResponse chatWithSavedLibrary(
            @PathVariable String accountId,
            @Valid @RequestBody LocalChatRequest request
    ) {
        return localRagService.chatWithSavedLibrary(accountId, request.message());
    }
}
