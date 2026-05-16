package com.contentintelligence.platform.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "account_saved_content",
        indexes = {
                @Index(name = "idx_account_saved_content_account", columnList = "account_id"),
                @Index(name = "idx_account_saved_content_account_video", columnList = "account_id,video_id")
        }
)
public class AccountSavedContent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "video_id", nullable = false)
    private String videoId;

    @Column(name = "saved_at", nullable = false)
    private Instant savedAt;

    protected AccountSavedContent() {
    }

    public AccountSavedContent(String accountId, String videoId) {
        this.accountId = accountId;
        this.videoId = videoId;
        this.savedAt = Instant.now();
    }

    public String getAccountId() {
        return accountId;
    }

    public String getVideoId() {
        return videoId;
    }

    public Instant getSavedAt() {
        return savedAt;
    }
}
