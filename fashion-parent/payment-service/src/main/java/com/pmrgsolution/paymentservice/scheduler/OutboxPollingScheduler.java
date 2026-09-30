package com.pmrgsolution.paymentservice.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrgsolution.paymentservice.domain.entity.OutboxEvent;
import com.pmrgsolution.paymentservice.domain.enums.OutboxStatus;
import com.pmrgsolution.paymentservice.kafka.PaymentEventProducer;
import com.pmrgsolution.paymentservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, 50)
        );

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Processing {} pending payment outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                String topic = switch (event.getEventType()) {
                    case "PAYMENT_INITIATED" -> PaymentEventProducer.TOPIC_PAYMENT_INITIATED;
                    case "PAYMENT_VERIFIED" -> PaymentEventProducer.TOPIC_PAYMENT_VERIFIED;
                    case "PAYMENT_REJECTED" -> PaymentEventProducer.TOPIC_PAYMENT_REJECTED;
                    case "NOTIFICATION_REQUEST" -> PaymentEventProducer.TOPIC_NOTIFICATION_REQUEST;
                    default -> event.getEventType();
                };

                Object payload = objectMapper.readTree(event.getPayload());
                kafkaTemplate.send(topic, event.getAggregateId(), payload).get();

                event.setStatus(OutboxStatus.PUBLISHED);
                event.setProcessedAt(Instant.now());
                log.info("Successfully published payment outbox event: {} ({}) to topic: {}",
                        event.getId(), event.getEventType(), topic);
            } catch (Exception e) {
                log.error("Failed to publish payment outbox event {}: {}", event.getId(), e.getMessage(), e);
                event.setRetryCount(event.getRetryCount() + 1);
                event.setErrorMessage(e.getMessage() != null && e.getMessage().length() > 950 ?
                        e.getMessage().substring(0, 950) : e.getMessage());
                if (event.getRetryCount() >= 5) {
                    event.setStatus(OutboxStatus.FAILED);
                }
            }
            outboxEventRepository.save(event);
        }
    }

    @Scheduled(cron = "0 0 2 * * *") // Daily at 02:00 AM
    @Transactional
    public void purgeOldOutboxEvents() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        int purged = outboxEventRepository.purgePublishedEvents(cutoff);
        if (purged > 0) {
            log.info("Purged {} published payment outbox events older than 7 days", purged);
        }
    }
}
