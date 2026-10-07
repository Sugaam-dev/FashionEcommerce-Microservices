package com.pmrgsolution.notificationservice.dto.response;

import com.pmrgsolution.notificationservice.entity.NotificationLog;
import com.pmrgsolution.notificationservice.enums.NotificationStatus;
import com.pmrgsolution.shared.core.util.MaskingUtil;
import com.pmrgsolution.shared.events.notification.NotificationChannel;

import java.time.Instant;
import java.util.UUID;

public record NotificationLogResponse(
        UUID id,
        String recipient,
        NotificationChannel channel,
        String templateId,
        String subject,
        NotificationStatus status,
        String errorMessage,
        int retryCount,
        Instant sentAt,
        Instant createdAt
) {
    public static NotificationLogResponse fromEntity(NotificationLog entity) {
        return new NotificationLogResponse(
                entity.getId(),
                MaskingUtil.maskEmail(entity.getRecipient()),
                entity.getChannel(),
                entity.getTemplateId(),
                entity.getSubject(),
                entity.getStatus(),
                entity.getErrorMessage(),
                entity.getRetryCount(),
                entity.getSentAt(),
                entity.getCreatedAt()
        );
    }
}
