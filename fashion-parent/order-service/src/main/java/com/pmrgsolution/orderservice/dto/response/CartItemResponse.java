package com.pmrgsolution.orderservice.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        String sku,
        String productId,
        int quantity,
        Instant updatedAt
) {}
