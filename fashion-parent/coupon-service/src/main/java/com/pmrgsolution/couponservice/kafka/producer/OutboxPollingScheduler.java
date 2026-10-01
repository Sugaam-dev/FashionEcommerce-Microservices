package com.pmrgsolution.couponservice.kafka.producer;

import com.pmrgsolution.couponservice.entity.OutboxEvent;
import com.pmrgsolution.couponservice.enums.OutboxStatus;
import com.pmrgsolution.couponservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPollingScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                event.setStatus(OutboxStatus.PUBLISHED);
                                event.setSentAt(Instant.now());
                                outboxEventRepository.save(event);
                                log.debug("Dispatched outbox event id={} to topic={}", event.getId(), event.getEventType());
                            } else {
                                event.setRetryCount(event.getRetryCount() + 1);
                                if (event.getRetryCount() >= 5) {
                                    event.setStatus(OutboxStatus.FAILED);
                                    log.error("Outbox event id={} failed after 5 retries: {}", event.getId(), ex.getMessage());
                                }
                                outboxEventRepository.save(event);
                            }
                        });
            } catch (Exception e) {
                log.error("Synchronous dispatch failure for outbox event id={}: {}", event.getId(), e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 5) {
                    event.setStatus(OutboxStatus.FAILED);
                }
                outboxEventRepository.save(event);
            }
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupProcessedOutboxEvents() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        int deleted = outboxEventRepository.deleteByStatusAndCreatedAtBefore(OutboxStatus.PUBLISHED, cutoff);
        log.info("Cleaned up {} published outbox events older than 7 days", deleted);
    }
}
