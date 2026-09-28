package com.pmrgsolution.paymentservice.dto.response;

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
public class PaymentSettingsResponse {
    private UUID id;
    private boolean razorpayEnabled;
    private String razorpayKeyId;
    private boolean manualUpiEnabled;
    private String upiId;
    private String upiAccountHolder;
    private String upiQrCodeUrl;
    private String bankName;
    private String maskedBankAccountNumber;
    private String bankIfscCode;
    private String bankBranch;
}
