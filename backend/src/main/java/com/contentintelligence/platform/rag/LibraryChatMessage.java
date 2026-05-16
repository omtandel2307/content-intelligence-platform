package com.contentintelligence.platform.rag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "library_chat_messages",
        indexes = {
                @Index(name = "idx_library_chat_messages_account_created", columnList = "account_id,created_at")
        }
)
public class LibraryChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RagChatRole role;

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String model;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LibraryChatMessage() {
    }

    public LibraryChatMessage(
            String accountId,
            RagChatRole role,
            String content,
            String provider,
            String model
    ) {
        this.accountId = accountId;
        this.role = role;
        this.content = content;
        this.provider = provider;
        this.model = model;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public RagChatRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
