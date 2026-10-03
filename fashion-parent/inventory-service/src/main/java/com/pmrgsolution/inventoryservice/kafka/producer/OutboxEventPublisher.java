package com.pmrgsolution.inventoryservice.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrgsolution.inventoryservice.entity.OutboxEvent;
import com.pmrgsolution.inventoryservice.enums.OutboxStatus;
import com.pmrgsolution.inventoryservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void saveEvent(String aggregateType, String aggregateId, String eventType, Object event) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            OutboxEvent outbox = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(jsonPayload)
                    .status(OutboxStatus.PENDING)
                    .build();

            outboxEventRepository.save(outbox);
            log.debug("Saved outbox event: type={}, aggregateId={}, eventType={}",
                    aggregateType, aggregateId, eventType);
        } catch (Exception e) {
            log.error("Failed to serialize and save outbox event for aggregate {}: {}", aggregateId, e.getMessage(), e);
            throw new RuntimeException("Failed to stage outbox event: " + e.getMessage(), e);
        }
    }
}
