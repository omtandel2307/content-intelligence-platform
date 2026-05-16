package com.contentintelligence.platform.summary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiSummaryClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.model}")
    private String model;

    public String getModel() {
        return model;
    }

    public String summarize(String title, String transcriptText) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OPENAI_API_KEY is not configured."
            );
        }

        try {
            String responseBody = postResponsesApi(title, limitTranscript(transcriptText));
            String outputText = extractOutputText(responseBody);

            if (outputText.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "OpenAI returned an empty summary."
                );
            }

            return outputText;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI summary request failed: " + exception.getMessage(),
                    exception
            );
        }
    }

    private String postResponsesApi(String title, String transcriptText)
            throws IOException, InterruptedException {
        Map<String, Object> payload = Map.of(
                "model", model,
                "input", List.of(
                        Map.of(
                                "role", "system",
                                "content", "You create crisp, faithful study notes from video transcripts. Do not invent facts."
                        ),
                        Map.of(
                                "role", "user",
                                "content", """
                                        Summarize this video transcript.

                                        Return:
                                        1. A short title
                                        2. A 4-6 bullet summary
                                        3. Key takeaways
                                        4. Useful quotes or moments if visible from the transcript

                                        Video title: %s

                                        Transcript:
                                        %s
                                        """.formatted(title, transcriptText)
                        )
                )
        );
        String requestBody = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/responses"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI returned status " + response.statusCode() + ": " + response.body()
            );
        }

        return response.body();
    }

    private String extractOutputText(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        String outputText = root.path("output_text").asText("");

        if (!outputText.isBlank()) {
            return outputText;
        }

        StringBuilder fallbackText = new StringBuilder();

        for (JsonNode outputItem : root.path("output")) {
            for (JsonNode contentItem : outputItem.path("content")) {
                String text = contentItem.path("text").asText("");

                if (!text.isBlank()) {
                    fallbackText.append(text).append("\n");
                }
            }
        }

        return fallbackText.toString().trim();
    }

    private String limitTranscript(String transcriptText) {
        int maxLength = 60000;

        if (transcriptText.length() <= maxLength) {
            return transcriptText;
        }

        return transcriptText.substring(0, maxLength)
                + "\n\n[Transcript truncated for this first summarization pass.]";
    }
}
