package com.pmrgsolution.notificationservice.dto.response;

import com.pmrgsolution.notificationservice.entity.EmailUsageQuota;

import java.time.Instant;

public record EmailQuotaResponse(
        String dateKey,
        int count,
        int dailyLimit,
        int remaining,
        Instant lastSentAt
) {
    public static EmailQuotaResponse fromEntity(EmailUsageQuota entity) {
        int remaining = Math.max(0, entity.getDailyLimit() - entity.getCount());
        return new EmailQuotaResponse(
                entity.getDateKey(),
                entity.getCount(),
                entity.getDailyLimit(),
                remaining,
                entity.getLastSentAt()
        );
    }
}
