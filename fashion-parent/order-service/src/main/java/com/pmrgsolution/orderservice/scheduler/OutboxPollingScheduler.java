package com.pmrgsolution.orderservice.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrgsolution.orderservice.entity.OutboxEvent;
import com.pmrgsolution.orderservice.enums.OutboxStatus;
import com.pmrgsolution.orderservice.kafka.producer.OrderEventProducer;
import com.pmrgsolution.orderservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPollingScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final OrderEventProducer orderEventProducer;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    public void pollOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events to publish", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                Object payloadObj = objectMapper.readValue(event.getPayload(), Object.class);
                orderEventProducer.publishRaw(event.getEventType(), event.getAggregateId(), payloadObj);
                outboxEventRepository.markPublished(event.getId(), Instant.now());
                log.info("Successfully published outbox event {} to topic {}", event.getId(), event.getEventType());
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", event.getId(), e.getMessage());
                if (event.getRetryCount() >= 5) {
                    outboxEventRepository.updateStatusAndError(event.getId(), OutboxStatus.FAILED, e.getMessage());
                } else {
                    outboxEventRepository.updateStatusAndError(event.getId(), OutboxStatus.PENDING, e.getMessage());
                }
            }
        }
    }

    @Scheduled(cron = "0 */15 * * * *")
    @Transactional(readOnly = true)
    public void checkFailedOutboxEvents() {
        long failedCount = outboxEventRepository.countByStatus(OutboxStatus.FAILED);
        if (failedCount > 0) {
            log.error("ALERT: {} outbox events permanently failed in order-service. Manual intervention required.", failedCount);
        }
    }
}
