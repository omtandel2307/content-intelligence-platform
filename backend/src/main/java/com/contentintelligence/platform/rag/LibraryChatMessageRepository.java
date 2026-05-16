package com.contentintelligence.platform.rag;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LibraryChatMessageRepository extends JpaRepository<LibraryChatMessage, UUID> {

    List<LibraryChatMessage> findByAccountIdOrderByCreatedAtAsc(String accountId);
}
