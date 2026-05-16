package com.contentintelligence.platform.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record QuizGradeRequest(
        @Valid
        @NotEmpty(message = "At least one answer is required.")
        List<QuizAnswerRequest> answers
) {
}
