package com.pmrgsolution.inventoryservice.kafka.producer;

import com.pmrgsolution.inventoryservice.entity.InventoryItem;
import com.pmrgsolution.shared.core.constants.KafkaTopics;
import com.pmrgsolution.shared.events.inventory.InventoryLowEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventProducer {

    private final OutboxEventPublisher outboxEventPublisher;

    public void publishLowStockAlert(InventoryItem item) {
        InventoryLowEvent event = new InventoryLowEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                item.getSku(),
                item.getProductName(),
                item.getAvailableQuantity(),
                item.getLowStockThreshold()
        );

        outboxEventPublisher.saveEvent(
                "InventoryItem",
                item.getId().toString(),
                KafkaTopics.INVENTORY_LOW_STOCK,
                event
        );

        log.info("Staged low stock outbox alert for SKU={}, available={}, threshold={}",
                item.getSku(), item.getAvailableQuantity(), item.getLowStockThreshold());
    }
}
