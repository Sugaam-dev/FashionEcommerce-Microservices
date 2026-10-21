package com.pmrgsolution.orderservice.kafka.consumer;

import com.pmrgsolution.orderservice.dto.request.ConfirmStockRequest;
import com.pmrgsolution.orderservice.dto.request.ReleaseStockRequest;
import com.pmrgsolution.orderservice.entity.Order;
import com.pmrgsolution.orderservice.entity.OrderStatusHistory;
import com.pmrgsolution.orderservice.entity.ProcessedEvent;
import com.pmrgsolution.orderservice.enums.OrderStatus;
import com.pmrgsolution.orderservice.feign.InventoryClient;
import com.pmrgsolution.orderservice.repository.OrderRepository;
import com.pmrgsolution.orderservice.repository.ProcessedEventRepository;
import com.pmrgsolution.shared.events.payment.PaymentRejectedEvent;
import com.pmrgsolution.shared.events.payment.PaymentVerifiedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaymentKafkaConsumer {

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final InventoryClient inventoryClient;

    @Value("${app.security.internal-service-key:fashion-internal-secret-key-2026-v1}")
    private String internalServiceKey;

    @Transactional
    @KafkaListener(topics = "payment.verified.v1", groupId = "order-service-group")
    public void onPaymentVerified(PaymentVerifiedEvent event) {
        String targetId = (event.entityId() != null && !event.entityId().isBlank()) ? event.entityId() : event.orderId();
        log.info("Received PaymentVerifiedEvent for order targetId: {}, paymentId: {}", targetId, event.paymentId());

        if (event.eventId() != null && processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("Duplicate PaymentVerifiedEvent ignored: {}", event.eventId());
            return;
        }

        try {
            Optional<Order> orderOpt = findOrder(targetId);
            if (orderOpt.isEmpty()) {
                log.error("Order not found for PaymentVerifiedEvent: {}", targetId);
                return;
            }

            Order order = orderOpt.get();

            // Financial integrity: Verify paid amount matches order total amount
            if (event.amount() != null && event.amount().compareTo(order.getTotalAmount()) != 0) {
                log.error("CRITICAL: Payment amount mismatch for order {}. Expected: {}, Received: {}. Refusing to confirm order.",
                        order.getOrderNumber(), order.getTotalAmount(), event.amount());
                return;
            }

            if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
                order.setPaidAt(event.occurredAt() != null ? event.occurredAt() : Instant.now());

                String verifier = event.verifiedBy() != null ? event.verifiedBy() : "GATEWAY";
                OrderStatusHistory history = OrderStatusHistory.builder()
                        .status(OrderStatus.PAYMENT_CONFIRMED)
                        .comments("Payment confirmed via " + verifier + " (Payment ID: " + event.paymentId() + ")")
                        .changedBy("SYSTEM")
                        .build();
                order.addStatusHistory(history);
                orderRepository.save(order);

                // Confirm stock reservation with inventory-service
                try {
                    inventoryClient.confirmReservation(internalServiceKey, new ConfirmStockRequest(order.getOrderNumber()));
                } catch (Exception e) {
                    log.error("Failed to confirm stock reservation for order {}: {}", order.getOrderNumber(), e.getMessage());
                }
            }

            if (event.eventId() != null) {
                ProcessedEvent processed = ProcessedEvent.builder()
                        .eventId(event.eventId())
                        .topic("payment.verified.v1")
                        .build();
                processedEventRepository.save(processed);
            }
        } catch (Exception e) {
            log.error("Error processing PaymentVerifiedEvent: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    @KafkaListener(topics = "payment.rejected.v1", groupId = "order-service-group")
    public void onPaymentRejected(PaymentRejectedEvent event) {
        String targetId = (event.entityId() != null && !event.entityId().isBlank()) ? event.entityId() : event.orderId();
        String rejectionReason = (event.remarks() != null && !event.remarks().isBlank()) ? event.remarks() : "Payment rejected by gateway";
        log.info("Received PaymentRejectedEvent for order targetId: {}, reason: {}", targetId, rejectionReason);

        if (event.eventId() != null && processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("Duplicate PaymentRejectedEvent ignored: {}", event.eventId());
            return;
        }

        try {
            Optional<Order> orderOpt = findOrder(targetId);
            if (orderOpt.isEmpty()) {
                log.error("Order not found for PaymentRejectedEvent: {}", targetId);
                return;
            }

            Order order = orderOpt.get();
            if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancelledAt(event.occurredAt() != null ? event.occurredAt() : Instant.now());
                order.setCancellationReason(rejectionReason);

                OrderStatusHistory history = OrderStatusHistory.builder()
                        .status(OrderStatus.CANCELLED)
                        .comments("Payment rejected: " + rejectionReason)
                        .changedBy("SYSTEM")
                        .build();
                order.addStatusHistory(history);
                orderRepository.save(order);

                // Release stock reservation with inventory-service
                try {
                    inventoryClient.releaseReservation(internalServiceKey, new ReleaseStockRequest(order.getOrderNumber()));
                } catch (Exception e) {
                    log.error("Failed to release stock reservation for order {}: {}", order.getOrderNumber(), e.getMessage());
                }
            }

            if (event.eventId() != null) {
                ProcessedEvent processed = ProcessedEvent.builder()
                        .eventId(event.eventId())
                        .topic("payment.rejected.v1")
                        .build();
                processedEventRepository.save(processed);
            }
        } catch (Exception e) {
            log.error("Error processing PaymentRejectedEvent: {}", e.getMessage(), e);
            throw e;
        }
    }

    private Optional<Order> findOrder(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return Optional.empty();
        }
        try {
            UUID id = UUID.fromString(entityId);
            Optional<Order> byId = orderRepository.findById(id);
            if (byId.isPresent()) return byId;
        } catch (IllegalArgumentException ignored) {
            // Not a UUID, treat as orderNumber
        }
        return orderRepository.findByOrderNumber(entityId);
    }
}
