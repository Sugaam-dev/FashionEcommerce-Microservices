package com.pmrgsolution.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrgsolution.orderservice.entity.OutboxEvent;
import com.pmrgsolution.orderservice.enums.OutboxStatus;
import com.pmrgsolution.orderservice.repository.OutboxEventRepository;
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
            String payload = event instanceof String s ? s : objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payload)
                    .status(OutboxStatus.PENDING)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Saved outbox event: {} for aggregate: {} ({})", eventType, aggregateType, aggregateId);
        } catch (Exception e) {
            log.error("Failed to save outbox event for {} {}: {}", aggregateType, aggregateId, e.getMessage(), e);
            throw new RuntimeException("Failed to stage transactional outbox event", e);
        }
    }
}
