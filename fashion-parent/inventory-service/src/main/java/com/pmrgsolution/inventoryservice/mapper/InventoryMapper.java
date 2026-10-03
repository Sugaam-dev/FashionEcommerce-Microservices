package com.pmrgsolution.inventoryservice.mapper;

import com.pmrgsolution.inventoryservice.dto.request.CreateInventoryItemRequest;
import com.pmrgsolution.inventoryservice.dto.response.InventoryItemResponse;
import com.pmrgsolution.inventoryservice.entity.InventoryItem;

public final class InventoryMapper {

    private InventoryMapper() {
    }

    public static InventoryItem toEntity(CreateInventoryItemRequest request) {
        if (request == null) {
            return null;
        }
        return InventoryItem.builder()
                .sku(request.sku().trim())
                .productId(request.productId().trim())
                .productName(request.productName().trim())
                .availableQuantity(request.initialQuantity())
                .reservedQuantity(0)
                .lowStockThreshold(request.lowStockThreshold())
                .warehouseCode(request.warehouseCode() != null && !request.warehouseCode().isBlank()
                        ? request.warehouseCode().trim()
                        : "WH-MAIN")
                .build();
    }

    public static InventoryItemResponse toResponse(InventoryItem item) {
        if (item == null) {
            return null;
        }
        return new InventoryItemResponse(
                item.getId(),
                item.getSku(),
                item.getProductId(),
                item.getProductName(),
                item.getAvailableQuantity(),
                item.getReservedQuantity(),
                item.getTotalQuantity(),
                item.getLowStockThreshold(),
                item.isLowStock(),
                item.getWarehouseCode(),
                item.getUpdatedAt()
        );
    }
}
