package com.pmrgsolution.orderservice.scheduler;

import com.pmrgsolution.orderservice.dto.request.ReleaseStockRequest;
import com.pmrgsolution.orderservice.entity.Order;
import com.pmrgsolution.orderservice.entity.OrderStatusHistory;
import com.pmrgsolution.orderservice.enums.OrderStatus;
import com.pmrgsolution.orderservice.feign.InventoryClient;
import com.pmrgsolution.orderservice.repository.OrderRepository;
import com.pmrgsolution.orderservice.service.OutboxEventPublisher;
import com.pmrgsolution.shared.events.order.OrderCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderReconciliationScheduler {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final OutboxEventPublisher outboxEventPublisher;

    @Value("${app.security.internal-service-key:fashion-internal-secret-key-2026-v1}")
    private String internalServiceKey;

    @Scheduled(cron = "0 30 2 * * *") // Daily at 02:30 UTC
    @Transactional
    public void reconcileStuckOrders() {
        log.info("Running daily order reconciliation for stuck orders");

        Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        List<Order> stuckOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, twentyFourHoursAgo);

        if (stuckOrders.isEmpty()) {
            log.info("No stuck PENDING_PAYMENT orders found");
            return;
        }

        log.warn("Found {} stuck orders in PENDING_PAYMENT past 24 hours, auto-cancelling...", stuckOrders.size());

        for (Order order : stuckOrders) {
            try {
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancelledAt(Instant.now());
                order.setCancellationReason("Auto-cancelled by reconciliation: payment not completed within 24 hours");

                OrderStatusHistory history = OrderStatusHistory.builder()
                        .status(OrderStatus.CANCELLED)
                        .comments("Auto-cancelled by reconciliation job")
                        .changedBy("SYSTEM_RECONCILIATION")
                        .build();
                order.addStatusHistory(history);
                orderRepository.save(order);

                // Release inventory reservation
                try {
                    inventoryClient.releaseReservation(internalServiceKey, new ReleaseStockRequest(order.getOrderNumber()));
                } catch (Exception e) {
                    log.error("Failed to release inventory during reconciliation for order {}: {}", order.getOrderNumber(), e.getMessage());
                }

                // Stage cancellation event
                OrderCancelledEvent cancelledEvent = new OrderCancelledEvent(
                        UUID.randomUUID().toString(),
                        Instant.now(),
                        order.getId().toString(),
                        order.getUserId().toString(),
                        "Payment window expired (24h)"
                );
                outboxEventPublisher.saveEvent("Order", order.getId().toString(), "order.cancelled.v1", cancelledEvent);

                log.info("Successfully auto-cancelled stuck order: {}", order.getOrderNumber());
            } catch (Exception e) {
                log.error("Failed to reconcile order {}: {}", order.getOrderNumber(), e.getMessage(), e);
            }
        }
    }
}
