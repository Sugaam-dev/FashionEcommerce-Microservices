package com.pmrgsolution.shared.events.notification;

import java.time.Instant;
import java.util.Map;

/**
 * Requests the notification-service to dispatch a message via the specified channel.
 *
 * @param eventId    unique event ID for idempotency
 * @param occurredAt event creation timestamp
 * @param recipient  destination address — email, phone number, or device token
 * @param channel    delivery channel ({@link NotificationChannel})
 * @param templateId identifier of the message template to render
 * @param variables  template variable substitutions
 */
public record NotificationRequestEvent(
        String eventId,
        Instant occurredAt,
        String recipient,
        NotificationChannel channel,
        String templateId,
        Map<String, Object> variables
) {
}
