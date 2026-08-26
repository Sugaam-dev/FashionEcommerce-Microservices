package com.pmrgsolution.shared.events.notification;

import java.time.Instant;
import java.util.Map;

public record NotificationRequestEvent(
        String eventId,
        Instant occurredAt,
        String recipient,
        String channel,
        String templateId,
        Map<String, Object> variables
) {
}
