package com.contentintelligence.platform.quiz;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "content_quizzes")
public class ContentQuiz {

    @Id
    private String videoId;

    @Column(columnDefinition = "text", nullable = false)
    private String quizJson;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private Instant generatedAt;

    protected ContentQuiz() {
    }

    public ContentQuiz(String videoId, String quizJson, String model) {
        this.videoId = videoId;
        this.quizJson = quizJson;
        this.model = model;
        this.generatedAt = Instant.now();
    }

    public String getVideoId() {
        return videoId;
    }

    public String getQuizJson() {
        return quizJson;
    }

    public String getModel() {
        return model;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void replaceWith(String quizJson, String model) {
        this.quizJson = quizJson;
        this.model = model;
        this.generatedAt = Instant.now();
    }
}
