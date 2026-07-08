package com.contentintelligence.platform.quiz;

import com.contentintelligence.platform.ai.OpenAiClient;
import com.contentintelligence.platform.content.ContentItem;
import com.contentintelligence.platform.content.ContentItemRepository;
import com.contentintelligence.platform.quiz.dto.QuizAnswerRequest;
import com.contentintelligence.platform.quiz.dto.QuizGradeRequest;
import com.contentintelligence.platform.quiz.dto.QuizGradeResponse;
import com.contentintelligence.platform.quiz.dto.QuizGradeResultResponse;
import com.contentintelligence.platform.quiz.dto.QuizQuestionResponse;
import com.contentintelligence.platform.quiz.dto.QuizResponse;
import com.contentintelligence.platform.transcript.ContentTranscript;
import com.contentintelligence.platform.transcript.ContentTranscriptRepository;
import com.contentintelligence.platform.transcript.TranscriptStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
public class ContentQuizService {

    private static final int QUESTION_COUNT = 5;

    private final ContentQuizRepository quizRepository;
    private final ContentItemRepository contentItemRepository;
    private final ContentTranscriptRepository transcriptRepository;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ContentQuizService(
            ContentQuizRepository quizRepository,
            ContentItemRepository contentItemRepository,
            ContentTranscriptRepository transcriptRepository,
            OpenAiClient openAiClient
    ) {
        this.quizRepository = quizRepository;
        this.contentItemRepository = contentItemRepository;
        this.transcriptRepository = transcriptRepository;
        this.openAiClient = openAiClient;
    }

    public Optional<QuizResponse> getQuiz(String videoId) {
        return quizRepository.findById(videoId).map(this::toQuizResponse);
    }

