package com.pmrgsolution.inventoryservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReserveStockRequest(
        @NotBlank(message = "Order ID is required")
        @Size(max = 100, message = "Order ID must not exceed 100 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String orderId,

        @NotEmpty(message = "Items to reserve cannot be empty")
        List<@Valid StockItemReserveInfo> items
) {
}
