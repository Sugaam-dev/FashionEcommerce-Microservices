package com.pmrgsolution.catalogservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductCreateRequest(
        @NotBlank(message = "Product title is required")
        @Size(max = 255, message = "Product title must not exceed 255 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String title,

        @NotBlank(message = "SKU prefix is required")
        @Size(max = 50, message = "SKU prefix must not exceed 50 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String skuPrefix,

        @Size(max = 500, message = "Short description must not exceed 500 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String shortDescription,

        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String description,

        @NotNull(message = "Category ID is required")
        UUID categoryId,

        @Size(max = 100, message = "Brand must not exceed 100 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String brand,

        @NotNull(message = "Base price is required")
        @Positive(message = "Base price must be greater than zero")
        BigDecimal basePrice,

        @Positive(message = "Sale price must be greater than zero")
        BigDecimal salePrice,

        List<String> tags,

        @NotEmpty(message = "At least one product variant is required")
        List<@Valid VariantCreateRequest> variants
) {
}
