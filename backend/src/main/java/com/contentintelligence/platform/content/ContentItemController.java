package com.contentintelligence.platform.content;

import com.contentintelligence.platform.content.dto.ContentItemResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/content")
public class ContentItemController {

    private final ContentItemService contentItemService;

    public ContentItemController(ContentItemService contentItemService) {
        this.contentItemService = contentItemService;
    }

    @GetMapping
    public List<ContentItemResponse> listSavedContent(
            @RequestHeader("X-Account-Id") String accountId
    ) {
        return contentItemService.listSavedContent(accountId);
    }

    @PostMapping("/youtube/{videoId}")
    public ContentItemResponse saveYoutubeVideo(
            @RequestHeader("X-Account-Id") String accountId,
            @PathVariable @NotBlank String videoId
    ) {
        return contentItemService.saveFromYoutube(accountId, videoId);
    }
}
