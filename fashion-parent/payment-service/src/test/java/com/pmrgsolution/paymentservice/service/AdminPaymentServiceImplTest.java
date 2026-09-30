package com.pmrgsolution.paymentservice.service;

import com.pmrgsolution.paymentservice.domain.entity.PaymentGatewaySettings;
import com.pmrgsolution.paymentservice.domain.entity.PaymentTransaction;
import com.pmrgsolution.paymentservice.domain.enums.PaymentMethod;
import com.pmrgsolution.paymentservice.domain.enums.PaymentStatus;
import com.pmrgsolution.paymentservice.dto.request.ReviewManualPaymentRequest;
import com.pmrgsolution.paymentservice.dto.request.UpdatePaymentSettingsRequest;
import com.pmrgsolution.paymentservice.dto.response.PaymentSettingsResponse;
import com.pmrgsolution.paymentservice.dto.response.PaymentTransactionResponse;
import com.pmrgsolution.paymentservice.mapper.PaymentMapper;
import com.pmrgsolution.paymentservice.repository.PaymentGatewaySettingsRepository;
import com.pmrgsolution.paymentservice.repository.PaymentTransactionRepository;
import com.pmrgsolution.paymentservice.service.impl.AdminPaymentServiceImpl;
import com.pmrgsolution.shared.core.exception.BadRequestException;
import com.pmrgsolution.shared.core.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPaymentServiceImplTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private PaymentGatewaySettingsRepository paymentGatewaySettingsRepository;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @Spy
    private PaymentMapper paymentMapper = new PaymentMapper();

    @InjectMocks
    private AdminPaymentServiceImpl adminPaymentService;

    private UUID transactionId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("getPendingManualTransactions returns paginated pending reviews")
    void getPendingManualTransactions_returnsPage() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .id(transactionId)
                .orderId("ORD-3003")
                .userId(userId)
                .amount(BigDecimal.valueOf(3999.00))
                .status(PaymentStatus.PENDING_MANUAL_REVIEW)
                .paymentMethod(PaymentMethod.MANUAL_UPI_QR)
                .manualUpiUtrNumber("UTR999888777")
                .build();

        Page<PaymentTransaction> page = new PageImpl<>(List.of(tx));
        when(paymentTransactionRepository.findByStatus(eq(PaymentStatus.PENDING_MANUAL_REVIEW), any())).thenReturn(page);

        PageResponse<PaymentTransactionResponse> response = adminPaymentService.getPendingManualTransactions(PageRequest.of(0, 10));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).getStatus()).isEqualTo(PaymentStatus.PENDING_MANUAL_REVIEW);
    }

    @Test
    @DisplayName("reviewManualPayment with VERIFIED approves transaction and stages Outbox event")
    void reviewManualPayment_approve_publishesVerifiedEvent() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .id(transactionId)
                .orderId("ORD-3003")
                .userId(userId)
                .amount(BigDecimal.valueOf(3999.00))
                .status(PaymentStatus.PENDING_MANUAL_REVIEW)
                .manualUpiUtrNumber("UTR999888777")
                .build();

        when(paymentTransactionRepository.findById(transactionId)).thenReturn(Optional.of(tx));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));

        ReviewManualPaymentRequest request = ReviewManualPaymentRequest.builder()
                .decision(PaymentStatus.VERIFIED)
                .build();

        PaymentTransactionResponse response = adminPaymentService.reviewManualPayment(transactionId, request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.VERIFIED);
        assertThat(response.getVerifiedAt()).isNotNull();
        verify(outboxEventPublisher).saveEvent(eq("Payment"), eq(transactionId.toString()), eq("PAYMENT_VERIFIED"), any());
    }

    @Test
    @DisplayName("reviewManualPayment with REJECTED marks transaction rejected and stages Outbox event")
    void reviewManualPayment_reject_publishesRejectedEvent() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .id(transactionId)
                .orderId("ORD-3003")
                .userId(userId)
                .amount(BigDecimal.valueOf(3999.00))
                .status(PaymentStatus.PENDING_MANUAL_REVIEW)
                .manualUpiUtrNumber("UTR999888777")
                .build();

        when(paymentTransactionRepository.findById(transactionId)).thenReturn(Optional.of(tx));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));

        ReviewManualPaymentRequest request = ReviewManualPaymentRequest.builder()
                .decision(PaymentStatus.REJECTED)
                .rejectionReason("Fake UTR receipt")
                .build();

        PaymentTransactionResponse response = adminPaymentService.reviewManualPayment(transactionId, request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(response.getFailureReason()).isEqualTo("Fake UTR receipt");
        verify(outboxEventPublisher).saveEvent(eq("Payment"), eq(transactionId.toString()), eq("PAYMENT_REJECTED"), any());
    }

    @Test
    @DisplayName("reviewManualPayment when not in PENDING_MANUAL_REVIEW throws BadRequestException")
    void reviewManualPayment_whenAlreadyVerified_throwsBadRequest() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .id(transactionId)
                .status(PaymentStatus.VERIFIED)
                .build();

        when(paymentTransactionRepository.findById(transactionId)).thenReturn(Optional.of(tx));

        ReviewManualPaymentRequest request = ReviewManualPaymentRequest.builder()
                .decision(PaymentStatus.VERIFIED)
                .build();

        assertThatThrownBy(() -> adminPaymentService.reviewManualPayment(transactionId, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("updateSettings updates settings successfully")
    void updateSettings_success() {
        PaymentGatewaySettings settings = PaymentGatewaySettings.builder()
                .id(UUID.randomUUID())
                .razorpayEnabled(true)
                .razorpayKeyId("rzp_old")
                .manualUpiEnabled(true)
                .upiId("old@upi")
                .build();

        when(paymentGatewaySettingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));
        when(paymentGatewaySettingsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdatePaymentSettingsRequest request = UpdatePaymentSettingsRequest.builder()
                .razorpayEnabled(true)
                .razorpayKeyId("rzp_new_key")
                .manualUpiEnabled(true)
                .upiId("new@upi")
                .bankName("State Bank")
                .bankAccountNumber("123456789012")
                .build();

        PaymentSettingsResponse response = adminPaymentService.updateSettings(request);

        assertThat(response.getRazorpayKeyId()).isEqualTo("rzp_new_key");
        assertThat(response.getUpiId()).isEqualTo("new@upi");
        assertThat(response.getBankName()).isEqualTo("State Bank");
        assertThat(response.getMaskedBankAccountNumber()).isEqualTo("******9012");
    }
}
