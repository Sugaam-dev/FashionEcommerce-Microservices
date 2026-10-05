package com.pmrgsolution.catalogservice.controller;

import com.pmrgsolution.catalogservice.dto.request.CreateReviewRequest;
import com.pmrgsolution.catalogservice.dto.request.ProductFilterCriteria;
import com.pmrgsolution.catalogservice.dto.response.ProductDetailResponse;
import com.pmrgsolution.catalogservice.dto.response.ProductSummaryResponse;
import com.pmrgsolution.catalogservice.dto.response.ReviewResponse;
import com.pmrgsolution.catalogservice.enums.ProductStatus;
import com.pmrgsolution.catalogservice.security.UserPrincipal;
import com.pmrgsolution.catalogservice.service.ProductService;
import com.pmrgsolution.catalogservice.service.ReviewService;
import com.pmrgsolution.shared.core.response.ApiResponse;
import com.pmrgsolution.shared.core.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Public Products", description = "Public endpoints for searching and viewing products and reviews")
public class PublicProductController {

    private final ProductService productService;
    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "Search and filter published products with pagination")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> getProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        ProductFilterCriteria criteria = new ProductFilterCriteria(
                categoryId,
                brand,
                minPrice,
                maxPrice,
                minRating,
                ProductStatus.PUBLISHED,
                search
        );
        PageResponse<ProductSummaryResponse> response = productService.getProducts(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Products retrieved successfully", response));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get published product details by slug")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductBySlug(@PathVariable String slug) {
        ProductDetailResponse product = productService.getBySlug(slug);
        return ResponseEntity.ok(ApiResponse.ok("Product details retrieved successfully", product));
    }

    @GetMapping("/{productId}/reviews")
    @Operation(summary = "Get approved customer reviews for a product")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getProductReviews(
            @PathVariable UUID productId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        PageResponse<ReviewResponse> reviews = reviewService.getReviews(productId, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Product reviews retrieved successfully", reviews));
    }

    @PostMapping("/{productId}/reviews")
    @Operation(summary = "Submit a customer product review (authenticated)")
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            @PathVariable UUID productId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        String reviewerName = (principal.getEmail() != null) ? principal.getEmail() : "Customer";
        ReviewResponse review = reviewService.submitReview(productId, principal.getId(), reviewerName, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Review submitted successfully and is pending moderation", review));
    }
}
