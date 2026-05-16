package com.contentintelligence.platform.rag;

import com.contentintelligence.platform.account.UserAccountService;
import com.contentintelligence.platform.content.AccountSavedContent;
import com.contentintelligence.platform.content.AccountSavedContentRepository;
import com.contentintelligence.platform.content.ContentItem;
import com.contentintelligence.platform.content.ContentItemRepository;
import com.contentintelligence.platform.rag.dto.LibraryChatMessageResponse;
import com.contentintelligence.platform.rag.dto.LibraryChatResponse;
import com.contentintelligence.platform.rag.dto.LibraryRagSourceResponse;
import com.contentintelligence.platform.rag.dto.LocalChatResponse;
import com.contentintelligence.platform.rag.dto.RagChatMessageResponse;
import com.contentintelligence.platform.rag.dto.RagIndexResponse;
import com.contentintelligence.platform.rag.dto.RagSourceResponse;
import com.contentintelligence.platform.transcript.ContentTranscript;
import com.contentintelligence.platform.transcript.ContentTranscriptRepository;
import com.contentintelligence.platform.transcript.TranscriptStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LocalRagService {

    private final TranscriptChunkRepository chunkRepository;
    private final ContentTranscriptRepository transcriptRepository;
    private final ContentItemRepository contentItemRepository;
    private final AccountSavedContentRepository savedContentRepository;
    private final RagChatMessageRepository chatMessageRepository;
    private final LibraryChatMessageRepository libraryChatMessageRepository;
    private final UserAccountService accountService;
    private final OllamaClient ollamaClient;

    @Value("${local-ai.rag.max-chunks}")
    private int maxChunks;

    @Value("${local-ai.rag.chunk-word-count}")
    private int chunkWordCount;

    @Value("${local-ai.rag.chunk-overlap-word-count}")
    private int chunkOverlapWordCount;

    @Value("${local-ai.rag.top-k}")
    private int topK;

    public LocalRagService(
            TranscriptChunkRepository chunkRepository,
            ContentTranscriptRepository transcriptRepository,
            ContentItemRepository contentItemRepository,
            AccountSavedContentRepository savedContentRepository,
            RagChatMessageRepository chatMessageRepository,
            LibraryChatMessageRepository libraryChatMessageRepository,
            UserAccountService accountService,
            OllamaClient ollamaClient
    ) {
        this.chunkRepository = chunkRepository;
        this.transcriptRepository = transcriptRepository;
        this.contentItemRepository = contentItemRepository;
        this.savedContentRepository = savedContentRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.libraryChatMessageRepository = libraryChatMessageRepository;
        this.accountService = accountService;
        this.ollamaClient = ollamaClient;
    }

    public RagIndexResponse getIndexStatus(String videoId) {
        int chunkCount = (int) chunkRepository.countByVideoIdAndEmbeddingModel(
                videoId,
                ollamaClient.getEmbeddingModel()
        );

        return new RagIndexResponse(
                videoId,
                chunkCount,
                ollamaClient.getEmbeddingModel(),
                Instant.now()
        );
    }

    public List<RagChatMessageResponse> getChatHistory(String accountId, String videoId) {
        accountService.requireAccount(accountId);
        return chatMessageRepository.findByAccountIdAndVideoIdOrderByCreatedAtAsc(accountId, videoId)
                .stream()
                .map(RagChatMessageResponse::from)
                .toList();
    }

    public List<LibraryChatMessageResponse> getLibraryChatHistory(String accountId) {
        accountService.requireAccount(accountId);
        return libraryChatMessageRepository.findByAccountIdOrderByCreatedAtAsc(accountId)
                .stream()
                .map(LibraryChatMessageResponse::from)
                .toList();
    }

    @Transactional
    public RagIndexResponse indexTranscript(String videoId) {
        ContentTranscript transcript = transcriptRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Fetch the transcript before building local RAG."
                ));

        if (transcript.getStatus() != TranscriptStatus.READY
                || transcript.getTranscriptText() == null
                || transcript.getTranscriptText().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A ready transcript is required before building local RAG."
            );
        }

        List<String> chunks = splitIntoChunks(transcript.getTranscriptText());
        chunkRepository.deleteByVideoId(videoId);

        List<TranscriptChunk> transcriptChunks = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            List<Double> embedding = ollamaClient.embed(chunks.get(index));
            transcriptChunks.add(new TranscriptChunk(
                    videoId,
                    index,
                    chunks.get(index),
                    ollamaClient.serializeEmbedding(embedding),
                    ollamaClient.getEmbeddingModel()
            ));
        }

        chunkRepository.saveAll(transcriptChunks);

        return new RagIndexResponse(
                videoId,
                transcriptChunks.size(),
                ollamaClient.getEmbeddingModel(),
                Instant.now()
        );
    }

    @Transactional
    public LocalChatResponse chatWithVideo(String accountId, String videoId, String message) {
        accountService.requireAccount(accountId);
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is required.");
        }

        ensureIndexed(videoId);
        String trimmedMessage = message.trim();
        chatMessageRepository.save(new RagChatMessage(
                accountId,
                videoId,
                RagChatRole.USER,
                trimmedMessage,
                "human",
                "user"
        ));

        List<TranscriptChunk> chunks =
                chunkRepository.findByVideoIdAndEmbeddingModelOrderByChunkIndexAsc(
                        videoId,
                        ollamaClient.getEmbeddingModel()
                );

        if (chunks.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No local RAG chunks are available for this video yet."
            );
        }

        List<Double> questionEmbedding = ollamaClient.embed(trimmedMessage);
        List<ScoredChunk> scoredChunks = chunks.stream()
                .map(chunk -> new ScoredChunk(
                        chunk,
                        cosineSimilarity(
                                questionEmbedding,
                                ollamaClient.parseEmbedding(chunk.getEmbeddingJson())
                        )
                ))
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(Math.max(1, topK))
                .toList();

        String answer = ollamaClient.chat(
                systemPrompt(),
                userPrompt(videoId, trimmedMessage, scoredChunks)
        );
        chatMessageRepository.save(new RagChatMessage(
                accountId,
                videoId,
                RagChatRole.ASSISTANT,
                answer,
                "ollama",
                ollamaClient.getChatModel()
        ));
        List<RagSourceResponse> sources = scoredChunks.stream()
                .map(scoredChunk -> new RagSourceResponse(
                        scoredChunk.chunk().getChunkIndex(),
                        roundScore(scoredChunk.score()),
                        preview(scoredChunk.chunk().getChunkText())
                ))
                .toList();

        return new LocalChatResponse(
                videoId,
                answer,
                "ollama",
                ollamaClient.getChatModel(),
                getChatHistory(accountId, videoId),
                sources
        );
    }

    @Transactional
    public LibraryChatResponse chatWithSavedLibrary(String accountId, String message) {
        accountService.requireAccount(accountId);
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is required.");
        }

        List<String> videoIds = savedContentRepository.findAllByAccountIdOrderBySavedAtDesc(accountId)
                .stream()
                .map(AccountSavedContent::getVideoId)
                .distinct()
                .toList();

        if (videoIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Save at least one video before asking your whole library."
            );
        }

        List<ContentTranscript> readyTranscripts = transcriptRepository.findAllById(videoIds)
                .stream()
                .filter(transcript -> transcript.getStatus() == TranscriptStatus.READY)
                .filter(transcript -> transcript.getTranscriptText() != null)
                .filter(transcript -> !transcript.getTranscriptText().isBlank())
                .toList();
        List<String> readyVideoIds = readyTranscripts.stream()
                .map(ContentTranscript::getVideoId)
                .toList();

        if (readyVideoIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Fetch transcripts for saved videos before asking your whole library."
            );
        }

        for (String videoId : readyVideoIds) {
            ensureIndexed(videoId);
        }

        List<TranscriptChunk> chunks =
                chunkRepository.findByVideoIdInAndEmbeddingModelOrderByVideoIdAscChunkIndexAsc(
                        readyVideoIds,
                        ollamaClient.getEmbeddingModel()
                );

        if (chunks.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No local RAG chunks are available for your saved library yet."
            );
        }

        String trimmedMessage = message.trim();
        libraryChatMessageRepository.save(new LibraryChatMessage(
                accountId,
                RagChatRole.USER,
                trimmedMessage,
                "human",
                "user"
        ));

        List<Double> questionEmbedding = ollamaClient.embed(trimmedMessage);
        List<ScoredChunk> scoredChunks = chunks.stream()
                .map(chunk -> new ScoredChunk(
                        chunk,
                        cosineSimilarity(
                                questionEmbedding,
                                ollamaClient.parseEmbedding(chunk.getEmbeddingJson())
                        )
                ))
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(Math.max(1, topK))
                .toList();
        Map<String, ContentItem> contentByVideoId = contentItemRepository.findAllById(readyVideoIds)
                .stream()
                .collect(Collectors.toMap(ContentItem::getVideoId, Function.identity()));

        String answer = ollamaClient.chat(
                librarySystemPrompt(),
                libraryUserPrompt(trimmedMessage, scoredChunks, contentByVideoId)
        );
        libraryChatMessageRepository.save(new LibraryChatMessage(
                accountId,
                RagChatRole.ASSISTANT,
                answer,
                "ollama",
                ollamaClient.getChatModel()
        ));
        List<LibraryRagSourceResponse> sources = scoredChunks.stream()
                .map(scoredChunk -> {
                    TranscriptChunk chunk = scoredChunk.chunk();
                    ContentItem item = contentByVideoId.get(chunk.getVideoId());

                    return new LibraryRagSourceResponse(
                            chunk.getVideoId(),
                            item == null ? "Saved video" : item.getTitle(),
                            item == null ? "Unknown channel" : item.getChannelTitle(),
                            chunk.getChunkIndex(),
                            roundScore(scoredChunk.score()),
                            preview(chunk.getChunkText())
                    );
                })
                .toList();

        return new LibraryChatResponse(
                answer,
                "ollama",
                ollamaClient.getChatModel(),
                getLibraryChatHistory(accountId),
                sources
        );
    }

    private void ensureIndexed(String videoId) {
        long chunkCount = chunkRepository.countByVideoIdAndEmbeddingModel(
                videoId,
                ollamaClient.getEmbeddingModel()
        );

        if (chunkCount == 0) {
            indexTranscript(videoId);
        }
    }

    private String systemPrompt() {
        return """
                You answer questions about a YouTube video using only the provided transcript chunks.
                If the chunks do not contain enough evidence, say what is missing instead of inventing.
                Keep the answer useful, concise, and grounded in the transcript.
                """;
    }

    private String librarySystemPrompt() {
        return """
                You answer questions across a user's saved YouTube library using only the provided transcript chunks.
                Synthesize across videos when multiple sources are relevant.
                Mention video titles naturally when comparing or attributing ideas.
                If the saved transcripts do not contain enough evidence, say what is missing instead of inventing.
                Keep the answer practical, concise, and grounded in the retrieved transcript evidence.
                """;
    }

    private String userPrompt(String videoId, String message, List<ScoredChunk> scoredChunks) {
        Optional<ContentItem> contentItem = contentItemRepository.findById(videoId);
        String title = contentItem.map(ContentItem::getTitle).orElse("Unknown title");
        String channel = contentItem.map(ContentItem::getChannelTitle).orElse("Unknown channel");
        StringBuilder context = new StringBuilder();

        for (int index = 0; index < scoredChunks.size(); index++) {
            ScoredChunk scoredChunk = scoredChunks.get(index);
            context.append("Source ")
                    .append(index + 1)
                    .append(" - transcript chunk ")
                    .append(scoredChunk.chunk().getChunkIndex())
                    .append(", relevance ")
                    .append(roundScore(scoredChunk.score()))
                    .append(":\n")
                    .append(scoredChunk.chunk().getChunkText())
                    .append("\n\n");
        }

        return """
                Video title: %s
                Channel: %s

                User question:
                %s

                Retrieved transcript chunks:
                %s
                """.formatted(title, channel, message.trim(), context);
    }

    private String libraryUserPrompt(
            String message,
            List<ScoredChunk> scoredChunks,
            Map<String, ContentItem> contentByVideoId
    ) {
        StringBuilder context = new StringBuilder();

        for (int index = 0; index < scoredChunks.size(); index++) {
            ScoredChunk scoredChunk = scoredChunks.get(index);
            TranscriptChunk chunk = scoredChunk.chunk();
            ContentItem item = contentByVideoId.get(chunk.getVideoId());
            String title = item == null ? "Saved video" : item.getTitle();
            String channel = item == null ? "Unknown channel" : item.getChannelTitle();

            context.append("Source ")
                    .append(index + 1)
                    .append(" - ")
                    .append(title)
                    .append(" by ")
                    .append(channel)
                    .append(", transcript chunk ")
                    .append(chunk.getChunkIndex())
                    .append(", relevance ")
                    .append(roundScore(scoredChunk.score()))
                    .append(":\n")
                    .append(chunk.getChunkText())
                    .append("\n\n");
        }

        return """
                User question:
                %s

                Retrieved transcript chunks from saved videos:
                %s
                """.formatted(message, context);
    }

    private List<String> splitIntoChunks(String transcriptText) {
        String[] words = transcriptText.trim().split("\\s+");
        int wordsPerChunk = Math.max(80, chunkWordCount);
        int overlap = Math.max(0, Math.min(chunkOverlapWordCount, wordsPerChunk - 1));
        int step = wordsPerChunk - overlap;
        List<String> chunks = new ArrayList<>();

        for (int start = 0; start < words.length && chunks.size() < maxChunks; start += step) {
            int end = Math.min(start + wordsPerChunk, words.length);
            chunks.add(String.join(" ", List.of(words).subList(start, end)));

            if (end == words.length) {
                break;
            }
        }

        return chunks;
    }

    private double cosineSimilarity(List<Double> first, List<Double> second) {
        int size = Math.min(first.size(), second.size());
        double dot = 0;
        double firstMagnitude = 0;
        double secondMagnitude = 0;

        for (int index = 0; index < size; index++) {
            double firstValue = first.get(index);
            double secondValue = second.get(index);
            dot += firstValue * secondValue;
            firstMagnitude += firstValue * firstValue;
            secondMagnitude += secondValue * secondValue;
        }

        if (firstMagnitude == 0 || secondMagnitude == 0) {
            return 0;
        }

        return dot / (Math.sqrt(firstMagnitude) * Math.sqrt(secondMagnitude));
    }

    private String preview(String text) {
        int maxLength = 420;

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength).trim() + "...";
    }

    private double roundScore(double score) {
        return Math.round(score * 1000.0) / 1000.0;
    }

    private record ScoredChunk(TranscriptChunk chunk, double score) {
    }
}
