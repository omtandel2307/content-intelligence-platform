package com.contentintelligence.platform.youtube;

import com.contentintelligence.platform.youtube.dto.YoutubeSearchResponse;
import com.contentintelligence.platform.youtube.dto.YoutubeVideoDetails;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/youtube")
public class YoutubeSearchController {

    private final YoutubeSearchService youtubeSearchService;

    public YoutubeSearchController(YoutubeSearchService youtubeSearchService) {
        this.youtubeSearchService = youtubeSearchService;
    }

    @GetMapping("/search")
    public YoutubeSearchResponse search(@RequestParam @NotBlank String q) {
        return youtubeSearchService.search(q);
    }

    @GetMapping("/videos/{videoId}")
    public YoutubeVideoDetails getVideoDetails(@PathVariable @NotBlank String videoId) {
        return youtubeSearchService.getVideoDetails(videoId);
    }
}
