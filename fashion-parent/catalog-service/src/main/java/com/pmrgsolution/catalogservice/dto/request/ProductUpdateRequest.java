package com.pmrgsolution.catalogservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.catalogservice.enums.ProductStatus;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductUpdateRequest(
        @Size(max = 255, message = "Product title must not exceed 255 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String title,

        @Size(max = 500, message = "Short description must not exceed 500 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String shortDescription,

        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String description,

        UUID categoryId,

        @Size(max = 100, message = "Brand must not exceed 100 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String brand,

        ProductStatus status,

        @Positive(message = "Base price must be greater than zero")
        BigDecimal basePrice,

        @Positive(message = "Sale price must be greater than zero")
        BigDecimal salePrice,

        List<String> tags
) {
}
