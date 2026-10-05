package com.pmrgsolution.authservice.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public CompletableFuture<SendResult<String, String>> sendEvent(
            String topic,
            String key,
            String payload,
            String eventType,
            String correlationId
    ) {
        String corrId = (correlationId != null && !correlationId.isBlank())
                ? correlationId
                : UUID.randomUUID().toString();

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);
        record.headers().add(new RecordHeader("X-Event-Type", eventType.getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("X-Source-Service", "auth-service".getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("X-Correlation-Id", corrId.getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("X-Timestamp", String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8)));

        log.info("Publishing Kafka event to topic={}, eventType={}, key={}, correlationId={}",
                topic, eventType, key, corrId);

        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send Kafka event to topic={} for key={}: {}", topic, key, ex.getMessage());
            } else {
                log.debug("Successfully published event to topic={}, partition={}, offset={}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });

        return future;
    }
}
