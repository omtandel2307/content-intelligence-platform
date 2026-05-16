package com.contentintelligence.platform.rag;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RagChatMessageRepository extends JpaRepository<RagChatMessage, UUID> {

    List<RagChatMessage> findByVideoIdOrderByCreatedAtAsc(String videoId);

    List<RagChatMessage> findByAccountIdAndVideoIdOrderByCreatedAtAsc(String accountId, String videoId);

    List<RagChatMessage> findByAccountIdOrderByCreatedAtDesc(String accountId);

    List<RagChatMessage> findTop5ByAccountIdAndRoleOrderByCreatedAtDesc(
            String accountId,
            RagChatRole role
    );

    List<RagChatMessage> findTop5ByAccountIdOrderByCreatedAtDesc(String accountId);

    long countByAccountId(String accountId);
}
