package com.contentintelligence.platform.summary;

import com.contentintelligence.platform.content.ContentItem;
import com.contentintelligence.platform.content.ContentItemRepository;
import com.contentintelligence.platform.summary.dto.SummaryResponse;
import com.contentintelligence.platform.transcript.ContentTranscript;
import com.contentintelligence.platform.transcript.ContentTranscriptRepository;
import com.contentintelligence.platform.transcript.TranscriptStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class ContentSummaryService {

    private final ContentSummaryRepository summaryRepository;
    private final ContentItemRepository contentItemRepository;
    private final ContentTranscriptRepository transcriptRepository;
    private final OpenAiSummaryClient openAiSummaryClient;

    public ContentSummaryService(
            ContentSummaryRepository summaryRepository,
            ContentItemRepository contentItemRepository,
            ContentTranscriptRepository transcriptRepository,
            OpenAiSummaryClient openAiSummaryClient
    ) {
        this.summaryRepository = summaryRepository;
        this.contentItemRepository = contentItemRepository;
        this.transcriptRepository = transcriptRepository;
        this.openAiSummaryClient = openAiSummaryClient;
    }

    public Optional<SummaryResponse> getSummary(String videoId) {
        return summaryRepository.findById(videoId).map(SummaryResponse::from);
    }

    public SummaryResponse generateSummary(String videoId) {
        ContentItem contentItem = contentItemRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Save this video before generating a summary."
                ));
        ContentTranscript transcript = transcriptRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Fetch the transcript before generating a summary."
                ));

        if (transcript.getStatus() != TranscriptStatus.READY
                || transcript.getTranscriptText() == null
                || transcript.getTranscriptText().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A ready transcript is required before generating a summary."
            );
        }

        ContentSummary summary = summaryRepository.findById(videoId)
                .orElseGet(() -> new ContentSummary(
                        videoId,
                        null,
                        openAiSummaryClient.getModel(),
                        SummaryStatus.FAILED,
                        null
                ));

        try {
            String summaryText = openAiSummaryClient.summarize(
                    contentItem.getTitle(),
                    transcript.getTranscriptText()
            );
            summary.replaceWith(
                    summaryText,
                    openAiSummaryClient.getModel(),
                    SummaryStatus.READY,
                    null
            );
        } catch (ResponseStatusException exception) {
            summary.replaceWith(
                    null,
                    openAiSummaryClient.getModel(),
                    SummaryStatus.FAILED,
                    exception.getReason()
            );
        }

        return SummaryResponse.from(summaryRepository.save(summary));
    }
}