    public QuizResponse generateQuiz(String videoId) {
        ContentItem contentItem = contentItemRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Save this video before generating a quiz."
                ));
        ContentTranscript transcript = transcriptRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Fetch the transcript before generating a quiz."
                ));

        if (transcript.getStatus() != TranscriptStatus.READY
                || transcript.getTranscriptText() == null
                || transcript.getTranscriptText().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A ready transcript is required before generating a quiz."
            );
        }

        String normalizedQuizJson = generateQuestionByQuestionQuiz(
                contentItem.getTitle(),
                transcript.getTranscriptText()
        );
        ContentQuiz quiz = quizRepository.findById(videoId)
                .orElseGet(() -> new ContentQuiz(
                        videoId,
                        normalizedQuizJson,
                        openAiClient.getChatModel()
                ));
        quiz.replaceWith(normalizedQuizJson, openAiClient.getChatModel());

        return toQuizResponse(quizRepository.save(quiz));
    }

    public QuizGradeResponse gradeQuiz(String videoId, QuizGradeRequest request) {
        ContentQuiz quiz = quizRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Generate a quiz before grading answers."
                ));
        JsonNode questions = parseQuestions(quiz.getQuizJson());
        Map<String, Integer> submittedAnswers = new HashMap<>();

        for (QuizAnswerRequest answer : request.answers()) {
            submittedAnswers.put(answer.questionId(), answer.selectedOptionIndex());
        }

        List<QuizGradeResultResponse> results = new ArrayList<>();
        int score = 0;

        for (JsonNode question : questions) {
            String questionId = question.path("id").asText();
            int correctOptionIndex = question.path("correctOptionIndex").asInt();
            Integer selectedOptionIndex = submittedAnswers.get(questionId);
            String selectedAnswer = optionText(question, selectedOptionIndex);
            String correctAnswer = optionText(question, correctOptionIndex);
            boolean correct = selectedOptionIndex != null
                    && selectedOptionIndex == correctOptionIndex;

            if (correct) {
                score++;
            }

            results.add(new QuizGradeResultResponse(
                    questionId,
                    question.path("question").asText(),
                    selectedOptionIndex,
                    correctOptionIndex,
                    selectedAnswer,
                    correctAnswer,
                    correct,
                    question.path("explanation").asText("")
            ));
        }

        int total = results.size();
        int percentage = total == 0 ? 0 : Math.round((score * 100.0f) / total);

        return new QuizGradeResponse(videoId, score, total, percentage, results);
    }

    private QuizResponse toQuizResponse(ContentQuiz quiz) {
        JsonNode questions = parseQuestions(quiz.getQuizJson());
        List<QuizQuestionResponse> questionResponses = new ArrayList<>();

        for (JsonNode question : questions) {
            List<String> options = new ArrayList<>();
            for (JsonNode option : question.path("options")) {
                options.add(option.asText());
            }

            questionResponses.add(new QuizQuestionResponse(
                    question.path("id").asText(),
                    question.path("question").asText(),
                    options
            ));
        }

        return new QuizResponse(
                quiz.getVideoId(),
                quiz.getModel(),
                quiz.getGeneratedAt(),
                questionResponses
        );
    }

    private String normalizeQuizJson(String quizJson) {
        try {
            JsonNode questions = parseQuestions(quizJson);
            ArrayNode normalizedQuestions = objectMapper.createArrayNode();
            int questionIndex = 1;

            for (JsonNode question : questions) {
                String questionText = firstText(
                        question,
                        "question",
                        "statement",
                        "text"
                );

                if (questionText.isBlank()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "OpenAI quiz response included a question without text."
                    );
                }

                int correctOptionIndex = resolveTrueFalseAnswer(question);

                ObjectNode normalizedQuestion = objectMapper.createObjectNode();
                normalizedQuestion.put("id", "q" + questionIndex);
                normalizedQuestion.put("question", questionText);
                normalizedQuestion.set("options", trueFalseOptions());
                normalizedQuestion.put("correctOptionIndex", correctOptionIndex);
                normalizedQuestion.put(
                        "explanation",
                        firstText(question, "explanation", "reason", "rationale")
                );
                normalizedQuestions.add(normalizedQuestion);
                questionIndex++;
            }

            if (normalizedQuestions.size() == 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "OpenAI quiz response did not include questions."
                );
            }

            ObjectNode root = objectMapper.createObjectNode();
            root.set("questions", normalizedQuestions);
            return objectMapper.writeValueAsString(root);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI returned quiz JSON that could not be parsed. Try generating again.",
                    exception
            );
        }
    }
    private String generateQuestionByQuestionQuiz(String title, String transcriptText) {
        try {
            ArrayNode normalizedQuestions = objectMapper.createArrayNode();
            List<Boolean> desiredAnswers = new ArrayList<>(List.of(true, true, false, false, true));
            List<String> questionSkills = questionSkillPattern();
            Collections.shuffle(desiredAnswers);

            for (int index = 0; index < QUESTION_COUNT; index++) {
                boolean desiredAnswer = desiredAnswers.get(index);
                String questionSkill = questionSkills.get(index % questionSkills.size());
                String quizJson = openAiClient.generateJson(
                        quizSystemPrompt(),
                        singleQuestionPrompt(
                                title,
                                transcriptExcerpt(transcriptText, index),
                                index + 1,
                                desiredAnswer,
                                questionSkill
                        )
                );
                normalizedQuestions.add(normalizeSingleQuestion(quizJson, index + 1, desiredAnswer));
            }

            ObjectNode root = objectMapper.createObjectNode();
            root.set("questions", normalizedQuestions);
            return objectMapper.writeValueAsString(root);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI quiz generation failed while creating individual questions.",
                    exception
            );
        }
    }

    private List<String> questionSkillPattern() {
        List<String> skills = new ArrayList<>(List.of(
                "core concept understanding",
                "cause and effect",
                "comparison between ideas",
                "applying the taught idea",
                "misconception detection"
        ));
        Collections.shuffle(skills);
        return skills;
    }

    private ObjectNode normalizeSingleQuestion(String quizJson, int questionIndex, boolean desiredAnswer) {
        JsonNode question = parseSingleQuestion(quizJson);
        String questionText = firstText(question, "question", "statement", "text");

        if (questionText.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI quiz response included a question without text."
            );
        }

        int correctOptionIndex = resolveTrueFalseAnswer(question);
        int expectedOptionIndex = desiredAnswer ? 0 : 1;

        if (correctOptionIndex != expectedOptionIndex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI generated a quiz answer that did not match the requested true/false balance."
            );
        }

        ObjectNode normalizedQuestion = objectMapper.createObjectNode();
        normalizedQuestion.put("id", "q" + questionIndex);
        normalizedQuestion.put("question", questionText);
        normalizedQuestion.set("options", trueFalseOptions());
        normalizedQuestion.put("correctOptionIndex", correctOptionIndex);
        normalizedQuestion.put(
                "explanation",
                firstText(question, "explanation", "reason", "rationale")
        );
        return normalizedQuestion;
    }

    private JsonNode parseSingleQuestion(String quizJson) {
        try {
            JsonNode root = objectMapper.readTree(extractJsonPayload(quizJson));

            if (root.isArray() && !root.isEmpty()) {
                return root.get(0);
            }

            JsonNode questions = root.path("questions");
            if (questions.isArray() && !questions.isEmpty()) {
                return questions.get(0);
            }

            if (root.has("question") || root.has("statement") || root.has("text")) {
                return root;
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI quiz response did not contain a question object."
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Quiz JSON could not be parsed.",
                    exception
            );
        }
    }

    private JsonNode parseQuestions(String quizJson) {
        try {
            JsonNode root = objectMapper.readTree(extractJsonPayload(quizJson));
            JsonNode questions = root.isArray() ? root : root.path("questions");

            if (!questions.isArray()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Quiz JSON did not include a questions array."
                );
            }

            return questions;
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Quiz JSON could not be parsed.",
                    exception
            );
        }
    }

    private ArrayNode trueFalseOptions() {
        ArrayNode options = objectMapper.createArrayNode();
        options.add("True");
        options.add("False");
        return options;
    }

    private int resolveTrueFalseAnswer(JsonNode question) {
        int answerFromIndex = firstValidIndex(
                question,
                "correctOptionIndex",
                "correctAnswerIndex",
                "answerIndex"
        );

        if (answerFromIndex >= 0) {
            JsonNode options = question.path("options");

            if (options.isArray() && options.size() > answerFromIndex) {
                String selectedOption = options.path(answerFromIndex).asText("");
                Integer selectedBoolean = trueFalseIndexFromText(selectedOption);

                if (selectedBoolean != null) {
                    return selectedBoolean;
                }
            }

            return answerFromIndex;
        }

        String answerText = firstText(
                question,
                "correctAnswer",
                "correct_answer",
                "answer",
                "isTrue",
                "is_true"
        );

        Integer answerFromText = trueFalseIndexFromText(answerText);
        if (answerFromText != null) {
            return answerFromText;
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "OpenAI quiz response did not include a usable true/false answer key."
        );
    }

    private Integer trueFalseIndexFromText(String text) {
        String normalized = text.toLowerCase();

        if (normalized.contains("true") || normalized.equals("yes")) {
            return 0;
        }

        if (normalized.contains("false") || normalized.equals("no")) {
            return 1;
        }

        return null;
    }

    private int firstValidIndex(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.path(fieldName);

            if (field.isInt() && field.asInt() >= 0 && field.asInt() <= 1) {
                return field.asInt();
            }

            if (field.isTextual()) {
                try {
                    int parsed = Integer.parseInt(field.asText());

                    if (parsed >= 0 && parsed <= 1) {
                        return parsed;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return -1;
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.path(fieldName);

            if (field.isBoolean()) {
                return Boolean.toString(field.asBoolean());
            }

            String text = field.asText("");

            if (!text.isBlank()) {
                return text.trim();
            }
        }

        return "";
    }

    private String extractJsonPayload(String text) {
        int objectStart = text.indexOf('{');
        int objectEnd = text.lastIndexOf('}');
        int arrayStart = text.indexOf('[');
        int arrayEnd = text.lastIndexOf(']');

        if (objectStart >= 0 && objectEnd > objectStart) {
            return text.substring(objectStart, objectEnd + 1);
        }

        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            return text.substring(arrayStart, arrayEnd + 1);
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "OpenAI did not return JSON."
        );
    }

    private String optionText(JsonNode question, Integer optionIndex) {
        if (optionIndex == null || optionIndex < 0 || optionIndex > 1) {
            return null;
        }

        return question.path("options").path(optionIndex).asText(null);
    }

    private String quizSystemPrompt() {
        return """
                You generate true/false learning-check quizzes from video transcripts.
                Return strict JSON only. Do not use markdown or commentary.
                First infer the main topic, lesson, or skill being taught.
                Questions should test what a learner should understand after watching the video, not minor transcript details or exact wording.
                Questions must still be supported by the transcript's taught ideas.
                Each question must be a clear statement that can be marked true or false.
                Each question needs exactly these 2 options: ["True", "False"].
                correctOptionIndex must be 0 for True or 1 for False.
                """;
    }

    private String singleQuestionPrompt(
            String title,
            String transcriptExcerpt,
            int questionNumber,
            boolean desiredAns,
            String questionSkill
    ) {
        int correctOptionIndex = desiredAns ? 0 : 1;
        String correctAnswer = desiredAns ? "True" : "False";
        return """
                Create exactly 1 true/false quiz question using this transcript excerpt as source material.
                This is question %d of %d.

                The target correct answer is: %s.
                The question focus is: %s.

                Return exactly this JSON shape:
                {
                  "question": "A learning-check statement about the topic taught in the video",
                  "options": ["True", "False"],
                  "correctOptionIndex": %d,
                  "explanation": "One sentence explaining why the answer is correct."
                }

                Rules:
                - Infer what topic, concept, skill, or lesson the excerpt is teaching.
                - Ask about the taught idea, not the transcript wording.
                - Do not ask about timestamps, speaker phrasing, examples that are only trivia, or tiny details.
                - The question should test understanding someone should have after watching this part of the video.
                - The answer must be supported by the transcript excerpt's taught ideas.
                - Make the question medium difficulty.
                - Avoid copying wording directly from the transcript.
                - If the target correct answer is True, write a statement that accurately reflects the concept being taught.
                - If the target correct answer is False, write a plausible misconception or misunderstanding of the taught concept.
                - For false statements, prefer subtle mistakes such as reversing cause/effect, changing scope, confusing two concepts, overstating a claim, or applying the idea incorrectly.
                - The explanation must explain why the generated statement is true or false based on the lesson in the excerpt.
                - Use exactly the field names shown above.
                - Use exactly these options: ["True", "False"].
                - correctOptionIndex must be exactly %d.
                - Return one JSON object only.

                Video title: %s

                Transcript excerpt:
                %s
                """.formatted(
                questionNumber,
                QUESTION_COUNT,
                correctAnswer,
                questionSkill,
                correctOptionIndex,
                correctOptionIndex,
                title,
                transcriptExcerpt
        );
    }

    private String quizUserPrompt(String title, String transcriptText) {
        return """
                Create a %d-question true/false quiz for this video.
                Mix true and false answers when the transcript supports it.

                JSON schema:
                {
                  "questions": [
                    {
                      "question": "A factual statement about the video",
                      "options": ["True", "False"],
                      "correctOptionIndex": 0,
                      "explanation": "One sentence explaining why the answer is correct."
                    }
                  ]
                }

                Use exactly the field names shown above.
                Do not use true/false booleans for correctOptionIndex.

                Video title: %s

                Transcript:
                %s
                """.formatted(QUESTION_COUNT, title, limitTranscript(transcriptText));
    }

    private String limitTranscript(String transcriptText) {
        int maxLength = 24000;

        if (transcriptText.length() <= maxLength) {
            return transcriptText;
        }

        return transcriptText.substring(0, maxLength)
                + "\n\n[Transcript truncated for quiz generation.]";
    }

    private String transcriptExcerpt(String transcriptText, int excerptIndex) {
        int excerptLength = 4500;
        String compactTranscript = transcriptText.replaceAll("\\s+", " ").trim();

        if (compactTranscript.length() <= excerptLength) {
            return compactTranscript;
        }

        int usableLength = compactTranscript.length() - excerptLength;
        int start = Math.min(
                usableLength,
                (int) Math.round((usableLength / (double) Math.max(1, QUESTION_COUNT - 1)) * excerptIndex)
        );

        return compactTranscript.substring(start, start + excerptLength);
    }
}
