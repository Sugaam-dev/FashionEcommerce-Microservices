package com.pmrgsolution.paymentservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitManualUpiProofRequest {

    @NotBlank(message = "Order ID is required")
    @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
    private String orderId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "UTR number is required")
    @Size(max = 50, message = "UTR number must not exceed 50 characters")
    @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
    private String utrNumber;

    @NotBlank(message = "Proof image URL is required")
    @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
    private String proofImageUrl;
}
