package com.pmrgsolution.paymentservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePaymentSettingsRequest {

    private boolean razorpayEnabled;

    @Size(max = 100, message = "Razorpay key ID must not exceed 100 characters")
    @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
    private String razorpayKeyId;

    private boolean manualUpiEnabled;

    @Size(max = 100, message = "UPI ID must not exceed 100 characters")
    @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
    private String upiId;

    @Size(max = 100, message = "UPI account holder must not exceed 100 characters")
    @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
    private String upiAccountHolder;

    @Size(max = 500, message = "UPI QR Code URL must not exceed 500 characters")
    @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
    private String upiQrCodeUrl;

    @Size(max = 100, message = "Bank name must not exceed 100 characters")
    @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
    private String bankName;

    @Size(max = 100, message = "Bank account number must not exceed 100 characters")
    @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
    private String bankAccountNumber;

    @Size(max = 20, message = "Bank IFSC code must not exceed 20 characters")
    @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
    private String bankIfscCode;

    @Size(max = 100, message = "Bank branch must not exceed 100 characters")
    @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
    private String bankBranch;
}
