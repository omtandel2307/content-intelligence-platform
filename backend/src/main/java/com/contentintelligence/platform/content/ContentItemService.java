package com.contentintelligence.platform.content;

import com.contentintelligence.platform.account.UserAccountService;
import com.contentintelligence.platform.content.dto.ContentItemResponse;
import com.contentintelligence.platform.transcript.ContentTranscript;
import com.contentintelligence.platform.transcript.ContentTranscriptRepository;
import com.contentintelligence.platform.youtube.YoutubeSearchService;
import com.contentintelligence.platform.youtube.dto.YoutubeVideoDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ContentItemService {

    private final ContentItemRepository contentItemRepository;
    private final AccountSavedContentRepository savedContentRepository;
    private final ContentTranscriptRepository transcriptRepository;
    private final YoutubeSearchService youtubeSearchService;
    private final UserAccountService accountService;

    public ContentItemService(
            ContentItemRepository contentItemRepository,
            AccountSavedContentRepository savedContentRepository,
            ContentTranscriptRepository transcriptRepository,
            YoutubeSearchService youtubeSearchService,
            UserAccountService accountService
    ) {
        this.contentItemRepository = contentItemRepository;
        this.savedContentRepository = savedContentRepository;
        this.transcriptRepository = transcriptRepository;
        this.youtubeSearchService = youtubeSearchService;
        this.accountService = accountService;
    }

    public ContentItemResponse saveFromYoutube(String accountId, String videoId) {
        accountService.requireAccount(accountId);
        ContentItem item = findOrCreateVideo(videoId);
        savedContentRepository.findByAccountIdAndVideoId(accountId, videoId)
                .orElseGet(() -> savedContentRepository.save(
                        new AccountSavedContent(accountId, videoId)
                ));

        return ContentItemResponse.from(item);
    }

    public ContentItem findOrCreateVideo(String videoId) {
        return contentItemRepository.findById(videoId)
                .orElseGet(() -> {
                    YoutubeVideoDetails details = youtubeSearchService.getVideoDetails(videoId);
                    return contentItemRepository.save(new ContentItem(
                            details.videoId(),
                            details.title(),
                            details.description(),
                            details.channelTitle(),
                            details.thumbnailUrl(),
                            details.publishedAt(),
                            details.duration(),
                            details.viewCount(),
                            details.likeCount()
                    ));
                });
    }

    public List<ContentItemResponse> listSavedContent(String accountId) {
        accountService.requireAccount(accountId);
        List<AccountSavedContent> savedItems =
                savedContentRepository.findAllByAccountIdOrderBySavedAtDesc(accountId);
        Map<String, ContentItem> contentByVideoId = contentItemRepository
                .findAllById(savedItems.stream().map(AccountSavedContent::getVideoId).toList())
                .stream()
                .collect(Collectors.toMap(ContentItem::getVideoId, Function.identity()));
        Map<String, ContentTranscript> transcriptByVideoId = transcriptRepository
                .findAllById(savedItems.stream().map(AccountSavedContent::getVideoId).toList())
                .stream()
                .collect(Collectors.toMap(ContentTranscript::getVideoId, Function.identity()));

        return savedItems.stream()
                .map(saved -> contentByVideoId.get(saved.getVideoId()))
                .filter(item -> item != null)
                .map(item -> ContentItemResponse.from(
                        item,
                        transcriptByVideoId.get(item.getVideoId())
                ))
                .toList();
    }
}
