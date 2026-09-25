package com.pmrgsolution.orderservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AddToCartRequest(
        @NotBlank(message = "SKU is required")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String sku,

        @NotBlank(message = "Product ID is required")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String productId,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity
) {}
