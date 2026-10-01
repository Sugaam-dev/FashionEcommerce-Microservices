package com.pmrgsolution.couponservice.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrgsolution.couponservice.entity.OutboxEvent;
import com.pmrgsolution.couponservice.enums.OutboxStatus;
import com.pmrgsolution.couponservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();

            outboxEventRepository.save(event);
            log.debug("Saved outbox event type={} for aggregateId={}", eventType, aggregateId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload for aggregateId={}: {}", aggregateId, e.getMessage(), e);
            throw new IllegalArgumentException("Could not serialize event payload", e);
        }
    }
}
