package com.pmrgsolution.catalogservice.service;

import com.pmrgsolution.catalogservice.client.OrderServiceClient;
import com.pmrgsolution.catalogservice.dto.request.CreateReviewRequest;
import com.pmrgsolution.catalogservice.dto.response.ReviewResponse;
import com.pmrgsolution.catalogservice.entity.Product;
import com.pmrgsolution.catalogservice.entity.ProductReview;
import com.pmrgsolution.catalogservice.repository.ProductRepository;
import com.pmrgsolution.catalogservice.repository.ProductReviewRepository;
import com.pmrgsolution.catalogservice.service.impl.ReviewServiceImpl;
import com.pmrgsolution.shared.core.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ProductReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderServiceClient orderServiceClient;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private UUID productId;
    private UUID userId;
    private UUID reviewId;
    private Product product;
    private CreateReviewRequest request;
    private ProductReview review;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        userId = UUID.randomUUID();
        reviewId = UUID.randomUUID();

        product = Product.builder()
                .id(productId)
                .title("Test Shirt")
                .averageRating(0.0)
                .reviewCount(0)
                .build();

        request = new CreateReviewRequest(5, "Great fit", "Loved the fabric quality!");

        review = ProductReview.builder()
                .id(reviewId)
                .product(product)
                .userId(userId)
                .reviewerName("Test User")
                .rating(5)
                .title("Great fit")
                .comment("Loved the fabric quality!")
                .verifiedPurchase(true)
                .approved(false)
                .build();
    }

    @Test
    @DisplayName("submitReview - when user purchased sets verifiedPurchase=true")
    void submitReview_whenUserPurchased_setsVerifiedPurchaseTrue() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByProductIdAndUserId(productId, userId)).thenReturn(false);
        when(orderServiceClient.hasPurchasedProduct(any(), eq(userId), eq(productId))).thenReturn(true);
        when(reviewRepository.save(any(ProductReview.class))).thenReturn(review);

        ReviewResponse response = reviewService.submitReview(productId, userId, "Test User", request);

        assertNotNull(response);
        assertTrue(response.verifiedPurchase());
        assertFalse(response.approved());
        verify(reviewRepository).save(any(ProductReview.class));
    }

    @Test
    @DisplayName("submitReview - when duplicate review throws ConflictException")
    void submitReview_whenDuplicateReview_throwsConflictException() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByProductIdAndUserId(productId, userId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> reviewService.submitReview(productId, userId, "Test User", request));
    }

    @Test
    @DisplayName("approveReview - approves and recalculates average rating")
    void approveReview_recalculatesProductRating() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewRepository.findAverageRating(productId)).thenReturn(Optional.of(4.8));
        when(reviewRepository.countApprovedReviews(productId)).thenReturn(10L);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        reviewService.approveReview(reviewId);

        assertTrue(review.isApproved());
        assertEquals(4.8, product.getAverageRating());
        assertEquals(10, product.getReviewCount());
        verify(productRepository).save(product);
    }
}
