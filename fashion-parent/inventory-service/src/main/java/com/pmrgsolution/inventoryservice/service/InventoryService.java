package com.pmrgsolution.inventoryservice.service;

import com.pmrgsolution.inventoryservice.dto.request.BulkStockCheckRequest;
import com.pmrgsolution.inventoryservice.dto.request.ConfirmStockRequest;
import com.pmrgsolution.inventoryservice.dto.request.CreateInventoryItemRequest;
import com.pmrgsolution.inventoryservice.dto.request.ReleaseStockRequest;
import com.pmrgsolution.inventoryservice.dto.request.ReserveStockRequest;
import com.pmrgsolution.inventoryservice.dto.request.StockAdjustmentRequest;
import com.pmrgsolution.inventoryservice.dto.response.BulkStockCheckResponse;
import com.pmrgsolution.inventoryservice.dto.response.InventoryItemResponse;
import com.pmrgsolution.inventoryservice.dto.response.ReservationResponse;
import com.pmrgsolution.shared.core.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface InventoryService {

    InventoryItemResponse createInventoryItem(CreateInventoryItemRequest request);

    InventoryItemResponse adjustStock(String sku, StockAdjustmentRequest request);

    InventoryItemResponse getStockBySku(String sku);

    BulkStockCheckResponse checkStock(BulkStockCheckRequest request);

    ReservationResponse reserveStock(ReserveStockRequest request, String idempotencyKey);

    ReservationResponse confirmStock(ConfirmStockRequest request);

    ReservationResponse releaseStock(ReleaseStockRequest request);

    PageResponse<InventoryItemResponse> getAllInventory(Pageable pageable);

    PageResponse<InventoryItemResponse> getLowStockItems(Pageable pageable);
}
