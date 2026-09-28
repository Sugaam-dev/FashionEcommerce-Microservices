package com.pmrgsolution.paymentservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.paymentservice.domain.enums.PaymentStatus;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.NotNull;
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
public class ReviewManualPaymentRequest {

    @NotNull(message = "Decision is required")
    private PaymentStatus decision;

    @Size(max = 500, message = "Rejection reason must not exceed 500 characters")
    @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
    private String rejectionReason;
}
