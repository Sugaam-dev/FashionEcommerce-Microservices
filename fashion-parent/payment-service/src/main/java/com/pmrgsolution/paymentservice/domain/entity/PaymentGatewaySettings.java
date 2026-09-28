package com.pmrgsolution.paymentservice.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_gateway_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentGatewaySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "razorpay_enabled", nullable = false)
    @Builder.Default
    private boolean razorpayEnabled = true;

    @Column(name = "razorpay_key_id", length = 100)
    private String razorpayKeyId;

    @Column(name = "manual_upi_enabled", nullable = false)
    @Builder.Default
    private boolean manualUpiEnabled = true;

    @Column(name = "upi_id", length = 100)
    private String upiId;

    @Column(name = "upi_account_holder", length = 100)
    private String upiAccountHolder;

    @Column(name = "upi_qr_code_url", length = 500)
    private String upiQrCodeUrl;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_number", length = 100)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc_code", length = 20)
    private String bankIfscCode;

    @Column(name = "bank_branch", length = 100)
    private String bankBranch;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaymentGatewaySettings that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
