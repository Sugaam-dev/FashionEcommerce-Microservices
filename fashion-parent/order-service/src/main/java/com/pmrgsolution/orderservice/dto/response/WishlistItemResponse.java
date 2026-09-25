package com.pmrgsolution.orderservice.dto.response;

import java.time.Instant;
import java.util.UUID;

public record WishlistItemResponse(
        UUID id,
        String productId,
        Instant addedAt
) {}
