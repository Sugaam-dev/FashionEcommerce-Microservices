package com.pmrgsolution.paymentservice.mapper;

import com.pmrgsolution.paymentservice.domain.entity.PaymentGatewaySettings;
import com.pmrgsolution.paymentservice.domain.entity.PaymentTransaction;
import com.pmrgsolution.paymentservice.dto.response.PaymentSettingsResponse;
import com.pmrgsolution.paymentservice.dto.response.PaymentTransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentTransactionResponse toResponse(PaymentTransaction tx) {
        if (tx == null) {
            return null;
        }

        return PaymentTransactionResponse.builder()
                .id(tx.getId())
                .orderId(tx.getOrderId())
                .userId(tx.getUserId())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .paymentMethod(tx.getPaymentMethod())
                .status(tx.getStatus())
                .gatewayOrderId(tx.getGatewayOrderId())
                .gatewayPaymentId(tx.getGatewayPaymentId())
                .manualUpiUtrNumber(tx.getManualUpiUtrNumber())
                .maskedProofUrl(maskProofUrl(tx.getManualUpiProofImageUrl()))
                .failureReason(tx.getFailureReason())
                .verifiedAt(tx.getVerifiedAt())
                .createdAt(tx.getCreatedAt())
                .updatedAt(tx.getUpdatedAt())
                .build();
    }

    public PaymentSettingsResponse toSettingsResponse(PaymentGatewaySettings settings) {
        if (settings == null) {
            return null;
        }

        return PaymentSettingsResponse.builder()
                .id(settings.getId())
                .razorpayEnabled(settings.isRazorpayEnabled())
                .razorpayKeyId(settings.getRazorpayKeyId())
                .manualUpiEnabled(settings.isManualUpiEnabled())
                .upiId(settings.getUpiId())
                .upiAccountHolder(settings.getUpiAccountHolder())
                .upiQrCodeUrl(settings.getUpiQrCodeUrl())
                .bankName(settings.getBankName())
                .maskedBankAccountNumber(maskAccountNumber(settings.getBankAccountNumber()))
                .bankIfscCode(settings.getBankIfscCode())
                .bankBranch(settings.getBankBranch())
                .build();
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return null;
        }
        String trimmed = accountNumber.trim();
        if (trimmed.length() <= 4) {
            return "******" + trimmed;
        }
        return "******" + trimmed.substring(trimmed.length() - 4);
    }

    private String maskProofUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash != -1 && lastSlash < url.length() - 1) {
            String filename = url.substring(lastSlash + 1);
            if (filename.length() > 8) {
                return url.substring(0, lastSlash + 1) + filename.substring(0, 4) + "***" + filename.substring(filename.length() - 4);
            }
        }
        return url;
    }
}
