package com.contentintelligence.platform.quiz.dto;

public record QuizGradeResultResponse(
        String questionId,
        String question,
        Integer selectedOptionIndex,
        Integer correctOptionIndex,
        String selectedAnswer,
        String correctAnswer,
        boolean correct,
        String explanation
) {
}
