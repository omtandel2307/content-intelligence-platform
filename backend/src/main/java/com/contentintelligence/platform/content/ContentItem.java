package com.contentintelligence.platform.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "content_items")
public class ContentItem {

    @Id
    private String videoId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private String channelTitle;

    private String thumbnailUrl;
    private String publishedAt;
    private String duration;
    private String viewCount;
    private String likeCount;

    @Column(nullable = false)
    private Instant savedAt;

    protected ContentItem() {
    }

    public ContentItem(
            String videoId,
            String title,
            String description,
            String channelTitle,
            String thumbnailUrl,
            String publishedAt,
            String duration,
            String viewCount,
            String likeCount
    ) {
        this.videoId = videoId;
        this.title = title;
        this.description = description;
        this.channelTitle = channelTitle;
        this.thumbnailUrl = thumbnailUrl;
        this.publishedAt = publishedAt;
        this.duration = duration;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.savedAt = Instant.now();
    }

    public String getVideoId() {
        return videoId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getChannelTitle() {
        return channelTitle;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public String getDuration() {
        return duration;
    }

    public String getViewCount() {
        return viewCount;
    }

    public String getLikeCount() {
        return likeCount;
    }

    public Instant getSavedAt() {
        return savedAt;
    }
}
