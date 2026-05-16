package com.contentintelligence.platform.rag;

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
        name = "transcript_chunks",
        indexes = {
                @Index(name = "idx_transcript_chunks_video_id", columnList = "video_id"),
                @Index(name = "idx_transcript_chunks_video_model", columnList = "video_id,embedding_model")
        }
)
public class TranscriptChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "video_id", nullable = false)
    private String videoId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "chunk_text", columnDefinition = "text", nullable = false)
    private String chunkText;

    @Column(name = "embedding_json", columnDefinition = "text", nullable = false)
    private String embeddingJson;

    @Column(name = "embedding_model", nullable = false)
    private String embeddingModel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TranscriptChunk() {
    }

    public TranscriptChunk(
            String videoId,
            Integer chunkIndex,
            String chunkText,
            String embeddingJson,
            String embeddingModel
    ) {
        this.videoId = videoId;
        this.chunkIndex = chunkIndex;
        this.chunkText = chunkText;
        this.embeddingJson = embeddingJson;
        this.embeddingModel = embeddingModel;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getVideoId() {
        return videoId;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public String getChunkText() {
        return chunkText;
    }

    public String getEmbeddingJson() {
        return embeddingJson;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
