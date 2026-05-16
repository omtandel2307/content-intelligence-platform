package com.contentintelligence.platform.quiz.dto;

import java.time.Instant;
import java.util.List;

public record QuizResponse(
        String videoId,
        String model,
        Instant generatedAt,
        List<QuizQuestionResponse> questions
) {
}
