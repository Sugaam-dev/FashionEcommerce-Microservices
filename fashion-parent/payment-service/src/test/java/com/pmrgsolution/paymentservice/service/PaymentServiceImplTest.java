package com.pmrgsolution.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pmrgsolution.paymentservice.domain.entity.PaymentTransaction;
import com.pmrgsolution.paymentservice.domain.enums.PaymentMethod;
import com.pmrgsolution.paymentservice.domain.enums.PaymentStatus;
import com.pmrgsolution.paymentservice.dto.request.InitiateRazorpayRequest;
import com.pmrgsolution.paymentservice.dto.request.SubmitManualUpiProofRequest;
import com.pmrgsolution.paymentservice.dto.request.VerifyRazorpayPaymentRequest;
import com.pmrgsolution.paymentservice.dto.response.PaymentTransactionResponse;
import com.pmrgsolution.paymentservice.dto.response.RazorpayOrderResponse;
import com.pmrgsolution.paymentservice.exception.InvalidSignatureException;
import com.pmrgsolution.paymentservice.exception.PaymentAlreadyProcessedException;
import com.pmrgsolution.paymentservice.mapper.PaymentMapper;
import com.pmrgsolution.paymentservice.repository.PaymentGatewaySettingsRepository;
import com.pmrgsolution.paymentservice.repository.PaymentTransactionRepository;
import com.pmrgsolution.paymentservice.service.gateway.RazorpayIntegrationService;
import com.pmrgsolution.paymentservice.service.impl.PaymentServiceImpl;
import com.pmrgsolution.shared.core.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private PaymentGatewaySettingsRepository paymentGatewaySettingsRepository;

    @Mock
    private RazorpayIntegrationService razorpayIntegrationService;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @Mock
    private DistributedLockService distributedLockService;

    @Spy
    private PaymentMapper paymentMapper = new PaymentMapper();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID userId;
    private UUID paymentId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        ReflectionTestUtils.setField(paymentService, "defaultCurrency", "INR");
    }

    @Test
    @DisplayName("initiateRazorpayPayment creates gateway order and saves transaction")
    void initiateRazorpayPayment_success() {
        InitiateRazorpayRequest request = InitiateRazorpayRequest.builder()
                .orderId("ORD-1001")
                .amount(BigDecimal.valueOf(1499.00))
                .currency("INR")
                .build();

        when(paymentTransactionRepository.findByOrderId("ORD-1001")).thenReturn(Optional.empty());
        when(razorpayIntegrationService.createOrder(any(), any(), any())).thenReturn("order_rzp_123");
        when(razorpayIntegrationService.getKeyId()).thenReturn("rzp_test_public_key");

        PaymentTransaction savedTx = PaymentTransaction.builder()
                .id(paymentId)
                .orderId("ORD-1001")
                .userId(userId)
                .amount(BigDecimal.valueOf(1499.00))
                .currency("INR")
                .paymentMethod(PaymentMethod.RAZORPAY)
                .status(PaymentStatus.INITIATED)
                .gatewayOrderId("order_rzp_123")
                .build();
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenReturn(savedTx);

        RazorpayOrderResponse response = paymentService.initiateRazorpayPayment(userId, request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getRazorpayOrderId()).isEqualTo("order_rzp_123");
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1499.00));
        verify(outboxEventPublisher).saveEvent(eq("Payment"), eq(paymentId.toString()), eq("PAYMENT_INITIATED"), any());
    }

    @Test
    @DisplayName("initiateRazorpayPayment throws when order already verified")
    void initiateRazorpayPayment_whenAlreadyVerified_throwsPaymentAlreadyProcessedException() {
        InitiateRazorpayRequest request = InitiateRazorpayRequest.builder()
                .orderId("ORD-1001")
                .amount(BigDecimal.valueOf(1499.00))
                .build();

        PaymentTransaction verifiedTx = PaymentTransaction.builder()
                .id(paymentId)
                .orderId("ORD-1001")
                .userId(userId)
                .status(PaymentStatus.VERIFIED)
                .build();

        when(paymentTransactionRepository.findByOrderId("ORD-1001")).thenReturn(Optional.of(verifiedTx));

        assertThatThrownBy(() -> paymentService.initiateRazorpayPayment(userId, request, "127.0.0.1"))
                .isInstanceOf(PaymentAlreadyProcessedException.class);
    }

    @Test
    @DisplayName("verifyRazorpayPayment with valid signature updates status to VERIFIED")
    void verifyRazorpay_withValidSignature_updatesStatusVerified() {
        VerifyRazorpayPaymentRequest request = VerifyRazorpayPaymentRequest.builder()
                .paymentId(paymentId)
                .razorpayOrderId("order_rzp_123")
                .razorpayPaymentId("pay_rzp_999")
                .razorpaySignature("valid_sig_hash")
                .build();

        PaymentTransaction tx = PaymentTransaction.builder()
                .id(paymentId)
                .orderId("ORD-1001")
                .userId(userId)
                .amount(BigDecimal.valueOf(1499.00))
                .currency("INR")
                .status(PaymentStatus.INITIATED)
                .build();

        when(distributedLockService.get(anyString())).thenReturn(null);
        when(distributedLockService.acquireLock(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(paymentTransactionRepository.findById(paymentId)).thenReturn(Optional.of(tx));
        when(razorpayIntegrationService.verifySignature("order_rzp_123", "pay_rzp_999", "valid_sig_hash")).thenReturn(true);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));

        PaymentTransactionResponse response = paymentService.verifyRazorpayPayment(userId, request, "idem-1");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.VERIFIED);
        assertThat(response.getGatewayPaymentId()).isEqualTo("pay_rzp_999");
        verify(outboxEventPublisher).saveEvent(eq("Payment"), eq(paymentId.toString()), eq("PAYMENT_VERIFIED"), any());
    }

    @Test
    @DisplayName("verifyRazorpayPayment with invalid signature throws InvalidSignatureException")
    void verifyRazorpay_withInvalidSignature_throwsInvalidSignatureException() {
        VerifyRazorpayPaymentRequest request = VerifyRazorpayPaymentRequest.builder()
                .paymentId(paymentId)
                .razorpayOrderId("order_rzp_123")
                .razorpayPaymentId("pay_rzp_999")
                .razorpaySignature("invalid_sig")
                .build();

        PaymentTransaction tx = PaymentTransaction.builder()
                .id(paymentId)
                .orderId("ORD-1001")
                .userId(userId)
                .amount(BigDecimal.valueOf(1499.00))
                .status(PaymentStatus.INITIATED)
                .build();

        when(distributedLockService.get(anyString())).thenReturn(null);
        when(distributedLockService.acquireLock(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(paymentTransactionRepository.findById(paymentId)).thenReturn(Optional.of(tx));
        when(razorpayIntegrationService.verifySignature("order_rzp_123", "pay_rzp_999", "invalid_sig")).thenReturn(false);

        assertThatThrownBy(() -> paymentService.verifyRazorpayPayment(userId, request, "idem-1"))
                .isInstanceOf(InvalidSignatureException.class);

        assertThat(tx.getStatus()).isEqualTo(PaymentStatus.REJECTED);
        verify(outboxEventPublisher).saveEvent(eq("Payment"), eq(paymentId.toString()), eq("PAYMENT_REJECTED"), any());
    }

    @Test
    @DisplayName("verifyRazorpayPayment when other user attempts verification throws ForbiddenException (IDOR)")
    void verifyRazorpay_whenNotOwner_throwsForbiddenException() {
        VerifyRazorpayPaymentRequest request = VerifyRazorpayPaymentRequest.builder()
                .paymentId(paymentId)
                .razorpayOrderId("order_rzp_123")
                .razorpayPaymentId("pay_rzp_999")
                .razorpaySignature("valid_sig")
                .build();

        PaymentTransaction tx = PaymentTransaction.builder()
                .id(paymentId)
                .orderId("ORD-1001")
                .userId(UUID.randomUUID()) // different user
                .status(PaymentStatus.INITIATED)
                .build();

        when(distributedLockService.get(anyString())).thenReturn(null);
        when(distributedLockService.acquireLock(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(paymentTransactionRepository.findById(paymentId)).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> paymentService.verifyRazorpayPayment(userId, request, "idem-1"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("submitManualUpiProof saves transaction with PENDING_MANUAL_REVIEW and stages notification")
    void submitManualProof_savesTransactionAsPendingReview() {
        SubmitManualUpiProofRequest request = SubmitManualUpiProofRequest.builder()
                .orderId("ORD-2002")
                .amount(BigDecimal.valueOf(2500.00))
                .utrNumber("UTR123456789")
                .proofImageUrl("https://s3.aws.com/receipts/proof123.jpg")
                .build();

        when(paymentTransactionRepository.findByOrderId("ORD-2002")).thenReturn(Optional.empty());

        PaymentTransaction savedTx = PaymentTransaction.builder()
                .id(paymentId)
                .orderId("ORD-2002")
                .userId(userId)
                .amount(BigDecimal.valueOf(2500.00))
                .currency("INR")
                .paymentMethod(PaymentMethod.MANUAL_UPI_QR)
                .status(PaymentStatus.PENDING_MANUAL_REVIEW)
                .manualUpiUtrNumber("UTR123456789")
                .manualUpiProofImageUrl("https://s3.aws.com/receipts/proof123.jpg")
                .createdAt(Instant.now())
                .build();

        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenReturn(savedTx);

        PaymentTransactionResponse response = paymentService.submitManualUpiProof(userId, request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING_MANUAL_REVIEW);
        assertThat(response.getManualUpiUtrNumber()).isEqualTo("UTR123456789");
        verify(outboxEventPublisher).saveEvent(eq("Notification"), eq(paymentId.toString()), eq("NOTIFICATION_REQUEST"), any());
    }
}
