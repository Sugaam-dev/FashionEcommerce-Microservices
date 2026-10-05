package com.pmrgsolution.authservice.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ActiveSessionResponse(
        UUID sessionId,
        String ipAddress,
        String userAgent,
        Instant createdAt,
        Instant lastActivityAt,
        boolean current
) {
}
