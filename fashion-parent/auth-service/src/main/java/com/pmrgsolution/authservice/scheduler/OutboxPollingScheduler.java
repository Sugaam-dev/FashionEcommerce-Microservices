package com.pmrgsolution.authservice.scheduler;

import com.pmrgsolution.authservice.entity.OutboxEvent;
import com.pmrgsolution.authservice.enums.OutboxStatus;
import com.pmrgsolution.authservice.kafka.producer.AuthEventProducer;
import com.pmrgsolution.authservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPollingScheduler {

    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final AuthEventProducer authEventProducer;

    @Scheduled(fixedDelayString = "${app.outbox.polling-interval-ms:2000}", initialDelay = 5000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findTop50ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
                        List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
                        MAX_RETRIES
                );

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Polling Outbox: processing {} pending events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                authEventProducer.sendEvent(
                        event.getTopic(),
                        event.getAggregateId(),
                        event.getPayload(),
                        event.getEventType(),
                        null
                ).get(5, TimeUnit.SECONDS);

                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                event.setErrorMessage(null);
            } catch (Exception e) {
                log.error("Failed to publish outbox event id={}: {}", event.getId(), e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                event.setStatus(OutboxStatus.FAILED);
                event.setErrorMessage(e.getMessage());
            }

            outboxEventRepository.save(event);
        }
    }
}
