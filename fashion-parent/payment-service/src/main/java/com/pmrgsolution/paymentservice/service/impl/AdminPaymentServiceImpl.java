package com.pmrgsolution.paymentservice.service.impl;

import com.pmrgsolution.paymentservice.domain.entity.PaymentGatewaySettings;
import com.pmrgsolution.paymentservice.domain.entity.PaymentTransaction;
import com.pmrgsolution.paymentservice.domain.enums.PaymentMethod;
import com.pmrgsolution.paymentservice.domain.enums.PaymentStatus;
import com.pmrgsolution.paymentservice.dto.request.ReviewManualPaymentRequest;
import com.pmrgsolution.paymentservice.dto.request.UpdatePaymentSettingsRequest;
import com.pmrgsolution.paymentservice.dto.response.PaymentSettingsResponse;
import com.pmrgsolution.paymentservice.dto.response.PaymentTransactionResponse;
import com.pmrgsolution.paymentservice.exception.PaymentNotFoundException;
import com.pmrgsolution.paymentservice.mapper.PaymentMapper;
import com.pmrgsolution.paymentservice.repository.PaymentGatewaySettingsRepository;
import com.pmrgsolution.paymentservice.repository.PaymentTransactionRepository;
import com.pmrgsolution.paymentservice.service.AdminPaymentService;
import com.pmrgsolution.paymentservice.service.OutboxEventPublisher;
import com.pmrgsolution.shared.core.exception.BadRequestException;
import com.pmrgsolution.shared.core.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPaymentServiceImpl implements AdminPaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentGatewaySettingsRepository paymentGatewaySettingsRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentTransactionResponse> getTransactions(PaymentStatus status, PaymentMethod method, Pageable pageable) {
        Page<PaymentTransaction> page;
        if (status != null && method != null) {
            page = paymentTransactionRepository.findByPaymentMethodAndStatus(method, status, pageable);
        } else if (status != null) {
            page = paymentTransactionRepository.findByStatus(status, pageable);
        } else {
            page = paymentTransactionRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(paymentMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentTransactionResponse> getPendingManualTransactions(Pageable pageable) {
        Page<PaymentTransaction> page = paymentTransactionRepository.findByStatus(PaymentStatus.PENDING_MANUAL_REVIEW, pageable);
        return PageResponse.from(page.map(paymentMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentTransactionResponse getTransactionById(UUID transactionId) {
        PaymentTransaction tx = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment transaction not found with ID: " + transactionId));
        return paymentMapper.toResponse(tx);
    }

    @Override
    @Transactional
    public PaymentTransactionResponse reviewManualPayment(UUID transactionId, ReviewManualPaymentRequest req) {
        log.info("Admin reviewing manual payment {}: decision={}", transactionId, req.getDecision());

        PaymentTransaction tx = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment transaction not found with ID: " + transactionId));

        if (tx.getStatus() != PaymentStatus.PENDING_MANUAL_REVIEW) {
            throw new BadRequestException("Transaction status is " + tx.getStatus() + ", only PENDING_MANUAL_REVIEW transactions can be reviewed");
        }

        if (req.getDecision() != PaymentStatus.VERIFIED && req.getDecision() != PaymentStatus.REJECTED) {
            throw new BadRequestException("Review decision must be either VERIFIED or REJECTED");
        }

        if (req.getDecision() == PaymentStatus.VERIFIED) {
            tx.setStatus(PaymentStatus.VERIFIED);
            tx.setVerifiedAt(Instant.now());

            // Stage Outbox Event: PAYMENT_VERIFIED
            Map<String, Object> verifiedEvent = new HashMap<>();
            verifiedEvent.put("eventId", UUID.randomUUID().toString());
            verifiedEvent.put("occurredAt", tx.getVerifiedAt());
            verifiedEvent.put("paymentId", tx.getId().toString());
            verifiedEvent.put("entityType", "ORDER");
            verifiedEvent.put("entityId", tx.getOrderId());
            verifiedEvent.put("orderId", tx.getOrderId());
            verifiedEvent.put("userId", tx.getUserId());
            verifiedEvent.put("amount", tx.getAmount());
            verifiedEvent.put("gatewayPaymentId", tx.getManualUpiUtrNumber());
            verifiedEvent.put("verifiedBy", "ADMIN");
            verifiedEvent.put("timestamp", tx.getVerifiedAt());
            outboxEventPublisher.saveEvent("Payment", tx.getId().toString(), "PAYMENT_VERIFIED", verifiedEvent);
        } else {
            tx.setStatus(PaymentStatus.REJECTED);
            tx.setFailureReason(req.getRejectionReason() != null ? req.getRejectionReason() : "Rejected by admin manual review");

            // Stage Outbox Event: PAYMENT_REJECTED
            Map<String, Object> rejectEvent = new HashMap<>();
            rejectEvent.put("eventId", UUID.randomUUID().toString());
            rejectEvent.put("occurredAt", Instant.now());
            rejectEvent.put("paymentId", tx.getId().toString());
            rejectEvent.put("entityType", "ORDER");
            rejectEvent.put("entityId", tx.getOrderId());
            rejectEvent.put("orderId", tx.getOrderId());
            rejectEvent.put("userId", tx.getUserId());
            rejectEvent.put("rejectedBy", "ADMIN");
            rejectEvent.put("remarks", tx.getFailureReason());
            rejectEvent.put("reason", tx.getFailureReason());
            rejectEvent.put("timestamp", Instant.now());
            outboxEventPublisher.saveEvent("Payment", tx.getId().toString(), "PAYMENT_REJECTED", rejectEvent);
        }

        PaymentTransaction savedTx = paymentTransactionRepository.save(tx);
        return paymentMapper.toResponse(savedTx);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentSettingsResponse getSettings() {
        PaymentGatewaySettings settings = paymentGatewaySettingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> paymentGatewaySettingsRepository.save(PaymentGatewaySettings.builder().build()));
        return paymentMapper.toSettingsResponse(settings);
    }

    @Override
    @Transactional
    public PaymentSettingsResponse updateSettings(UpdatePaymentSettingsRequest req) {
        log.info("Admin updating payment gateway settings");

        PaymentGatewaySettings settings = paymentGatewaySettingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> PaymentGatewaySettings.builder().build());

        settings.setRazorpayEnabled(req.isRazorpayEnabled());
        if (req.getRazorpayKeyId() != null) {
            settings.setRazorpayKeyId(req.getRazorpayKeyId());
        }
        settings.setManualUpiEnabled(req.isManualUpiEnabled());
        if (req.getUpiId() != null) {
            settings.setUpiId(req.getUpiId());
        }
        if (req.getUpiAccountHolder() != null) {
            settings.setUpiAccountHolder(req.getUpiAccountHolder());
        }
        if (req.getUpiQrCodeUrl() != null) {
            settings.setUpiQrCodeUrl(req.getUpiQrCodeUrl());
        }
        if (req.getBankName() != null) {
            settings.setBankName(req.getBankName());
        }
        if (req.getBankAccountNumber() != null) {
            settings.setBankAccountNumber(req.getBankAccountNumber());
        }
        if (req.getBankIfscCode() != null) {
            settings.setBankIfscCode(req.getBankIfscCode());
        }
        if (req.getBankBranch() != null) {
            settings.setBankBranch(req.getBankBranch());
        }

        PaymentGatewaySettings saved = paymentGatewaySettingsRepository.save(settings);
        return paymentMapper.toSettingsResponse(saved);
    }
}
