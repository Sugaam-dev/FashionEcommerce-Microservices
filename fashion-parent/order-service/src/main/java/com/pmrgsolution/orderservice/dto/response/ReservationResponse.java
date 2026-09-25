package com.pmrgsolution.orderservice.dto.response;

import java.time.Instant;

public record ReservationResponse(
        boolean success,
        String message,
        Instant expiresAt
) {}
