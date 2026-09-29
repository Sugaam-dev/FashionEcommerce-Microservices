package com.pmrgsolution.paymentservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrgsolution.paymentservice.domain.entity.PaymentGatewaySettings;
import com.pmrgsolution.paymentservice.domain.entity.PaymentTransaction;
import com.pmrgsolution.paymentservice.domain.enums.PaymentMethod;
import com.pmrgsolution.paymentservice.domain.enums.PaymentStatus;
import com.pmrgsolution.paymentservice.dto.request.InitiateRazorpayRequest;
import com.pmrgsolution.paymentservice.dto.request.SubmitManualUpiProofRequest;
import com.pmrgsolution.paymentservice.dto.request.VerifyRazorpayPaymentRequest;
import com.pmrgsolution.paymentservice.dto.response.PaymentSettingsResponse;
import com.pmrgsolution.paymentservice.dto.response.PaymentTransactionResponse;
import com.pmrgsolution.paymentservice.dto.response.RazorpayOrderResponse;
import com.pmrgsolution.paymentservice.exception.InvalidSignatureException;
import com.pmrgsolution.paymentservice.exception.PaymentAlreadyProcessedException;
import com.pmrgsolution.paymentservice.exception.PaymentNotFoundException;
import com.pmrgsolution.paymentservice.mapper.PaymentMapper;
import com.pmrgsolution.paymentservice.repository.PaymentGatewaySettingsRepository;
import com.pmrgsolution.paymentservice.repository.PaymentTransactionRepository;
import com.pmrgsolution.paymentservice.service.DistributedLockService;
import com.pmrgsolution.paymentservice.service.OutboxEventPublisher;
import com.pmrgsolution.paymentservice.service.PaymentService;
import com.pmrgsolution.paymentservice.service.gateway.RazorpayIntegrationService;
import com.pmrgsolution.shared.core.exception.BadRequestException;
import com.pmrgsolution.shared.core.exception.ForbiddenException;
import com.pmrgsolution.shared.core.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentGatewaySettingsRepository paymentGatewaySettingsRepository;
    private final RazorpayIntegrationService razorpayIntegrationService;
    private final OutboxEventPublisher outboxEventPublisher;
    private final DistributedLockService distributedLockService;
    private final PaymentMapper paymentMapper;
    private final ObjectMapper objectMapper;

    @Value("${app.defaults.currency:INR}")
    private String defaultCurrency;

    @Override
    @Transactional
    public RazorpayOrderResponse initiateRazorpayPayment(UUID userId, InitiateRazorpayRequest req, String clientIp) {
        log.info("Initiating Razorpay payment for user: {}, orderId: {}, amount: {}", userId, req.getOrderId(), req.getAmount());

        // Check if a payment for this order is already VERIFIED
        Optional<PaymentTransaction> existingTx = paymentTransactionRepository.findByOrderId(req.getOrderId());
        if (existingTx.isPresent() && existingTx.get().getStatus() == PaymentStatus.VERIFIED) {
            throw new PaymentAlreadyProcessedException("Payment for order " + req.getOrderId() + " is already verified");
        }

        String currency = req.getCurrency() != null && !req.getCurrency().isBlank() ? req.getCurrency() : defaultCurrency;

        // Call Razorpay API to create gateway order
        String razorpayOrderId = razorpayIntegrationService.createOrder(req.getAmount(), currency, req.getOrderId());

        PaymentTransaction tx;
        if (existingTx.isPresent()) {
            tx = existingTx.get();
            tx.setAmount(req.getAmount());
            tx.setCurrency(currency);
            tx.setPaymentMethod(PaymentMethod.RAZORPAY);
            tx.setStatus(PaymentStatus.INITIATED);
            tx.setGatewayOrderId(razorpayOrderId);
            tx.setClientIp(clientIp);
        } else {
            tx = PaymentTransaction.builder()
                    .orderId(req.getOrderId())
                    .userId(userId)
                    .amount(req.getAmount())
                    .currency(currency)
                    .paymentMethod(PaymentMethod.RAZORPAY)
                    .status(PaymentStatus.INITIATED)
                    .gatewayOrderId(razorpayOrderId)
                    .clientIp(clientIp)
                    .build();
        }

        PaymentTransaction savedTx = paymentTransactionRepository.save(tx);

        // Stage Outbox Event: PAYMENT_INITIATED
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("paymentId", savedTx.getId());
        eventData.put("orderId", savedTx.getOrderId());
        eventData.put("userId", savedTx.getUserId());
        eventData.put("amount", savedTx.getAmount());
        eventData.put("currency", savedTx.getCurrency());
        eventData.put("gatewayOrderId", savedTx.getGatewayOrderId());
        eventData.put("timestamp", Instant.now());

        outboxEventPublisher.saveEvent("Payment", savedTx.getId().toString(), "PAYMENT_INITIATED", eventData);

        return RazorpayOrderResponse.builder()
                .paymentId(savedTx.getId())
                .razorpayOrderId(razorpayOrderId)
                .amount(savedTx.getAmount())
                .currency(savedTx.getCurrency())
                .keyId(razorpayIntegrationService.getKeyId())
                .build();
    }

    @Override
    @Transactional
    public PaymentTransactionResponse verifyRazorpayPayment(UUID userId, VerifyRazorpayPaymentRequest req, String idempotencyKey) {
        log.info("Verifying Razorpay payment: {} for user: {}", req.getPaymentId(), userId);

        String idempKey = idempotencyKey != null && !idempotencyKey.isBlank() ? idempotencyKey : req.getRazorpayPaymentId();
        String redisIdempKey = "idempotency:payment:verify:" + idempKey;

        // Check Redis idempotency cache
        String cachedResult = distributedLockService.get(redisIdempKey);
        if (cachedResult != null) {
            log.info("Idempotent hit for payment verification key: {}", idempKey);
            try {
                return objectMapper.readValue(cachedResult, PaymentTransactionResponse.class);
            } catch (Exception e) {
                log.warn("Failed to parse cached payment response: {}", e.getMessage());
            }
        }

        String lockKey = "lock:payment:" + req.getPaymentId();
        String lockValue = UUID.randomUUID().toString();
        boolean locked = distributedLockService.acquireLock(lockKey, lockValue, Duration.ofSeconds(15));
        if (!locked) {
            throw new BadRequestException("Payment verification is already in progress. Please retry shortly.");
        }

        try {
            PaymentTransaction tx = paymentTransactionRepository.findById(req.getPaymentId())
                    .orElseThrow(() -> new PaymentNotFoundException("Payment transaction not found with ID: " + req.getPaymentId()));

            // IDOR Protection: verify user ownership
            if (!tx.getUserId().equals(userId)) {
                throw new ForbiddenException("You are not authorized to verify this payment transaction");
            }

            if (tx.getStatus() == PaymentStatus.VERIFIED) {
                log.info("Payment transaction {} is already verified", tx.getId());
                PaymentTransactionResponse resp = paymentMapper.toResponse(tx);
                cacheResponse(redisIdempKey, resp);
                return resp;
            }

            // Verify signature using constant-time comparison
            boolean isValid = razorpayIntegrationService.verifySignature(
                    req.getRazorpayOrderId(),
                    req.getRazorpayPaymentId(),
                    req.getRazorpaySignature()
            );

            if (!isValid) {
                log.warn("Invalid Razorpay signature for payment ID: {}", tx.getId());
                tx.setStatus(PaymentStatus.REJECTED);
                tx.setFailureReason("Invalid gateway signature verification");
                paymentTransactionRepository.save(tx);

                // Publish PAYMENT_REJECTED event
                Map<String, Object> rejectEvent = new HashMap<>();
                rejectEvent.put("eventId", UUID.randomUUID().toString());
                rejectEvent.put("occurredAt", Instant.now());
                rejectEvent.put("paymentId", tx.getId().toString());
                rejectEvent.put("entityType", "ORDER");
                rejectEvent.put("entityId", tx.getOrderId());
                rejectEvent.put("orderId", tx.getOrderId());
                rejectEvent.put("userId", tx.getUserId());
                rejectEvent.put("rejectedBy", "GATEWAY");
                rejectEvent.put("remarks", "Invalid signature");
                rejectEvent.put("reason", "Invalid signature");
                rejectEvent.put("timestamp", Instant.now());
                outboxEventPublisher.saveEvent("Payment", tx.getId().toString(), "PAYMENT_REJECTED", rejectEvent);

                throw new InvalidSignatureException("Payment verification failed: invalid signature");
            }

            // Update transaction to VERIFIED
            tx.setStatus(PaymentStatus.VERIFIED);
            tx.setGatewayPaymentId(req.getRazorpayPaymentId());
            tx.setGatewaySignature(req.getRazorpaySignature());
            tx.setVerifiedAt(Instant.now());
            PaymentTransaction savedTx = paymentTransactionRepository.save(tx);

            // Stage Outbox Event: PAYMENT_VERIFIED
            Map<String, Object> verifiedEvent = new HashMap<>();
            verifiedEvent.put("eventId", UUID.randomUUID().toString());
            verifiedEvent.put("occurredAt", savedTx.getVerifiedAt());
            verifiedEvent.put("paymentId", savedTx.getId().toString());
            verifiedEvent.put("entityType", "ORDER");
            verifiedEvent.put("entityId", savedTx.getOrderId());
            verifiedEvent.put("orderId", savedTx.getOrderId());
            verifiedEvent.put("userId", savedTx.getUserId());
            verifiedEvent.put("amount", savedTx.getAmount());
            verifiedEvent.put("gatewayPaymentId", savedTx.getGatewayPaymentId());
            verifiedEvent.put("verifiedBy", "RAZORPAY");
            verifiedEvent.put("timestamp", savedTx.getVerifiedAt());
            outboxEventPublisher.saveEvent("Payment", savedTx.getId().toString(), "PAYMENT_VERIFIED", verifiedEvent);

            PaymentTransactionResponse response = paymentMapper.toResponse(savedTx);
            cacheResponse(redisIdempKey, response);
            return response;
        } finally {
            distributedLockService.releaseLock(lockKey, lockValue);
        }
    }

    @Override
    @Transactional
    public PaymentTransactionResponse submitManualUpiProof(UUID userId, SubmitManualUpiProofRequest req, String clientIp) {
        log.info("Submitting manual UPI proof for order: {}, UTR: {} by user: {}", req.getOrderId(), req.getUtrNumber(), userId);

        Optional<PaymentTransaction> existingTx = paymentTransactionRepository.findByOrderId(req.getOrderId());
        if (existingTx.isPresent() && existingTx.get().getStatus() == PaymentStatus.VERIFIED) {
            throw new PaymentAlreadyProcessedException("Payment for order " + req.getOrderId() + " is already verified");
        }

        PaymentTransaction tx;
        if (existingTx.isPresent()) {
            tx = existingTx.get();
            tx.setAmount(req.getAmount());
            tx.setCurrency(defaultCurrency);
            tx.setPaymentMethod(PaymentMethod.MANUAL_UPI_QR);
            tx.setStatus(PaymentStatus.PENDING_MANUAL_REVIEW);
            tx.setManualUpiUtrNumber(req.getUtrNumber());
            tx.setManualUpiProofImageUrl(req.getProofImageUrl());
            tx.setClientIp(clientIp);
        } else {
            tx = PaymentTransaction.builder()
                    .orderId(req.getOrderId())
                    .userId(userId)
                    .amount(req.getAmount())
                    .currency(defaultCurrency)
                    .paymentMethod(PaymentMethod.MANUAL_UPI_QR)
                    .status(PaymentStatus.PENDING_MANUAL_REVIEW)
                    .manualUpiUtrNumber(req.getUtrNumber())
                    .manualUpiProofImageUrl(req.getProofImageUrl())
                    .clientIp(clientIp)
                    .build();
        }

        PaymentTransaction savedTx = paymentTransactionRepository.save(tx);

        // Stage notification event for admin alert
        Map<String, Object> adminAlert = new HashMap<>();
        adminAlert.put("templateId", "manual-payment-alert");
        adminAlert.put("orderId", savedTx.getOrderId());
        adminAlert.put("userId", savedTx.getUserId());
        adminAlert.put("amount", savedTx.getAmount());
        adminAlert.put("utrNumber", savedTx.getManualUpiUtrNumber());
        adminAlert.put("timestamp", Instant.now());
        outboxEventPublisher.saveEvent("Notification", savedTx.getId().toString(), "NOTIFICATION_REQUEST", adminAlert);

        return paymentMapper.toResponse(savedTx);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentSettingsResponse getPublicPaymentSettings() {
        PaymentGatewaySettings settings = paymentGatewaySettingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> paymentGatewaySettingsRepository.save(PaymentGatewaySettings.builder().build()));
        return paymentMapper.toSettingsResponse(settings);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentTransactionResponse getPaymentByOrderId(UUID userId, String orderId) {
        PaymentTransaction tx = paymentTransactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment transaction not found for order: " + orderId));

        if (!tx.getUserId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to view payment details for this order");
        }

        return paymentMapper.toResponse(tx);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentTransactionResponse> getUserPaymentHistory(UUID userId, Pageable pageable) {
        Page<PaymentTransaction> page = paymentTransactionRepository.findByUserId(userId, pageable);
        return PageResponse.from(page.map(paymentMapper::toResponse));
    }

    private void cacheResponse(String key, PaymentTransactionResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            distributedLockService.set(key, json, Duration.ofHours(1));
        } catch (Exception e) {
            log.warn("Failed to cache payment response in Redis: {}", e.getMessage());
        }
    }
}
