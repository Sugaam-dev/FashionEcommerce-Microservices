package com.pmrgsolution.inventoryservice.dto.response;

import java.time.Instant;

public record ReservationResponse(
        String orderId,
        boolean success,
        String message,
        Instant expiresAt
) {
}
