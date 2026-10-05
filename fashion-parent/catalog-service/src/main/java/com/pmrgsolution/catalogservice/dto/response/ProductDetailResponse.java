package com.pmrgsolution.catalogservice.dto.response;

import com.pmrgsolution.catalogservice.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductDetailResponse(
        UUID id,
        String title,
        String slug,
        String skuPrefix,
        String shortDescription,
        String description,
        CategoryResponse category,
        String brand,
        ProductStatus status,
        BigDecimal basePrice,
        BigDecimal salePrice,
        List<String> tags,
        List<String> imageUrls,
        String primaryImageUrl,
        double averageRating,
        int reviewCount,
        List<VariantResponse> variants,
        Instant createdAt,
        Instant updatedAt
) {
}
