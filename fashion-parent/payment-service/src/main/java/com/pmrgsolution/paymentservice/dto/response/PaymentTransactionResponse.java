package com.pmrgsolution.paymentservice.dto.response;

import com.pmrgsolution.paymentservice.domain.enums.PaymentMethod;
import com.pmrgsolution.paymentservice.domain.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransactionResponse {
    private UUID id;
    private String orderId;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String gatewayOrderId;
    private String gatewayPaymentId;
    private String manualUpiUtrNumber;
    private String maskedProofUrl;
    private String failureReason;
    private Instant verifiedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
