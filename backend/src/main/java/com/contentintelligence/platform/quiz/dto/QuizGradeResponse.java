package com.contentintelligence.platform.quiz.dto;

import java.util.List;

public record QuizGradeResponse(
        String videoId,
        int score,
        int total,
        int percentage,
        List<QuizGradeResultResponse> results
) {
}
