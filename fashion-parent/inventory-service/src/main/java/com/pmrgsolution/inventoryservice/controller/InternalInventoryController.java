package com.pmrgsolution.inventoryservice.controller;

import com.pmrgsolution.inventoryservice.dto.request.BulkStockCheckRequest;
import com.pmrgsolution.inventoryservice.dto.request.ConfirmStockRequest;
import com.pmrgsolution.inventoryservice.dto.request.ReleaseStockRequest;
import com.pmrgsolution.inventoryservice.dto.request.ReserveStockRequest;
import com.pmrgsolution.inventoryservice.dto.response.BulkStockCheckResponse;
import com.pmrgsolution.inventoryservice.dto.response.InventoryItemResponse;
import com.pmrgsolution.inventoryservice.dto.response.ReservationResponse;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/inventory")
@RequiredArgsConstructor
@Tag(name = "Internal Inventory API", description = "Internal microservice endpoints for stock checks, atomic reservations, confirmations and releases")
public class InternalInventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{sku}")
    @Operation(summary = "Internal query: Get stock details by SKU")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> getStockBySku(@PathVariable String sku) {
        InventoryItemResponse response = inventoryService.getStockBySku(sku);
        return ResponseEntity.ok(ApiResponse.ok("Inventory item fetched successfully", response));
    }

    @PostMapping("/bulk-check")
    @Operation(summary = "Internal query: Bulk check stock availability for a list of SKUs")
    public ResponseEntity<ApiResponse<BulkStockCheckResponse>> checkStock(
            @Valid @RequestBody BulkStockCheckRequest request
    ) {
        BulkStockCheckResponse response = inventoryService.checkStock(request);
        return ResponseEntity.ok(ApiResponse.ok("Stock check completed", response));
    }

    @PostMapping("/reserve")
    @Operation(summary = "Internal command: Atomically reserve stock for an order with distributed locking & idempotency")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveStock(
            @Valid @RequestBody ReserveStockRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        ReservationResponse response = inventoryService.reserveStock(request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok("Stock reserved successfully", response));
    }

    @PostMapping("/confirm")
    @Operation(summary = "Internal command: Confirm stock reservation upon successful order payment")
    public ResponseEntity<ApiResponse<ReservationResponse>> confirmStock(
            @Valid @RequestBody ConfirmStockRequest request
    ) {
        ReservationResponse response = inventoryService.confirmStock(request);
        return ResponseEntity.ok(ApiResponse.ok("Stock confirmed successfully", response));
    }

    @PostMapping("/release")
    @Operation(summary = "Internal command: Release stock reservation back to inventory upon order cancellation/failure")
    public ResponseEntity<ApiResponse<ReservationResponse>> releaseStock(
            @Valid @RequestBody ReleaseStockRequest request
    ) {
        ReservationResponse response = inventoryService.releaseStock(request);
        return ResponseEntity.ok(ApiResponse.ok("Stock released successfully", response));
    }
}
