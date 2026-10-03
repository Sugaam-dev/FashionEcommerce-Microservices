package com.pmrgsolution.inventoryservice.controller;

import com.pmrgsolution.inventoryservice.dto.request.BulkStockCheckRequest;
import com.pmrgsolution.inventoryservice.dto.response.BulkStockCheckResponse;
import com.pmrgsolution.inventoryservice.dto.response.InventoryItemResponse;
import com.pmrgsolution.inventoryservice.service.InventoryService;
import com.pmrgsolution.shared.core.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Customer Inventory API", description = "Endpoints for public stock checking and availability")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{sku}")
    @Operation(summary = "Check availability and stock level for a SKU")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> getStockBySku(@PathVariable String sku) {
        InventoryItemResponse response = inventoryService.getStockBySku(sku);
        return ResponseEntity.ok(ApiResponse.ok("Inventory item fetched successfully", response));
    }

    @PostMapping("/bulk-check")
    @Operation(summary = "Check availability for multiple SKUs")
    public ResponseEntity<ApiResponse<BulkStockCheckResponse>> checkStock(
            @Valid @RequestBody BulkStockCheckRequest request
    ) {
        BulkStockCheckResponse response = inventoryService.checkStock(request);
        return ResponseEntity.ok(ApiResponse.ok("Stock check completed", response));
    }
}
