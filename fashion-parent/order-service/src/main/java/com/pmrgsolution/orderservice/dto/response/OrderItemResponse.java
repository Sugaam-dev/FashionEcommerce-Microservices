package com.pmrgsolution.orderservice.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        String productId,
        String productTitle,
        String sku,
        String variantSize,
        String variantColor,
        String primaryImageUrl,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal totalPrice
) {}
