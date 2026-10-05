package com.pmrgsolution.authservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrgsolution.authservice.entity.OutboxEvent;
import com.pmrgsolution.authservice.enums.OutboxStatus;
import com.pmrgsolution.authservice.repository.OutboxEventRepository;
import com.pmrgsolution.authservice.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveEvent(String aggregateType, String aggregateId, String eventType, String topic, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .topic(topic)
                    .payload(payloadJson)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.debug("Saved OutboxEvent: aggregateType={}, aggregateId={}, eventType={}, topic={}",
                    aggregateType, aggregateId, eventType, topic);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload for aggregate {}: {}", aggregateId, e.getMessage());
            throw new IllegalArgumentException("Failed to serialize outbox event payload", e);
        }
    }
}
