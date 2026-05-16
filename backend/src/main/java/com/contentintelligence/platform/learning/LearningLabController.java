package com.contentintelligence.platform.learning;

import com.contentintelligence.platform.learning.dto.CompareVideosRequest;
import com.contentintelligence.platform.learning.dto.CompareVideosResponse;
import com.contentintelligence.platform.learning.dto.LearningTimelineResponse;
import com.contentintelligence.platform.learning.dto.ProjectPlanRequest;
import com.contentintelligence.platform.learning.dto.ProjectPlanResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts/{accountId}/learning")
public class LearningLabController {

    private final LearningLabService learningLabService;

    public LearningLabController(LearningLabService learningLabService) {
        this.learningLabService = learningLabService;
    }

    @PostMapping("/compare")
    public CompareVideosResponse compareVideos(
            @PathVariable String accountId,
            @Valid @RequestBody CompareVideosRequest request
    ) {
        return learningLabService.compareVideos(accountId, request.videoIds());
    }

    @PostMapping("/project-plan")
    public ProjectPlanResponse generateProjectPlan(
            @PathVariable String accountId,
            @RequestBody ProjectPlanRequest request
    ) {
        return learningLabService.generateProjectPlan(accountId, request.goal());
    }

    @GetMapping("/timeline")
    public LearningTimelineResponse getTimeline(@PathVariable String accountId) {
        return learningLabService.getTimeline(accountId);
    }
}
