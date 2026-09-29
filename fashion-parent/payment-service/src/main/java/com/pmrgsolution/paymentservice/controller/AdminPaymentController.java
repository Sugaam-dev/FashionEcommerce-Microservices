package com.pmrgsolution.paymentservice.controller;

import com.pmrgsolution.paymentservice.domain.enums.PaymentMethod;
import com.pmrgsolution.paymentservice.domain.enums.PaymentStatus;
import com.pmrgsolution.paymentservice.dto.request.ReviewManualPaymentRequest;
import com.pmrgsolution.paymentservice.dto.request.UpdatePaymentSettingsRequest;
import com.pmrgsolution.paymentservice.dto.response.PaymentSettingsResponse;
import com.pmrgsolution.paymentservice.dto.response.PaymentTransactionResponse;
import com.pmrgsolution.paymentservice.service.AdminPaymentService;
import com.pmrgsolution.shared.core.response.ApiResponse;
import com.pmrgsolution.shared.core.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Payments", description = "Admin Payment Management & Manual Review APIs")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    @GetMapping("/transactions")
    @Operation(summary = "Get paginated payment transactions with optional status and method filters")
    public ResponseEntity<ApiResponse<PageResponse<PaymentTransactionResponse>>> getTransactions(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentMethod method,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<PaymentTransactionResponse> response = adminPaymentService.getTransactions(status, method, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Payment transactions retrieved successfully", response));
    }

    @GetMapping("/pending-manual")
    @Operation(summary = "Get all pending manual review payment transactions")
    public ResponseEntity<ApiResponse<PageResponse<PaymentTransactionResponse>>> getPendingManualTransactions(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<PaymentTransactionResponse> response = adminPaymentService.getPendingManualTransactions(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Pending manual payment transactions retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment transaction details by ID")
    public ResponseEntity<ApiResponse<PaymentTransactionResponse>> getTransactionById(
            @PathVariable UUID id
    ) {
        PaymentTransactionResponse response = adminPaymentService.getTransactionById(id);
        return ResponseEntity.ok(ApiResponse.ok("Payment transaction retrieved successfully", response));
    }

    @PostMapping("/{id}/review")
    @Operation(summary = "Review and approve/reject manual payment proof")
    public ResponseEntity<ApiResponse<PaymentTransactionResponse>> reviewManualPayment(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewManualPaymentRequest request
    ) {
        PaymentTransactionResponse response = adminPaymentService.reviewManualPayment(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Payment review processed successfully", response));
    }

    @GetMapping("/settings")
    @Operation(summary = "Get complete payment gateway and bank account settings")
    public ResponseEntity<ApiResponse<PaymentSettingsResponse>> getSettings() {
        PaymentSettingsResponse response = adminPaymentService.getSettings();
        return ResponseEntity.ok(ApiResponse.ok("Payment settings retrieved successfully", response));
    }

    @PutMapping("/settings")
    @Operation(summary = "Update payment gateway and bank account settings")
    public ResponseEntity<ApiResponse<PaymentSettingsResponse>> updateSettings(
            @Valid @RequestBody UpdatePaymentSettingsRequest request
    ) {
        PaymentSettingsResponse response = adminPaymentService.updateSettings(request);
        return ResponseEntity.ok(ApiResponse.ok("Payment settings updated successfully", response));
    }
}
