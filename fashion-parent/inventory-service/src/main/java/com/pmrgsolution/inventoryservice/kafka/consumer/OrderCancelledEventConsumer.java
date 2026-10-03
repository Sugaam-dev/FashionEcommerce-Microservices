package com.pmrgsolution.inventoryservice.kafka.consumer;

import com.pmrgsolution.inventoryservice.dto.request.ReleaseStockRequest;
import com.pmrgsolution.inventoryservice.service.InventoryService;
import com.pmrgsolution.shared.core.constants.KafkaTopics;
import com.pmrgsolution.shared.events.order.OrderCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledEventConsumer {

    private final InventoryService inventoryService;

    @KafkaListener(
            topics = KafkaTopics.ORDER_CANCELLED,
            groupId = "${spring.kafka.consumer.group-id:inventory-service-group}"
    )
    public void handleOrderCancelled(OrderCancelledEvent event) {
        if (event == null || event.orderId() == null) {
            log.warn("Received empty or invalid OrderCancelledEvent");
            return;
        }

        log.info("Received OrderCancelledEvent for orderId={}, reason={}", event.orderId(), event.reason());
        try {
            inventoryService.releaseStock(new ReleaseStockRequest(event.orderId(), "Order cancelled: " + event.reason()));
        } catch (Exception e) {
            log.error("Failed to release stock for cancelled order {}: {}", event.orderId(), e.getMessage(), e);
        }
    }
}
