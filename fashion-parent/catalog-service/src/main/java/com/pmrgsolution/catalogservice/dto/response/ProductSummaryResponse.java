package com.pmrgsolution.catalogservice.dto.response;

import com.pmrgsolution.catalogservice.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSummaryResponse(
        UUID id,
        String title,
        String slug,
        String brand,
        BigDecimal basePrice,
        BigDecimal salePrice,
        String primaryImageUrl,
        double averageRating,
        int reviewCount,
        ProductStatus status,
        String categoryName
) {
}
