package com.contentintelligence.platform.learning;

import com.contentintelligence.platform.account.UserAccountService;
import com.contentintelligence.platform.ai.OpenAiClient;
import com.contentintelligence.platform.content.AccountSavedContent;
import com.contentintelligence.platform.content.AccountSavedContentRepository;
import com.contentintelligence.platform.content.ContentItem;
import com.contentintelligence.platform.content.ContentItemRepository;
import com.contentintelligence.platform.learning.dto.CompareVideosResponse;
import com.contentintelligence.platform.learning.dto.LearningTimelineEventResponse;
import com.contentintelligence.platform.learning.dto.LearningTimelineResponse;
import com.contentintelligence.platform.learning.dto.ProjectPhaseResponse;
import com.contentintelligence.platform.learning.dto.ProjectPlanResponse;
import com.contentintelligence.platform.learning.dto.VideoBriefResponse;
import com.contentintelligence.platform.quiz.ContentQuiz;
import com.contentintelligence.platform.quiz.ContentQuizRepository;
import com.contentintelligence.platform.rag.LibraryChatMessage;
import com.contentintelligence.platform.rag.LibraryChatMessageRepository;
import com.contentintelligence.platform.rag.RagChatMessage;
import com.contentintelligence.platform.rag.RagChatMessageRepository;
import com.contentintelligence.platform.rag.RagChatRole;
import com.contentintelligence.platform.summary.ContentSummary;
import com.contentintelligence.platform.summary.ContentSummaryRepository;
import com.contentintelligence.platform.summary.SummaryStatus;
import com.contentintelligence.platform.transcript.ContentTranscript;
import com.contentintelligence.platform.transcript.ContentTranscriptRepository;
import com.contentintelligence.platform.transcript.TranscriptStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LearningLabService {

    private static final int MAX_EXCERPT_LENGTH = 4500;

    private final UserAccountService accountService;
    private final AccountSavedContentRepository savedContentRepository;
    private final ContentItemRepository contentItemRepository;
    private final ContentTranscriptRepository transcriptRepository;
    private final ContentSummaryRepository summaryRepository;
    private final ContentQuizRepository quizRepository;
    private final RagChatMessageRepository ragChatMessageRepository;
    private final LibraryChatMessageRepository libraryChatMessageRepository;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LearningLabService(
            UserAccountService accountService,
            AccountSavedContentRepository savedContentRepository,
            ContentItemRepository contentItemRepository,
            ContentTranscriptRepository transcriptRepository,
            ContentSummaryRepository summaryRepository,
            ContentQuizRepository quizRepository,
            RagChatMessageRepository ragChatMessageRepository,
            LibraryChatMessageRepository libraryChatMessageRepository,
            OpenAiClient openAiClient
    ) {
        this.accountService = accountService;
        this.savedContentRepository = savedContentRepository;
        this.contentItemRepository = contentItemRepository;
        this.transcriptRepository = transcriptRepository;
        this.summaryRepository = summaryRepository;
        this.quizRepository = quizRepository;
        this.ragChatMessageRepository = ragChatMessageRepository;
        this.libraryChatMessageRepository = libraryChatMessageRepository;
        this.openAiClient = openAiClient;
    }

    public CompareVideosResponse compareVideos(String accountId, List<String> videoIds) {
        accountService.requireAccount(accountId);
        if (videoIds == null || videoIds.size() != 2 || videoIds.get(0).equals(videoIds.get(1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose two different saved videos.");
        }

        requireSavedVideos(accountId, videoIds);
        Map<String, ContentItem> contentByVideoId = contentItemRepository.findAllById(videoIds)
                .stream()
                .collect(Collectors.toMap(ContentItem::getVideoId, Function.identity()));
        Map<String, ContentTranscript> transcriptByVideoId = transcriptRepository.findAllById(videoIds)
                .stream()
                .collect(Collectors.toMap(ContentTranscript::getVideoId, Function.identity()));

        for (String videoId : videoIds) {
            ContentTranscript transcript = transcriptByVideoId.get(videoId);
            if (!isReadyTranscript(transcript)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Fetch transcripts for both videos before comparing them."
                );
            }
        }

        String json = openAiClient.generateJson(
                compareSystemPrompt(),
                compareUserPrompt(videoIds, contentByVideoId, transcriptByVideoId)
        );
        JsonNode root = readJson(json, "OpenAI video comparison JSON could not be parsed.");

        return new CompareVideosResponse(
                "openai",
                openAiClient.getChatModel(),
                Instant.now(),
                videoBrief(contentByVideoId.get(videoIds.get(0))),
                videoBrief(contentByVideoId.get(videoIds.get(1))),
                stringList(root.path("commonGround")),
                stringList(root.path("firstVideoStrengths")),
                stringList(root.path("secondVideoStrengths")),
                stringList(root.path("disagreementsOrGaps")),
                root.path("bestForBeginners").asText("Not enough evidence."),
                root.path("bestForDepth").asText("Not enough evidence."),
                root.path("learningRecommendation").asText("Watch the clearer overview first, then use the deeper video for reinforcement.")
        );
    }

    public ProjectPlanResponse generateProjectPlan(String accountId, String goal) {
        accountService.requireAccount(accountId);
        String json = openAiClient.generateJson(
                projectSystemPrompt(),
                projectUserPrompt(goal)
        );
        JsonNode root = readJson(json, "OpenAI project plan JSON could not be parsed.");
        List<ProjectPhaseResponse> phases = new ArrayList<>();

        for (JsonNode phase : root.path("phases")) {
            phases.add(new ProjectPhaseResponse(
                    phase.path("title").asText("Project phase"),
                    phase.path("outcome").asText("A working project increment."),
                    stringList(phase.path("tasks"))
            ));
        }

        return new ProjectPlanResponse(
                "openai",
                openAiClient.getChatModel(),
                Instant.now(),
                root.path("title").asText("Saved Video Project"),
                root.path("objective").asText("Build a practical project using ideas from your saved videos."),
                stringList(root.path("stack")),
                phases,
                stringList(root.path("stretchGoals"))
        );
    }

    public LearningTimelineResponse getTimeline(String accountId) {
        accountService.requireAccount(accountId);
        List<AccountSavedContent> savedItems =
                savedContentRepository.findAllByAccountIdOrderBySavedAtDesc(accountId);
        List<String> videoIds = savedItems.stream()
                .map(AccountSavedContent::getVideoId)
                .distinct()
                .toList();
        Map<String, ContentItem> contentByVideoId = contentItemRepository.findAllById(videoIds)
                .stream()
                .collect(Collectors.toMap(ContentItem::getVideoId, Function.identity()));
        List<LearningTimelineEventResponse> events = new ArrayList<>();

        for (AccountSavedContent saved : savedItems) {
            ContentItem item = contentByVideoId.get(saved.getVideoId());
            events.add(new LearningTimelineEventResponse(
                    "Saved",
                    item == null ? "Saved video" : item.getTitle(),
                    "Added this video to the account library.",
                    saved.getVideoId(),
                    saved.getSavedAt()
            ));
        }

        for (ContentTranscript transcript : transcriptRepository.findAllById(videoIds)) {
            if (transcript.getStatus() == TranscriptStatus.READY) {
                events.add(new LearningTimelineEventResponse(
                        "Transcript",
                        titleFor(contentByVideoId, transcript.getVideoId()),
                        "Fetched a transcript and made this video usable for AI features.",
                        transcript.getVideoId(),
                        transcript.getFetchedAt()
                ));
            }
        }

        for (ContentSummary summary : summaryRepository.findAllById(videoIds)) {
            if (summary.getStatus() == SummaryStatus.READY) {
                events.add(new LearningTimelineEventResponse(
                        "Summary",
                        titleFor(contentByVideoId, summary.getVideoId()),
                        "Generated an OpenAI summary.",
                        summary.getVideoId(),
                        summary.getGeneratedAt()
                ));
            }
        }

        for (ContentQuiz quiz : quizRepository.findAllById(videoIds)) {
            events.add(new LearningTimelineEventResponse(
                    "Quiz",
                    titleFor(contentByVideoId, quiz.getVideoId()),
                    "Generated a local AI quiz.",
                    quiz.getVideoId(),
                    quiz.getGeneratedAt()
            ));
        }

        for (RagChatMessage message : ragChatMessageRepository.findByAccountIdOrderByCreatedAtDesc(accountId)) {
            if (message.getRole() == RagChatRole.USER) {
                events.add(new LearningTimelineEventResponse(
                        "Video Chat",
                        titleFor(contentByVideoId, message.getVideoId()),
                        "Asked: " + preview(message.getContent(), 120),
                        message.getVideoId(),
                        message.getCreatedAt()
                ));
            }
        }

        for (LibraryChatMessage message : libraryChatMessageRepository.findByAccountIdOrderByCreatedAtAsc(accountId)) {
            if (message.getRole() == RagChatRole.USER) {
                events.add(new LearningTimelineEventResponse(
                        "Library Chat",
                        "Asked across saved videos",
                        preview(message.getContent(), 140),
                        null,
                        message.getCreatedAt()
                ));
            }
        }

        return new LearningTimelineResponse(
                accountId,
                events.stream()
                        .sorted(Comparator.comparing(LearningTimelineEventResponse::occurredAt).reversed())
                        .limit(40)
                        .toList()
        );
    }

    private void requireSavedVideos(String accountId, List<String> videoIds) {
        Set<String> saved = new LinkedHashSet<>(savedVideoIds(accountId));
        for (String videoId : videoIds) {
            if (!saved.contains(videoId)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Both videos must be saved to this account before comparing them."
                );
            }
        }
    }

    private List<String> savedVideoIds(String accountId) {
        return savedContentRepository.findAllByAccountIdOrderBySavedAtDesc(accountId)
                .stream()
                .map(AccountSavedContent::getVideoId)
                .distinct()
                .toList();
    }

    private boolean isReadyTranscript(ContentTranscript transcript) {
        return transcript != null
                && transcript.getStatus() == TranscriptStatus.READY
                && transcript.getTranscriptText() != null
                && !transcript.getTranscriptText().isBlank();
    }

    private String compareUserPrompt(
            List<String> videoIds,
            Map<String, ContentItem> contentByVideoId,
            Map<String, ContentTranscript> transcriptByVideoId
    ) {
        ContentItem first = contentByVideoId.get(videoIds.get(0));
        ContentItem second = contentByVideoId.get(videoIds.get(1));

        return """
                Compare these two saved YouTube videos for a learner.

                First decide whether these videos are meaningfully comparable.
                They are meaningfully comparable only if they share a topic,
                domain, problem, skill, technology, or learning path.
                If they are very dissimilar, do not force a comparison.
                For very dissimilar videos, return the same JSON shape with:
                - commonGround as an empty array
                - firstVideoStrengths as an empty array
                - secondVideoStrengths as an empty array
                - disagreementsOrGaps containing one clear sentence saying the videos are very dissimilar
                - bestForBeginners saying they are not comparable because they teach unrelated subjects
                - bestForDepth saying they are not comparable because they teach unrelated subjects
                - learningRecommendation saying to treat them as separate learning paths instead of choosing a watch order

                Return exactly this JSON shape:
                {
                  "commonGround": ["shared teaching point"],
                  "firstVideoStrengths": ["what the first video teaches better"],
                  "secondVideoStrengths": ["what the second video teaches better"],
                  "disagreementsOrGaps": ["differences, missing pieces, or framing gaps"],
                  "bestForBeginners": "which video is better for beginners and why",
                  "bestForDepth": "which video is better for depth and why",
                  "learningRecommendation": "recommended watch order and how to use both"
                }

                First video:
                Title: %s
                Transcript excerpt:
                %s

                Second video:
                Title: %s
                Transcript excerpt:
                %s
                """.formatted(
                first == null ? videoIds.get(0) : first.getTitle(),
                excerpt(transcriptByVideoId.get(videoIds.get(0)).getTranscriptText()),
                second == null ? videoIds.get(1) : second.getTitle(),
                excerpt(transcriptByVideoId.get(videoIds.get(1)).getTranscriptText())
        );
    }

    private String projectUserPrompt(String goal) {
        return """
                Create a practical, portfolio-grade software project plan for the user.

                Return exactly this JSON shape:
                {
                  "title": "Project title",
                  "objective": "What the learner will build and prove",
                  "stack": ["technology or concept"],
                  "phases": [
                    {
                      "title": "Phase name",
                      "outcome": "What should work after this phase",
                      "tasks": ["clear implementation task"]
                    }
                  ],
                  "stretchGoals": ["optional advanced improvement"]
                }

                Rules:
                - Return JSON only.
                - Make 4 to 6 phases.
                - Keep tasks concrete and buildable.
                - Use modern, sensible engineering choices.
                - Prefer a project with clear user value, a realistic scope, and demonstrable technical depth.
                - If the user gives a goal, tailor the plan to it.
                - If the user does not give a goal, choose a strong project idea yourself.
                - Include implementation tasks, not vague learning advice.

                User goal:
                %s
                """.formatted(
                goal == null || goal.isBlank()
                        ? "Choose the best practical portfolio project for a motivated full-stack learner."
                        : goal.trim()
        );
    }

    private String compareSystemPrompt() {
        return """
                You compare learning videos using transcript evidence.
                Return strict JSON only. Do not use markdown or commentary.
                Be practical: first detect whether the videos are meaningfully related.
                If they are very dissimilar, say so clearly instead of inventing overlap.
                If they are comparable, identify overlap, differences, gaps, and watch order.
                """;
    }

    private String projectSystemPrompt() {
        return """
                You design excellent portfolio software project plans.
                Return strict JSON only. Do not use markdown or commentary.
                Prefer concrete tasks, production-minded architecture, and realistic milestones over vague advice.
                """;
    }

    private JsonNode readJson(String text, String failureMessage) {
        try {
            return objectMapper.readTree(extractJsonPayload(text));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, failureMessage, exception);
        }
    }

    private String extractJsonPayload(String text) {
        int objectStart = text.indexOf('{');
        int objectEnd = text.lastIndexOf('}');

        if (objectStart >= 0 && objectEnd > objectStart) {
            return text.substring(objectStart, objectEnd + 1);
        }

        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI did not return JSON.");
    }

    private List<String> stringList(JsonNode node) {
        List<String> values = new ArrayList<>();

        if (!node.isArray()) {
            return values;
        }

        for (JsonNode item : node) {
            String value = item.asText("");
            if (!value.isBlank()) {
                values.add(value.trim());
            }
        }

        return values;
    }

    private VideoBriefResponse videoBrief(ContentItem item) {
        if (item == null) {
            return new VideoBriefResponse("unknown", "Saved video", "Unknown channel");
        }

        return new VideoBriefResponse(item.getVideoId(), item.getTitle(), item.getChannelTitle());
    }

    private String titleFor(Map<String, ContentItem> contentByVideoId, String videoId) {
        ContentItem item = contentByVideoId.get(videoId);
        return item == null ? "Saved video" : item.getTitle();
    }

    private String excerpt(String transcriptText) {
        String compactTranscript = transcriptText.replaceAll("\\s+", " ").trim();

        if (compactTranscript.length() <= MAX_EXCERPT_LENGTH) {
            return compactTranscript;
        }

        int midpoint = compactTranscript.length() / 2;
        int firstPart = MAX_EXCERPT_LENGTH / 2;
        int secondStart = Math.max(0, midpoint - (firstPart / 2));

        return compactTranscript.substring(0, firstPart)
                + "\n[Middle excerpt]\n"
                + compactTranscript.substring(
                secondStart,
                Math.min(compactTranscript.length(), secondStart + firstPart)
        );
    }

    private String preview(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength).trim() + "...";
    }
}
