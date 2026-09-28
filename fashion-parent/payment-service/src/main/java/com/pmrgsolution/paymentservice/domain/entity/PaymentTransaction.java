package com.pmrgsolution.paymentservice.domain.entity;

import com.pmrgsolution.paymentservice.domain.enums.PaymentMethod;
import com.pmrgsolution.paymentservice.domain.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payment_transactions",
        indexes = {
                @Index(name = "idx_tx_order", columnList = "order_id"),
                @Index(name = "idx_tx_status", columnList = "status"),
                @Index(name = "idx_tx_gateway_ref", columnList = "gateway_payment_id"),
                @Index(name = "idx_tx_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", length = 100, nullable = false)
    private String orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 10, nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50, nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Column(name = "gateway_order_id", length = 100)
    private String gatewayOrderId;

    @Column(name = "gateway_payment_id", length = 100)
    private String gatewayPaymentId;

    @Column(name = "gateway_signature", length = 255)
    private String gatewaySignature;

    @Column(name = "manual_upi_utr_number", length = 50)
    private String manualUpiUtrNumber;

    @Column(name = "manual_upi_proof_image_url", length = 500)
    private String manualUpiProofImageUrl;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaymentTransaction that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
