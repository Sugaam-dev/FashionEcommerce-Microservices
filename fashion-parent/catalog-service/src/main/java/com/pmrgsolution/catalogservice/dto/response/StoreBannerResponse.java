package com.pmrgsolution.catalogservice.dto.response;

import com.pmrgsolution.catalogservice.enums.BannerPosition;

import java.time.Instant;
import java.util.UUID;

public record StoreBannerResponse(
        UUID id,
        String title,
        String subtitle,
        String imageUrl,
        String targetUrl,
        BannerPosition position,
        int displayOrder,
        boolean isActive,
        Instant startsAt,
        Instant endsAt
) {
}
