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
        name = "rag_chat_messages",
        indexes = {
                @Index(name = "idx_rag_chat_messages_video_id", columnList = "video_id"),
                @Index(name = "idx_rag_chat_messages_video_created", columnList = "video_id,created_at"),
                @Index(name = "idx_rag_chat_messages_account_video", columnList = "account_id,video_id,created_at")
        }
)
public class RagChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "video_id", nullable = false)
    private String videoId;

    @Column(name = "account_id")
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

    protected RagChatMessage() {
    }

    public RagChatMessage(
            String accountId,
            String videoId,
            RagChatRole role,
            String content,
            String provider,
            String model
    ) {
        this.accountId = accountId;
        this.videoId = videoId;
        this.role = role;
        this.content = content;
        this.provider = provider;
        this.model = model;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getVideoId() {
        return videoId;
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
