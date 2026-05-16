package com.contentintelligence.platform.transcript;

import com.contentintelligence.platform.content.ContentItemService;
import com.contentintelligence.platform.transcript.dto.TranscriptResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class ContentTranscriptService {

    private final ContentTranscriptRepository transcriptRepository;
    private final ContentItemService contentItemService;
    private final YoutubeTranscriptService youtubeTranscriptService;

    public ContentTranscriptService(
            ContentTranscriptRepository transcriptRepository,
            ContentItemService contentItemService,
            YoutubeTranscriptService youtubeTranscriptService
    ) {
        this.transcriptRepository = transcriptRepository;
        this.contentItemService = contentItemService;
        this.youtubeTranscriptService = youtubeTranscriptService;
    }

    public Optional<TranscriptResponse> getTranscript(String videoId) {
        return transcriptRepository.findById(videoId).map(TranscriptResponse::from);
    }

    public TranscriptResponse fetchAndSaveTranscript(String accountId, String videoId) {
        contentItemService.saveFromYoutube(accountId, videoId);

        try {
            YoutubeTranscriptService.FetchedTranscript fetchedTranscript =
                    youtubeTranscriptService.fetchTranscript(videoId);
            ContentTranscript transcript = transcriptRepository.findById(videoId)
                    .orElseGet(() -> new ContentTranscript(
                            videoId,
                            null,
                            null,
                            TranscriptStatus.FAILED,
                            null
                    ));
            transcript.replaceWith(
                    fetchedTranscript.text(),
                    fetchedTranscript.languageCode(),
                    TranscriptStatus.READY,
                    null
            );

            return TranscriptResponse.from(transcriptRepository.save(transcript));
        } catch (ResponseStatusException exception) {
            ContentTranscript transcript = transcriptRepository.findById(videoId)
                    .orElseGet(() -> new ContentTranscript(
                            videoId,
                            null,
                            null,
                            TranscriptStatus.FAILED,
                            null
                    ));
            transcript.replaceWith(
                    null,
                    null,
                    exception.getStatusCode().value() == 404
                            ? TranscriptStatus.UNAVAILABLE
                            : TranscriptStatus.FAILED,
                    exception.getReason()
            );

            return TranscriptResponse.from(transcriptRepository.save(transcript));
        }
    }
}
