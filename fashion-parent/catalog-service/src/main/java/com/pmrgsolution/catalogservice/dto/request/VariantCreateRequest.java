package com.pmrgsolution.catalogservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record VariantCreateRequest(
        @NotBlank(message = "SKU is required")
        @Size(max = 100, message = "SKU must not exceed 100 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String sku,

        @Size(max = 30, message = "Size must not exceed 30 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String size,

        @Size(max = 50, message = "Color must not exceed 50 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String color,

        @Size(max = 10, message = "Color HEX must not exceed 10 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String colorHex,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than zero")
        BigDecimal price,

        @Positive(message = "Compare-at price must be greater than zero")
        BigDecimal compareAtPrice,

        @Size(max = 500, message = "Variant image URL must not exceed 500 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String variantImageUrl,

        boolean isActive
) {
}
