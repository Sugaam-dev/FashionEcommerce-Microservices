package com.pmrgsolution.shared.events.notification;

/**
 * Delivery channel for outbound notifications.
 * Extend with new values as channels (e.g. WHATSAPP, IN_APP) are onboarded.
 * Jackson serialises this as {@code "EMAIL"}, {@code "SMS"}, etc. by default.
 */
public enum NotificationChannel {
    EMAIL,
    SMS,
    PUSH
}
