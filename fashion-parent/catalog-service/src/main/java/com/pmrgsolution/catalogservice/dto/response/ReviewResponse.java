package com.pmrgsolution.catalogservice.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID productId,
        UUID userId,
        String reviewerName,
        int rating,
        String title,
        String comment,
        boolean verifiedPurchase,
        boolean approved,
        Instant createdAt
) {
}
