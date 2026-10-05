package com.pmrgsolution.catalogservice.service;

import com.pmrgsolution.catalogservice.dto.request.CreateReviewRequest;
import com.pmrgsolution.catalogservice.dto.response.ReviewResponse;
import com.pmrgsolution.shared.core.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {

    PageResponse<ReviewResponse> getReviews(UUID productId, Pageable pageable);

    PageResponse<ReviewResponse> getModerationQueue(Pageable pageable);

    ReviewResponse submitReview(UUID productId, UUID userId, String reviewerName, CreateReviewRequest request);

    void approveReview(UUID reviewId);

    void deleteReview(UUID reviewId);
}
