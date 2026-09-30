package com.pmrgsolution.paymentservice.service;

import com.pmrgsolution.paymentservice.domain.enums.PaymentMethod;
import com.pmrgsolution.paymentservice.domain.enums.PaymentStatus;
import com.pmrgsolution.paymentservice.dto.request.ReviewManualPaymentRequest;
import com.pmrgsolution.paymentservice.dto.request.UpdatePaymentSettingsRequest;
import com.pmrgsolution.paymentservice.dto.response.PaymentSettingsResponse;
import com.pmrgsolution.paymentservice.dto.response.PaymentTransactionResponse;
import com.pmrgsolution.shared.core.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminPaymentService {

    PageResponse<PaymentTransactionResponse> getTransactions(PaymentStatus status, PaymentMethod method, Pageable pageable);

    PageResponse<PaymentTransactionResponse> getPendingManualTransactions(Pageable pageable);

    PaymentTransactionResponse getTransactionById(UUID transactionId);

    PaymentTransactionResponse reviewManualPayment(UUID transactionId, ReviewManualPaymentRequest req);

    PaymentSettingsResponse getSettings();

    PaymentSettingsResponse updateSettings(UpdatePaymentSettingsRequest req);
}
