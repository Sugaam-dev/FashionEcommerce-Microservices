package com.pmrgsolution.inventoryservice.controller;

import com.pmrgsolution.inventoryservice.dto.request.CreateInventoryItemRequest;
import com.pmrgsolution.inventoryservice.dto.request.StockAdjustmentRequest;
import com.pmrgsolution.inventoryservice.dto.response.InventoryItemResponse;
import com.pmrgsolution.inventoryservice.service.InventoryService;
import com.pmrgsolution.shared.core.response.ApiResponse;
import com.pmrgsolution.shared.core.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
@Tag(name = "Admin Inventory API", description = "Endpoints for managing inventory catalog, warehouse stock levels and stock adjustments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @Operation(summary = "Create a new inventory SKU tracking item")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> createInventoryItem(
            @Valid @RequestBody CreateInventoryItemRequest request
    ) {
        InventoryItemResponse response = inventoryService.createInventoryItem(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Inventory item created successfully", response));
    }

    @PatchMapping("/{sku}/stock")
    @Operation(summary = "Adjust inventory stock levels for a SKU")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> adjustStock(
            @PathVariable String sku,
            @Valid @RequestBody StockAdjustmentRequest request
    ) {
        InventoryItemResponse response = inventoryService.adjustStock(sku, request);
        return ResponseEntity.ok(ApiResponse.ok("Stock adjusted successfully", response));
    }

    @GetMapping("/{sku}")
    @Operation(summary = "Get inventory item details by SKU")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> getStockBySku(@PathVariable String sku) {
        InventoryItemResponse response = inventoryService.getStockBySku(sku);
        return ResponseEntity.ok(ApiResponse.ok("Inventory item fetched successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get paginated list of all inventory items")
    public ResponseEntity<ApiResponse<PageResponse<InventoryItemResponse>>> getAllInventory(
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<InventoryItemResponse> response = inventoryService.getAllInventory(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Inventory items fetched successfully", response));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get paginated list of items below low stock threshold")
    public ResponseEntity<ApiResponse<PageResponse<InventoryItemResponse>>> getLowStockItems(
            @PageableDefault(size = 20, sort = "availableQuantity", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<InventoryItemResponse> response = inventoryService.getLowStockItems(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Low stock items fetched successfully", response));
    }
}
