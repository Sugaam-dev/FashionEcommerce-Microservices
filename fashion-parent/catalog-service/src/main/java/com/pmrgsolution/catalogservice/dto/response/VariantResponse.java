package com.pmrgsolution.catalogservice.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record VariantResponse(
        UUID id,
        String sku,
        String size,
        String color,
        String colorHex,
        BigDecimal price,
        BigDecimal compareAtPrice,
        String variantImageUrl,
        boolean isActive
) {
}
