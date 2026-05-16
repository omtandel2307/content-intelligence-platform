package com.contentintelligence.platform.transcript;

import com.contentintelligence.platform.transcript.dto.TranscriptResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/content/{videoId}/transcript")
public class ContentTranscriptController {

    private final ContentTranscriptService transcriptService;

    public ContentTranscriptController(ContentTranscriptService transcriptService) {
        this.transcriptService = transcriptService;
    }

    @GetMapping
    public ResponseEntity<TranscriptResponse> getTranscript(
            @PathVariable @NotBlank String videoId
    ) {
        return transcriptService.getTranscript(videoId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public TranscriptResponse fetchTranscript(
            @RequestHeader("X-Account-Id") String accountId,
            @PathVariable @NotBlank String videoId
    ) {
        return transcriptService.fetchAndSaveTranscript(accountId, videoId);
    }
}
