package com.pmrgsolution.inventoryservice.dto.response;

import java.time.Instant;
import java.util.UUID;

public record InventoryItemResponse(
        UUID id,
        String sku,
        String productId,
        String productName,
        int availableQuantity,
        int reservedQuantity,
        int totalQuantity,
        int lowStockThreshold,
        boolean isLowStock,
        String warehouseCode,
        Instant updatedAt
) {
}
