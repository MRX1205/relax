package com.relax.review;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.relax.auth.CurrentUser;
import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService reviewService;

    ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/technicians/{technicianId}/reviews")
    ApiResponse<List<ReviewMapper.ReviewView>> listByTechnician(
            @PathVariable long technicianId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ApiResponse.success(reviewService.listByTechnician(technicianId, page, size));
    }

    @GetMapping("/reviews")
    ApiResponse<List<ReviewMapper.ReviewView>> listAll(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ApiResponse.success(reviewService.listAll(page, size));
    }

    @PostMapping("/orders/{orderNo}/reviews")
    ApiResponse<ReviewMapper.ReviewView> createReview(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String orderNo, @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.success(reviewService.createReview(currentUser.id(), orderNo,
                request.score(), request.content()));
    }

    public record ReviewRequest(@Min(1) @Max(5) int score, @Size(max = 1000) String content) {}
}
