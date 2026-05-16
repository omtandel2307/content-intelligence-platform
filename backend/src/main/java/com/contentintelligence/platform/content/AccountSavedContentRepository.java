package com.contentintelligence.platform.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountSavedContentRepository extends JpaRepository<AccountSavedContent, UUID> {

    Optional<AccountSavedContent> findByAccountIdAndVideoId(String accountId, String videoId);

    List<AccountSavedContent> findAllByAccountIdOrderBySavedAtDesc(String accountId);

    long countByAccountId(String accountId);
}
