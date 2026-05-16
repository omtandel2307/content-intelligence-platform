package com.contentintelligence.platform.transcript;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.thoroldvix.api.Transcript;
import io.github.thoroldvix.api.TranscriptApiFactory;
import io.github.thoroldvix.api.TranscriptContent;
import io.github.thoroldvix.api.TranscriptList;
import io.github.thoroldvix.api.TranscriptRetrievalException;
import io.github.thoroldvix.api.YoutubeTranscriptApi;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class YoutubeTranscriptService {

    private static final Pattern PLAYER_RESPONSE_PATTERN =
            Pattern.compile("ytInitialPlayerResponse\\s*=\\s*(\\{.*?})\\s*;");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final YoutubeTranscriptApi transcriptApi = TranscriptApiFactory.createDefault();

    public FetchedTranscript fetchTranscript(String videoId) {
        String libraryFailureReason = null;
        String innertubeFailureReason = null;

        try {
            return fetchTranscriptFromLibrary(videoId);
        } catch (ResponseStatusException exception) {
            libraryFailureReason = exception.getReason();
        }

        try {
            FetchedTranscript innertubeTranscript = fetchTranscriptFromInnertube(videoId);

            if (innertubeTranscript != null) {
                return innertubeTranscript;
            }
        } catch (ResponseStatusException exception) {
            innertubeFailureReason = exception.getReason();
        }

        try {
            String watchHtml = getText("https://www.youtube.com/watch?v=" + videoId);
            JsonNode captionTrack = findCaptionTrack(watchHtml);
            String captionUrl = withJsonFormat(captionTrack.path("baseUrl").asText(""));
            String languageCode = captionTrack.path("languageCode").asText("");

            if (captionUrl.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transcript URL was not available."
                );
            }

            String transcriptJson = getText(captionUrl);

            return new FetchedTranscript(
                    extractTranscriptText(transcriptJson),
                    languageCode
            );
        } catch (ResponseStatusException exception) {
            if ((libraryFailureReason != null || innertubeFailureReason != null)
                    && exception.getReason() != null) {
                throw new ResponseStatusException(
                        exception.getStatusCode(),
                        "Library failed: " + libraryFailureReason
                                + " Innertube failed: " + innertubeFailureReason
                                + " Watch page failed: " + exception.getReason(),
                        exception
                );
            }

            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to fetch transcript for this video.",
                    exception
            );
        }
    }

    private FetchedTranscript fetchTranscriptFromLibrary(String videoId) {
        try {
            TranscriptList transcriptList = transcriptApi.listTranscripts(videoId);
            Transcript transcript = transcriptList.findTranscript("en");
            TranscriptContent content = transcript.fetch();

            return new FetchedTranscript(
                    extractTranscriptText(content),
                    transcript.getLanguageCode()
            );
        } catch (TranscriptRetrievalException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Transcript library could not retrieve captions: " + exception.getMessage(),
                    exception
            );
        }
    }

    private FetchedTranscript fetchTranscriptFromInnertube(String videoId) {
        try {
            String playerPayload = objectMapper.writeValueAsString(Map.of(
                    "context", Map.of(
                            "client", Map.of(
                                    "clientName", "WEB",
                                    "clientVersion", "2.20240509.00.00"
                            )
                    ),
                    "videoId", videoId
            ));
            String playerJson = postJson(
                    "https://www.youtube.com/youtubei/v1/player?prettyPrint=false",
                    playerPayload
            );
            JsonNode playerResponse = objectMapper.readTree(playerJson);
            JsonNode captionTracks = playerResponse
                    .path("captions")
                    .path("playerCaptionsTracklistRenderer")
                    .path("captionTracks");

            if (!captionTracks.isArray() || captionTracks.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Innertube metadata did not include caption tracks."
                );
            }

            JsonNode captionTrack = chooseCaptionTrack(captionTracks);
            String captionUrl = withJsonFormat(captionTrack.path("baseUrl").asText(""));

            if (captionUrl.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Innertube caption URL was not available."
                );
            }

            String transcriptJson = getText(captionUrl);

            if (transcriptJson == null || transcriptJson.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Innertube transcript response was empty."
                );
            }

            return new FetchedTranscript(
                    extractTranscriptText(transcriptJson),
                    captionTrack.path("languageCode").asText("")
            );
        } catch (Exception ignored) {
            if (ignored instanceof ResponseStatusException responseStatusException) {
                throw responseStatusException;
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Innertube transcript fetch failed: " + ignored.getMessage(),
                    ignored
            );
        }
    }

    private String getText(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Transcript source failed with status " + response.statusCode()
            );
        }

        if (response.statusCode() >= 300) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Transcript source returned redirect status " + response.statusCode()
            );
        }

        return response.body();
    }

    private String postJson(String url, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "YouTube player API failed with status " + response.statusCode()
            );
        }

        if (response.statusCode() >= 300) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "YouTube player API returned redirect status " + response.statusCode()
            );
        }

        return response.body();
    }

    private JsonNode findCaptionTrack(String watchHtml) throws IOException {
        Matcher playerResponseMatcher = PLAYER_RESPONSE_PATTERN.matcher(watchHtml);

        if (!playerResponseMatcher.find()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "YouTube player metadata did not include captions."
            );
        }

        JsonNode playerResponse = objectMapper.readTree(playerResponseMatcher.group(1));
        JsonNode captionTracks = playerResponse
                .path("captions")
                .path("playerCaptionsTracklistRenderer")
                .path("captionTracks");

        if (!captionTracks.isArray() || captionTracks.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No captions are available for this video."
            );
        }

        return chooseCaptionTrack(captionTracks);
    }

    private JsonNode chooseCaptionTrack(JsonNode captionTracks) {
        for (JsonNode track : captionTracks) {
            if ("en".equals(track.path("languageCode").asText())) {
                return track;
            }
        }

        for (JsonNode track : captionTracks) {
            if ("en".equals(track.path("vssId").asText().replace(".", ""))) {
                return track;
            }
        }

        return captionTracks.get(0);
    }

    private String withJsonFormat(String captionUrl) {
        if (captionUrl.contains("fmt=")) {
            return captionUrl;
        }

        return captionUrl + (captionUrl.contains("?") ? "&" : "?") + "fmt=json3";
    }

    private String extractTranscriptText(TranscriptContent transcriptContent) {
        if (transcriptContent == null || transcriptContent.getContent().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Transcript library returned no fragments."
            );
        }

        StringBuilder transcript = new StringBuilder();

        for (TranscriptContent.Fragment fragment : transcriptContent.getContent()) {
            String text = fragment.getText()
                    .replace("\n", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            if (!text.isBlank()) {
                transcript.append(text).append(" ");
            }
        }

        String result = transcript.toString().trim();

        if (result.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Transcript library returned empty text."
            );
        }

        return result;
    }

    private String extractTranscriptText(String transcriptJson) throws Exception {
        if (transcriptJson == null || transcriptJson.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Transcript response was empty."
            );
        }

        JsonNode root = objectMapper.readTree(transcriptJson);
        JsonNode events = root.path("events");

        if (!events.isArray() || events.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Transcript events were not available."
            );
        }

        StringBuilder transcript = new StringBuilder();

        for (JsonNode event : events) {
            JsonNode segments = event.path("segs");

            if (!segments.isArray()) {
                continue;
            }

            for (JsonNode segment : segments) {
                String text = segment.path("utf8").asText("")
                        .replace("\n", " ")
                        .replaceAll("\\s+", " ")
                        .trim();

                if (!text.isBlank()) {
                    transcript.append(text).append(" ");
                }
            }
        }

        String result = transcript.toString().trim();

        if (result.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Transcript text was empty."
            );
        }

        return result;
    }

    public record FetchedTranscript(String text, String languageCode) {
    }
}
