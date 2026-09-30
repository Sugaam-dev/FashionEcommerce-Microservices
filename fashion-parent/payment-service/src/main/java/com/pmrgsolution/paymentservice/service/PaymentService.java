package com.pmrgsolution.paymentservice.service;

import com.pmrgsolution.paymentservice.dto.request.InitiateRazorpayRequest;
import com.pmrgsolution.paymentservice.dto.request.SubmitManualUpiProofRequest;
import com.pmrgsolution.paymentservice.dto.request.VerifyRazorpayPaymentRequest;
import com.pmrgsolution.paymentservice.dto.response.PaymentSettingsResponse;
import com.pmrgsolution.paymentservice.dto.response.PaymentTransactionResponse;
import com.pmrgsolution.paymentservice.dto.response.RazorpayOrderResponse;
import com.pmrgsolution.shared.core.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PaymentService {

    RazorpayOrderResponse initiateRazorpayPayment(UUID userId, InitiateRazorpayRequest req, String clientIp);

    PaymentTransactionResponse verifyRazorpayPayment(UUID userId, VerifyRazorpayPaymentRequest req, String idempotencyKey);

    PaymentTransactionResponse submitManualUpiProof(UUID userId, SubmitManualUpiProofRequest req, String clientIp);

    PaymentSettingsResponse getPublicPaymentSettings();

    PaymentTransactionResponse getPaymentByOrderId(UUID userId, String orderId);

    PageResponse<PaymentTransactionResponse> getUserPaymentHistory(UUID userId, Pageable pageable);
}
