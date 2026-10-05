package com.pmrgsolution.catalogservice.controller;

import com.pmrgsolution.catalogservice.dto.response.ReviewResponse;
import com.pmrgsolution.catalogservice.service.ReviewService;
import com.pmrgsolution.shared.core.response.ApiResponse;
import com.pmrgsolution.shared.core.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Reviews", description = "Admin endpoints for moderating and deleting customer reviews")
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "Get pending reviews awaiting admin moderation")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getModerationQueue(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<ReviewResponse> queue = reviewService.getModerationQueue(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Moderation queue retrieved", queue));
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve a customer review")
    public ResponseEntity<ApiResponse<Void>> approveReview(@PathVariable UUID id) {
        reviewService.approveReview(id);
        return ResponseEntity.ok(ApiResponse.ok("Review approved successfully", null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete or reject a customer review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable UUID id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok(ApiResponse.ok("Review deleted successfully", null));
    }
}
