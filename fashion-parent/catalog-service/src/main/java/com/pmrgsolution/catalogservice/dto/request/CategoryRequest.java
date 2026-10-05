package com.pmrgsolution.catalogservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name must not exceed 100 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String name,

        @Size(max = 120, message = "Category slug must not exceed 120 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String slug,

        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String description,

        UUID parentId,

        @Size(max = 500, message = "Icon URL must not exceed 500 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String iconUrl,

        @Size(max = 500, message = "Banner URL must not exceed 500 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String bannerUrl,

        int displayOrder,

        boolean isActive
) {
    public CategoryRequest {
        if (isActive == false && displayOrder == 0) {
            // keep standard defaults
        }
    }
}
