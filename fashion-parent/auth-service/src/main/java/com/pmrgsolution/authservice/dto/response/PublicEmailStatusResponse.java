package com.pmrgsolution.authservice.dto.response;

public record PublicEmailStatusResponse(
        boolean emailServiceActive,
        String alternativeMethod,
        int dailyQuotaUsed,
        int dailyQuotaLimit
) {
}
