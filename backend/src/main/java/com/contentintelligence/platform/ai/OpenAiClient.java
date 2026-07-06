package com.contentintelligence.platform.ai;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.model}")
    private String chatModel;

    @Value("${openai.api.embedding-model}")
    private String embeddingModel;

    public String getChatModel() {
        return chatModel;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public List<Double> embed(String text) {
        List<List<Double>> embeddings = embedAll(List.of(text));

        if (embeddings.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI embedding response did not include a vector."
            );
        }

        return embeddings.getFirst();
    }

    public List<List<Double>> embedAll(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }

        requireApiKey();
        Map<String, Object> payload = Map.of(
                "model", embeddingModel,
                "input", texts
        );

        try {
            String responseBody = post("/embeddings", payload);
            JsonNode data = objectMapper.readTree(responseBody).path("data");

            if (!data.isArray() || data.size() != texts.size()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "OpenAI embedding response did not include the expected vectors."
                );
            }

            List<List<Double>> vectors = new ArrayList<>();
            for (int index = 0; index < texts.size(); index++) {
                vectors.add(null);
            }

            int fallbackIndex = 0;
            for (JsonNode item : data) {
                int vectorIndex = item.path("index").asInt(fallbackIndex);
                JsonNode embedding = item.path("embedding");

                if (vectorIndex < 0 || vectorIndex >= texts.size()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "OpenAI embedding response included an unexpected vector index."
                    );
                }

                vectors.set(vectorIndex, parseEmbeddingVector(embedding));
                fallbackIndex++;
            }

            if (vectors.stream().anyMatch(vector -> vector == null || vector.isEmpty())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "OpenAI embedding response included an empty vector."
                );
            }

            return vectors;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw openAiUnavailable("OpenAI embedding batch request failed: " + exception.getMessage(), exception);
        }
    }

    public String chat(String systemPrompt, String userPrompt) {
        return createResponse(systemPrompt, userPrompt, "OpenAI returned an empty chat response.");
    }

    public String generateJson(String systemPrompt, String userPrompt) {
        return createResponse(systemPrompt, userPrompt, "OpenAI returned an empty JSON response.");
    }

    public String serializeEmbedding(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not serialize embedding.",
                    exception
            );
        }
    }

    public List<Double> parseEmbedding(String embeddingJson) {
        try {
            JsonNode root = objectMapper.readTree(embeddingJson);
            List<Double> vector = new ArrayList<>();

            for (JsonNode value : root) {
                vector.add(value.asDouble());
            }

            return vector;
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not parse stored embedding.",
                    exception
            );
        }
    }

    private String createResponse(String systemPrompt, String userPrompt, String emptyResponseMessage) {
        requireApiKey();
        Map<String, Object> payload = Map.of(
                "model", chatModel,
                "input", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            String responseBody = post("/responses", payload);
            String outputText = extractOutputText(responseBody);

            if (outputText.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, emptyResponseMessage);
            }

            return outputText.trim();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw openAiUnavailable("OpenAI response request failed: " + exception.getMessage(), exception);
        }
    }

    private String post(String path, Map<String, Object> payload)
            throws IOException, InterruptedException {
        String requestBody = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1" + path))
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

    private List<Double> parseEmbeddingVector(JsonNode embedding) {
        if (!embedding.isArray() || embedding.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI embedding response did not include a vector."
            );
        }

        List<Double> vector = new ArrayList<>();
        for (JsonNode value : embedding) {
            vector.add(value.asDouble());
        }

        return vector;
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OPENAI_API_KEY is not configured."
            );
        }
    }

    private ResponseStatusException openAiUnavailable(String message, Exception exception) {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                message + ". Make sure OPENAI_API_KEY is configured and has access to the selected models.",
                exception
        );
    }
}
