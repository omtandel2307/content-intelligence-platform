package com.contentintelligence.platform.summary;

import com.contentintelligence.platform.summary.dto.SummaryResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/content/{videoId}/summary")
public class ContentSummaryController {

    private final ContentSummaryService summaryService;

    public ContentSummaryController(ContentSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping
    public ResponseEntity<SummaryResponse> getSummary(
            @PathVariable @NotBlank String videoId
    ) {
        return summaryService.getSummary(videoId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public SummaryResponse generateSummary(@PathVariable @NotBlank String videoId) {
        return summaryService.generateSummary(videoId);
    }
}
