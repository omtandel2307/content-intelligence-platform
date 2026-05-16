package com.contentintelligence.platform.rag;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TranscriptChunkRepository extends JpaRepository<TranscriptChunk, UUID> {

    long countByVideoIdAndEmbeddingModel(String videoId, String embeddingModel);

    List<TranscriptChunk> findByVideoIdAndEmbeddingModelOrderByChunkIndexAsc(
            String videoId,
            String embeddingModel
    );

    List<TranscriptChunk> findByVideoIdInAndEmbeddingModelOrderByVideoIdAscChunkIndexAsc(
            List<String> videoIds,
            String embeddingModel
    );

    void deleteByVideoId(String videoId);
}
