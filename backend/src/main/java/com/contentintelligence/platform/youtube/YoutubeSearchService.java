package com.contentintelligence.platform.youtube;

import com.contentintelligence.platform.youtube.dto.YoutubeSearchItem;
import com.contentintelligence.platform.youtube.dto.YoutubeSearchResponse;
import com.contentintelligence.platform.youtube.dto.YoutubeVideoDetails;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class YoutubeSearchService {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${youtube.api.key}")
    private String youtubeApiKey;

    public YoutubeSearchResponse search(String query) {
        ensureApiKeyConfigured();

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        URI uri = URI.create(
                "https://www.googleapis.com/youtube/v3/search"
                        + "?part=snippet"
                        + "&type=video"
                        + "&maxResults=12"
                        + "&q=" + encodedQuery
                        + "&key=" + youtubeApiKey
        );

        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "YouTube API request failed with status " + response.statusCode()
                );
            }

            return mapResponse(query, response.body());
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to call YouTube API.",
                    exception
            );
        }
    }

    public YoutubeVideoDetails getVideoDetails(String videoId) {
        ensureApiKeyConfigured();

        String encodedVideoId = URLEncoder.encode(videoId, StandardCharsets.UTF_8);
        URI uri = URI.create(
                "https://www.googleapis.com/youtube/v3/videos"
                        + "?part=snippet,contentDetails,statistics"
                        + "&id=" + encodedVideoId
                        + "&key=" + youtubeApiKey
        );

        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "YouTube API request failed with status " + response.statusCode()
                );
            }

            return mapVideoDetails(videoId, response.body());
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to call YouTube API.",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to call YouTube API.",
                    exception
            );
        }
    }

    private void ensureApiKeyConfigured() {
        if (youtubeApiKey == null || youtubeApiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "YOUTUBE_API_KEY is not configured."
            );
        }
    }

    private YoutubeSearchResponse mapResponse(String query, String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        List<YoutubeSearchItem> items = new ArrayList<>();

        for (JsonNode itemNode : root.path("items")) {
            JsonNode idNode = itemNode.path("id");
            JsonNode snippetNode = itemNode.path("snippet");

            items.add(new YoutubeSearchItem(
                    idNode.path("videoId").asText(""),
                    snippetNode.path("title").asText(""),
                    snippetNode.path("description").asText(""),
                    snippetNode.path("channelTitle").asText(""),
                    snippetNode.path("thumbnails").path("high").path("url").asText(""),
                    snippetNode.path("publishedAt").asText("")
            ));
        }

        return new YoutubeSearchResponse(query, items);
    }

    private YoutubeVideoDetails mapVideoDetails(String videoId, String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode firstItem = root.path("items").path(0);

        if (firstItem.isMissingNode()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found.");
        }

        JsonNode snippetNode = firstItem.path("snippet");
        JsonNode contentDetailsNode = firstItem.path("contentDetails");
        JsonNode statisticsNode = firstItem.path("statistics");

        return new YoutubeVideoDetails(
                videoId,
                snippetNode.path("title").asText(""),
                snippetNode.path("description").asText(""),
                snippetNode.path("channelTitle").asText(""),
                snippetNode.path("thumbnails").path("high").path("url").asText(""),
                snippetNode.path("publishedAt").asText(""),
                contentDetailsNode.path("duration").asText(""),
                statisticsNode.path("viewCount").asText(""),
                statisticsNode.path("likeCount").asText("")
        );
    }
}
