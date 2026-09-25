package com.pmrgsolution.orderservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.NotBlank;

public record AddToWishlistRequest(
        @NotBlank(message = "Product ID is required")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String productId
) {}
