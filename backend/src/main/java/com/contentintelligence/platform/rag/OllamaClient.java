package com.contentintelligence.platform.rag;

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
public class OllamaClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${local-ai.ollama.base-url}")
    private String baseUrl;

    @Value("${local-ai.ollama.chat-model}")
    private String chatModel;

    @Value("${local-ai.ollama.embedding-model}")
    private String embeddingModel;

    public String getChatModel() {
        return chatModel;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public List<Double> embed(String text) {
        Map<String, Object> payload = Map.of(
                "model", embeddingModel,
                "input", text
        );

        try {
            String responseBody = post("/api/embed", payload);
            JsonNode embeddings = objectMapper.readTree(responseBody).path("embeddings");

            if (!embeddings.isArray() || embeddings.isEmpty() || !embeddings.get(0).isArray()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Ollama embedding response did not include vectors."
                );
            }

            List<Double> vector = new ArrayList<>();
            for (JsonNode value : embeddings.get(0)) {
                vector.add(value.asDouble());
            }

            if (vector.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Ollama returned an empty embedding vector."
                );
            }

            return vector;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw ollamaUnavailable("Ollama embedding request failed: " + exception.getMessage(), exception);
        }
    }

    public String chat(String systemPrompt, String userPrompt) {
        Map<String, Object> payload = Map.of(
                "model", chatModel,
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            String responseBody = post("/api/chat", payload);
            String content = objectMapper.readTree(responseBody)
                    .path("message")
                    .path("content")
                    .asText("");

            if (content.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Ollama returned an empty chat response."
                );
            }

            return content.trim();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw ollamaUnavailable("Ollama chat request failed: " + exception.getMessage(), exception);
        }
    }

    public String generateJson(String systemPrompt, String userPrompt) {
        Map<String, Object> payload = Map.of(
                "model", chatModel,
                "stream", false,
                "format", "json",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            String responseBody = post("/api/chat", payload);
            String content = objectMapper.readTree(responseBody)
                    .path("message")
                    .path("content")
                    .asText("");

            if (content.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Ollama returned an empty JSON response."
                );
            }

            return content.trim();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw ollamaUnavailable("Ollama JSON generation failed: " + exception.getMessage(), exception);
        }
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

    private String post(String path, Map<String, Object> payload)
            throws IOException, InterruptedException {
        String requestBody = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder(URI.create(normalizedBaseUrl() + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Ollama returned status " + response.statusCode() + ": " + response.body()
            );
        }

        return response.body();
    }

    private String normalizedBaseUrl() {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl;
    }

    private ResponseStatusException ollamaUnavailable(String message, Exception exception) {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                message + ". Make sure Ollama is running and the configured models are pulled.",
                exception
        );
    }
}
