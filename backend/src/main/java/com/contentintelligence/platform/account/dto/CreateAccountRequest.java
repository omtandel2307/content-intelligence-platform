package com.contentintelligence.platform.account.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAccountRequest(
        @NotBlank(message = "Display name is required.")
        String displayName,
        String email
) {
}
