package com.pmrgsolution.catalogservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.catalogservice.enums.BannerPosition;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record StoreBannerRequest(
        @NotBlank(message = "Banner title is required")
        @Size(max = 200, message = "Banner title must not exceed 200 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String title,

        @Size(max = 300, message = "Banner subtitle must not exceed 300 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String subtitle,

        @NotBlank(message = "Banner image URL is required")
        @Size(max = 500, message = "Banner image URL must not exceed 500 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String imageUrl,

        @Size(max = 500, message = "Banner target URL must not exceed 500 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String targetUrl,

        @NotNull(message = "Banner position is required")
        BannerPosition position,

        int displayOrder,

        boolean isActive,

        Instant startsAt,

        Instant endsAt
) {
}
