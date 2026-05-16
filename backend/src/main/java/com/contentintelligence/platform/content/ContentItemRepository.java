package com.contentintelligence.platform.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentItemRepository extends JpaRepository<ContentItem, String> {
    List<ContentItem> findAllByOrderBySavedAtDesc();
}
