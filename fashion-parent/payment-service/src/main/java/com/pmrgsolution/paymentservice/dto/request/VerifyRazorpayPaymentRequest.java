package com.pmrgsolution.paymentservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyRazorpayPaymentRequest {

    @NotNull(message = "Payment ID is required")
    private UUID paymentId;

    @NotBlank(message = "Razorpay Order ID is required")
    @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay Payment ID is required")
    @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay Signature is required")
    @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
    private String razorpaySignature;
}
