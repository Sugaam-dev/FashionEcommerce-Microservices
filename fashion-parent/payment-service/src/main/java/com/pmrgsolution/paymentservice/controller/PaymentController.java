package com.pmrgsolution.paymentservice.controller;

import com.pmrgsolution.paymentservice.dto.request.InitiateRazorpayRequest;
import com.pmrgsolution.paymentservice.dto.request.SubmitManualUpiProofRequest;
import com.pmrgsolution.paymentservice.dto.request.VerifyRazorpayPaymentRequest;
import com.pmrgsolution.paymentservice.dto.response.PaymentSettingsResponse;
import com.pmrgsolution.paymentservice.dto.response.PaymentTransactionResponse;
import com.pmrgsolution.paymentservice.dto.response.RazorpayOrderResponse;
import com.pmrgsolution.paymentservice.security.UserPrincipal;
import com.pmrgsolution.paymentservice.service.PaymentService;
import com.pmrgsolution.shared.core.response.ApiResponse;
import com.pmrgsolution.shared.core.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Customer Payment Gateway APIs")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/razorpay/initiate")
    @Operation(summary = "Initiate Razorpay payment order")
    public ResponseEntity<ApiResponse<RazorpayOrderResponse>> initiateRazorpay(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody InitiateRazorpayRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        RazorpayOrderResponse response = paymentService.initiateRazorpayPayment(principal.getId(), request, clientIp);
        return ResponseEntity.ok(ApiResponse.ok("Razorpay order initiated successfully", response));
    }

    @PostMapping("/razorpay/verify")
    @Operation(summary = "Verify Razorpay payment signature (Idempotent)")
    public ResponseEntity<ApiResponse<PaymentTransactionResponse>> verifyRazorpay(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VerifyRazorpayPaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        PaymentTransactionResponse response = paymentService.verifyRazorpayPayment(principal.getId(), request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok("Payment verified successfully", response));
    }

    @PostMapping("/manual-upi/submit-proof")
    @Operation(summary = "Submit manual UPI payment proof (UTR & Screenshot)")
    public ResponseEntity<ApiResponse<PaymentTransactionResponse>> submitManualUpiProof(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SubmitManualUpiProofRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        PaymentTransactionResponse response = paymentService.submitManualUpiProof(principal.getId(), request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Manual payment proof submitted successfully", response));
    }

    @GetMapping("/settings/public")
    @Operation(summary = "Get public payment configuration and bank info")
    public ResponseEntity<ApiResponse<PaymentSettingsResponse>> getPublicSettings() {
        PaymentSettingsResponse response = paymentService.getPublicPaymentSettings();
        return ResponseEntity.ok(ApiResponse.ok("Public payment settings retrieved successfully", response));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment status by Order ID")
    public ResponseEntity<ApiResponse<PaymentTransactionResponse>> getPaymentByOrderId(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderId
    ) {
        PaymentTransactionResponse response = paymentService.getPaymentByOrderId(principal.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.ok("Payment transaction retrieved successfully", response));
    }

    @GetMapping("/my-history")
    @Operation(summary = "Get current user's payment history")
    public ResponseEntity<ApiResponse<PageResponse<PaymentTransactionResponse>>> getMyPaymentHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<PaymentTransactionResponse> response = paymentService.getUserPaymentHistory(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok("Payment history retrieved successfully", response));
    }

    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
