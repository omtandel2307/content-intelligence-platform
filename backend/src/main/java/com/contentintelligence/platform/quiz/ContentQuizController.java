package com.contentintelligence.platform.quiz;

import com.contentintelligence.platform.quiz.dto.QuizGradeRequest;
import com.contentintelligence.platform.quiz.dto.QuizGradeResponse;
import com.contentintelligence.platform.quiz.dto.QuizResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content/{videoId}/quiz")
public class ContentQuizController {

    private final ContentQuizService contentQuizService;

    public ContentQuizController(ContentQuizService contentQuizService) {
        this.contentQuizService = contentQuizService;
    }

    @GetMapping
    public ResponseEntity<QuizResponse> getQuiz(@PathVariable String videoId) {
        return contentQuizService.getQuiz(videoId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public QuizResponse generateQuiz(@PathVariable String videoId) {
        return contentQuizService.generateQuiz(videoId);
    }

    @PostMapping("/grade")
    public QuizGradeResponse gradeQuiz(
            @PathVariable String videoId,
            @Valid @RequestBody QuizGradeRequest request
    ) {
        return contentQuizService.gradeQuiz(videoId, request);
    }
}
