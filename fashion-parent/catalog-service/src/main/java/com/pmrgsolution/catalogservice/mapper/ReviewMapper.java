package com.pmrgsolution.catalogservice.mapper;

import com.pmrgsolution.catalogservice.dto.request.CreateReviewRequest;
import com.pmrgsolution.catalogservice.dto.response.ReviewResponse;
import com.pmrgsolution.catalogservice.entity.Product;
import com.pmrgsolution.catalogservice.entity.ProductReview;

import java.util.UUID;

public final class ReviewMapper {

    private ReviewMapper() {
    }

    public static ProductReview toEntity(CreateReviewRequest request, Product product, UUID userId, String reviewerName, boolean verifiedPurchase) {
        if (request == null) {
            return null;
        }
        return ProductReview.builder()
                .product(product)
                .userId(userId)
                .reviewerName(reviewerName != null ? reviewerName.trim() : "Anonymous")
                .rating(request.rating())
                .title(request.title() != null ? request.title().trim() : null)
                .comment(request.comment().trim())
                .verifiedPurchase(verifiedPurchase)
                .approved(false)
                .build();
    }

    public static ReviewResponse toResponse(ProductReview review) {
        if (review == null) {
            return null;
        }
        return new ReviewResponse(
                review.getId(),
                review.getProduct() != null ? review.getProduct().getId() : null,
                review.getUserId(),
                review.getReviewerName(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.isVerifiedPurchase(),
                review.isApproved(),
                review.getCreatedAt()
        );
    }
}
