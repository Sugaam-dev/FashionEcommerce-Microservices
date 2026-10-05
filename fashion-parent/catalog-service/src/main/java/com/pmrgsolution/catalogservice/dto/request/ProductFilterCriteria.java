package com.pmrgsolution.catalogservice.dto.request;

import com.pmrgsolution.catalogservice.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductFilterCriteria(
        UUID categoryId,
        String brand,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Double minRating,
        ProductStatus status,
        String search
) {
}
