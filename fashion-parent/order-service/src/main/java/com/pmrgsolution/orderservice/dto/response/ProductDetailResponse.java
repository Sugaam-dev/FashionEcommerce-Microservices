package com.pmrgsolution.orderservice.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductDetailResponse(
        UUID id,
        String title,
        String slug,
        String skuPrefix,
        String shortDescription,
        String description,
        String brand,
        String status,
        BigDecimal basePrice,
        BigDecimal salePrice,
        List<String> tags,
        List<String> imageUrls,
        String primaryImageUrl,
        double averageRating,
        int reviewCount,
        List<VariantResponse> variants
) {}
