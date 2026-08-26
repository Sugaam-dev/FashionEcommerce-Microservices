package com.pmrgsolution.shared.events.auth;

import java.time.Instant;

public record UserRegisteredEvent(
        String eventId,
        Instant occurredAt,
        String userId,
        String email,
        String fullName
) {
}
