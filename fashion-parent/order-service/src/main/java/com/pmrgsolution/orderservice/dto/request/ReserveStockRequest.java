package com.pmrgsolution.orderservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReserveStockRequest(
        @NotEmpty List<@Valid ReserveStockItemRequest> items,
        @NotBlank String orderId
) {}
