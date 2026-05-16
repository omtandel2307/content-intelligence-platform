package com.contentintelligence.platform.knowledge;

import com.contentintelligence.platform.account.UserAccountService;
import com.contentintelligence.platform.content.AccountSavedContent;
import com.contentintelligence.platform.content.AccountSavedContentRepository;
import com.contentintelligence.platform.content.ContentItem;
import com.contentintelligence.platform.content.ContentItemRepository;
import com.contentintelligence.platform.knowledge.dto.KnowledgeMapLinkResponse;
import com.contentintelligence.platform.knowledge.dto.KnowledgeMapNodeResponse;
import com.contentintelligence.platform.knowledge.dto.KnowledgeMapResponse;
import com.contentintelligence.platform.rag.OllamaClient;
import com.contentintelligence.platform.transcript.ContentTranscript;
import com.contentintelligence.platform.transcript.ContentTranscriptRepository;
import com.contentintelligence.platform.transcript.TranscriptStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class KnowledgeMapService {

    private static final int MAX_CONTEXT_LENGTH = 24000;
    private static final int MAX_EXCERPT_LENGTH = 3500;
    private static final int MAX_NODES = 10;
    private static final int MAX_LINKS = 14;

    private final UserAccountService accountService;
    private final AccountSavedContentRepository savedContentRepository;
    private final ContentItemRepository contentItemRepository;
    private final ContentTranscriptRepository transcriptRepository;
    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KnowledgeMapService(
            UserAccountService accountService,
            AccountSavedContentRepository savedContentRepository,
            ContentItemRepository contentItemRepository,
            ContentTranscriptRepository transcriptRepository,
            OllamaClient ollamaClient
    ) {
        this.accountService = accountService;
        this.savedContentRepository = savedContentRepository;
        this.contentItemRepository = contentItemRepository;
        this.transcriptRepository = transcriptRepository;
        this.ollamaClient = ollamaClient;
    }

    public KnowledgeMapResponse generateKnowledgeMap(String accountId) {
        accountService.requireAccount(accountId);
        List<String> savedVideoIds = savedContentRepository.findAllByAccountIdOrderBySavedAtDesc(accountId)
                .stream()
                .map(AccountSavedContent::getVideoId)
                .distinct()
                .toList();

        if (savedVideoIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Save videos before generating a knowledge map."
            );
        }

        List<ContentTranscript> readyTranscripts = transcriptRepository.findAllById(savedVideoIds)
                .stream()
                .filter(transcript -> transcript.getStatus() == TranscriptStatus.READY)
                .filter(transcript -> transcript.getTranscriptText() != null)
                .filter(transcript -> !transcript.getTranscriptText().isBlank())
                .toList();

        if (readyTranscripts.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Fetch transcripts for saved videos before generating a knowledge map."
            );
        }

        Map<String, ContentItem> contentByVideoId = contentItemRepository.findAllById(savedVideoIds)
                .stream()
                .collect(Collectors.toMap(ContentItem::getVideoId, Function.identity()));
        String mapJson = ollamaClient.generateJson(
                systemPrompt(),
                userPrompt(readyTranscripts, contentByVideoId)
        );

        return normalizeMap(accountId, mapJson, savedVideoIds);
    }

    private KnowledgeMapResponse normalizeMap(
            String accountId,
            String mapJson,
            List<String> allowedVideoIds
    ) {
        try {
            JsonNode root = objectMapper.readTree(extractJsonPayload(mapJson));
            JsonNode rawNodes = root.path("nodes");
            JsonNode rawLinks = root.path("links");

            if (!rawNodes.isArray() || rawNodes.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Ollama knowledge map response did not include topic nodes."
                );
            }

            Set<String> allowedVideoIdSet = new LinkedHashSet<>(allowedVideoIds);
            Map<String, String> idMap = new LinkedHashMap<>();
            Map<String, KnowledgeMapNodeResponse> nodesById = new LinkedHashMap<>();

            for (JsonNode rawNode : rawNodes) {
                if (nodesById.size() >= MAX_NODES) {
                    break;
                }

                String label = firstText(rawNode, "label", "name", "topic");
                if (label.isBlank()) {
                    continue;
                }

                String rawId = firstText(rawNode, "id", "key", "slug");
                String id = uniqueId(slugify(rawId.isBlank() ? label : rawId), nodesById.keySet());
                if (!rawId.isBlank()) {
                    idMap.put(rawId, id);
                }
                idMap.put(label, id);

                List<String> videoIds = new ArrayList<>();
                JsonNode rawVideoIds = rawNode.path("videoIds");
                if (!rawVideoIds.isArray()) {
                    rawVideoIds = rawNode.path("videos");
                }

                if (rawVideoIds.isArray()) {
                    for (JsonNode rawVideoId : rawVideoIds) {
                        String videoId = rawVideoId.asText("");
                        if (allowedVideoIdSet.contains(videoId) && !videoIds.contains(videoId)) {
                            videoIds.add(videoId);
                        }
                    }
                }

                nodesById.put(id, new KnowledgeMapNodeResponse(
                        id,
                        label.trim(),
                        firstText(rawNode, "summary", "description", "whyItMatters"),
                        clamp(firstInt(rawNode, "importance", "weight", "score"), 1, 5),
                        videoIds
                ));
            }

            List<KnowledgeMapLinkResponse> links = new ArrayList<>();
            if (rawLinks.isArray()) {
                for (JsonNode rawLink : rawLinks) {
                    if (links.size() >= MAX_LINKS) {
                        break;
                    }

                    String source = resolveNodeId(rawLink, idMap, "source", "from");
                    String target = resolveNodeId(rawLink, idMap, "target", "to");

                    if (source == null
                            || target == null
                            || source.equals(target)
                            || !nodesById.containsKey(source)
                            || !nodesById.containsKey(target)) {
                        continue;
                    }

                    links.add(new KnowledgeMapLinkResponse(
                            source,
                            target,
                            firstText(rawLink, "relation", "label", "why"),
                            clamp(firstInt(rawLink, "strength", "weight", "score"), 1, 5)
                    ));
                }
            }

            return new KnowledgeMapResponse(
                    accountId,
                    "ollama",
                    ollamaClient.getChatModel(),
                    Instant.now(),
                    List.copyOf(nodesById.values()),
                    links
            );
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Ollama knowledge map JSON could not be parsed. Try generating again.",
                    exception
            );
        }
    }

    private String userPrompt(
            List<ContentTranscript> transcripts,
            Map<String, ContentItem> contentByVideoId
    ) {
        StringBuilder context = new StringBuilder();

        for (ContentTranscript transcript : transcripts) {
            if (context.length() >= MAX_CONTEXT_LENGTH) {
                break;
            }

            ContentItem item = contentByVideoId.get(transcript.getVideoId());
            String title = item == null ? "Saved video" : item.getTitle();
            String channel = item == null ? "Unknown channel" : item.getChannelTitle();
            String excerpt = excerpt(transcript.getTranscriptText());

            context.append("Video ID: ")
                    .append(transcript.getVideoId())
                    .append("\nTitle: ")
                    .append(title)
                    .append("\nChannel: ")
                    .append(channel)
                    .append("\nTranscript excerpt:\n")
                    .append(excerpt)
                    .append("\n\n");
        }

        return """
                Generate a knowledge map from these saved YouTube transcripts.

                Return exactly this JSON shape:
                {
                  "nodes": [
                    {
                      "id": "short-topic-slug",
                      "label": "Topic name",
                      "summary": "One sentence explaining what this topic means in the saved videos.",
                      "importance": 1,
                      "videoIds": ["youtubeVideoId"]
                    }
                  ],
                  "links": [
                    {
                      "source": "source-topic-slug",
                      "target": "target-topic-slug",
                      "relation": "Short phrase explaining the relationship",
                      "strength": 1
                    }
                  ]
                }

                Rules:
                - Return JSON only.
                - Create 5 to 10 nodes.
                - Create only links between node ids you returned.
                - importance and strength must be integers from 1 to 5.
                - videoIds must use the exact Video ID values shown in the context.
                - Prefer durable learning concepts over tiny facts.

                Saved transcript context:
                %s
                """.formatted(context.substring(0, Math.min(context.length(), MAX_CONTEXT_LENGTH)));
    }

    private String systemPrompt() {
        return """
                You extract structured learning maps from video transcripts.
                Return strict JSON only. Do not use markdown or commentary.
                Focus on concepts, dependencies, and learning relationships.
                """;
    }

    private String resolveNodeId(JsonNode node, Map<String, String> idMap, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = node.path(fieldName).asText("");
            if (idMap.containsKey(value)) {
                return idMap.get(value);
            }

            String slug = slugify(value);
            if (idMap.containsValue(slug)) {
                return slug;
            }
        }

        return null;
    }

    private String excerpt(String transcriptText) {
        String compactTranscript = transcriptText.replaceAll("\\s+", " ").trim();

        if (compactTranscript.length() <= MAX_EXCERPT_LENGTH) {
            return compactTranscript;
        }

        int midpoint = compactTranscript.length() / 2;
        int headLength = MAX_EXCERPT_LENGTH / 2;
        int tailLength = MAX_EXCERPT_LENGTH - headLength;
        int middleStart = Math.max(0, midpoint - (headLength / 2));
        int middleEnd = Math.min(compactTranscript.length(), middleStart + tailLength);

        return compactTranscript.substring(0, headLength)
                + "\n[Transcript middle excerpt]\n"
                + compactTranscript.substring(middleStart, middleEnd);
    }

    private String extractJsonPayload(String text) {
        int objectStart = text.indexOf('{');
        int objectEnd = text.lastIndexOf('}');

        if (objectStart >= 0 && objectEnd > objectStart) {
            return text.substring(objectStart, objectEnd + 1);
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Ollama did not return JSON."
        );
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String text = node.path(fieldName).asText("");

            if (!text.isBlank()) {
                return text.trim();
            }
        }

        return "";
    }

    private int firstInt(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.path(fieldName);

            if (field.isInt()) {
                return field.asInt();
            }

            if (field.isTextual()) {
                try {
                    return Integer.parseInt(field.asText());
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return 3;
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private String uniqueId(String candidate, Set<String> existingIds) {
        String fallback = candidate.isBlank() ? "topic" : candidate;
        String unique = fallback;
        int suffix = 2;

        while (existingIds.contains(unique)) {
            unique = fallback + "-" + suffix;
            suffix++;
        }

        return unique;
    }

    private String slugify(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if (normalized.length() > 48) {
            return normalized.substring(0, 48).replaceAll("-$", "");
        }

        return normalized;
    }
}
