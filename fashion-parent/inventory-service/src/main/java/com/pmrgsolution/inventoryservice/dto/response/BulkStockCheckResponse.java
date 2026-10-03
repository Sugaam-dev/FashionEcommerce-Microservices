package com.pmrgsolution.inventoryservice.dto.response;

import java.util.Map;

public record BulkStockCheckResponse(
        Map<String, Integer> availableStockBySku
) {
}
