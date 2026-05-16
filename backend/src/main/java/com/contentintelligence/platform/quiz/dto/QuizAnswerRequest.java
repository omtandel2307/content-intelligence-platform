package com.contentintelligence.platform.quiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuizAnswerRequest(
        @NotBlank(message = "Question id is required.")
        String questionId,

        @NotNull(message = "Selected option is required.")
        @Min(0)
        @Max(1)
        Integer selectedOptionIndex
) {
}
