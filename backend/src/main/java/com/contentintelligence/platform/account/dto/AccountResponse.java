package com.contentintelligence.platform.account.dto;

import com.contentintelligence.platform.account.UserAccount;

import java.time.Instant;

public record AccountResponse(
        String id,
        String displayName,
        String email,
        Instant createdAt
) {

    public static AccountResponse from(UserAccount account) {
        return new AccountResponse(
                account.getId(),
                account.getDisplayName(),
                account.getEmail(),
                account.getCreatedAt()
        );
    }
}
