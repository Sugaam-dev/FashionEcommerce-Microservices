package com.pmrgsolution.catalogservice.service.impl;

import com.pmrgsolution.catalogservice.client.OrderServiceClient;
import com.pmrgsolution.catalogservice.dto.request.CreateReviewRequest;
import com.pmrgsolution.catalogservice.dto.response.ReviewResponse;
import com.pmrgsolution.catalogservice.entity.Product;
import com.pmrgsolution.catalogservice.entity.ProductReview;
import com.pmrgsolution.catalogservice.exception.ProductNotFoundException;
import com.pmrgsolution.catalogservice.mapper.ReviewMapper;
import com.pmrgsolution.catalogservice.repository.ProductRepository;
import com.pmrgsolution.catalogservice.repository.ProductReviewRepository;
import com.pmrgsolution.catalogservice.service.ReviewService;
import com.pmrgsolution.shared.core.exception.ConflictException;
import com.pmrgsolution.shared.core.exception.ResourceNotFoundException;
import com.pmrgsolution.shared.core.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderServiceClient orderServiceClient;

    @Value("${app.internal.service-key:fashion-internal-microservice-secret-key-2026}")
    private String internalServiceKey;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviews(UUID productId, Pageable pageable) {
        int boundedPageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Pageable capped = PageRequest.of(pageable.getPageNumber(), boundedPageSize, pageable.getSort());

        Page<ProductReview> reviewsPage = reviewRepository
                .findByProductIdAndApprovedTrueOrderByCreatedAtDesc(productId, capped);
        return PageResponse.from(reviewsPage.map(ReviewMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getModerationQueue(Pageable pageable) {
        int boundedPageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Pageable capped = PageRequest.of(pageable.getPageNumber(), boundedPageSize, pageable.getSort());

        Page<ProductReview> pendingPage = reviewRepository.findByApprovedFalseOrderByCreatedAtDesc(capped);
        return PageResponse.from(pendingPage.map(ReviewMapper::toResponse));
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ReviewResponse submitReview(UUID productId, UUID userId, String reviewerName, CreateReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));

        if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new ConflictException("You have already submitted a review for this product");
        }

        boolean verifiedPurchase = false;
        try {
            verifiedPurchase = orderServiceClient.hasPurchasedProduct(internalServiceKey, userId, productId);
        } catch (Exception e) {
            log.warn("Could not verify purchase with order-service for userId={}, productId={}: {}",
                    userId, productId, e.getMessage());
        }

        ProductReview review = ReviewMapper.toEntity(request, product, userId, reviewerName, verifiedPurchase);
        ProductReview saved = reviewRepository.save(review);

        log.info("Submitted review id={} for product id={} by userId={}, verifiedPurchase={}",
                saved.getId(), productId, userId, verifiedPurchase);

        return ReviewMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void approveReview(UUID reviewId) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        if (review.isApproved()) {
            return;
        }

        review.setApproved(true);
        reviewRepository.save(review);

        recalculateProductRating(review.getProduct());
        log.info("Approved review id={} for product id={}", reviewId, review.getProduct().getId());
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteReview(UUID reviewId) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        Product product = review.getProduct();
        reviewRepository.delete(review);

        recalculateProductRating(product);
        log.info("Deleted review id={} for product id={}", reviewId, product.getId());
    }

    private void recalculateProductRating(Product product) {
        if (product == null) {
            return;
        }
        Optional<Double> avgRating = reviewRepository.findAverageRating(product.getId());
        long count = reviewRepository.countApprovedReviews(product.getId());

        double roundedAvg = avgRating.map(val -> BigDecimal.valueOf(val)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue())
                .orElse(0.0);

        product.setAverageRating(roundedAvg);
        product.setReviewCount((int) count);
        productRepository.save(product);

        log.debug("Recalculated product id={} rating: avg={}, count={}", product.getId(), roundedAvg, count);
    }
}
