package com.pmrgsolution.paymentservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    public static final String TOPIC_PAYMENT_INITIATED = "payment.initiated.v1";
    public static final String TOPIC_PAYMENT_VERIFIED = "payment.verified.v1";
    public static final String TOPIC_PAYMENT_REJECTED = "payment.rejected.v1";
    public static final String TOPIC_NOTIFICATION_REQUEST = "notification.request.v1";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendEvent(String topic, String key, Object payload) {
        log.info("Publishing Kafka event to topic: {} with key: {}", topic, key);
        kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send Kafka event to topic {} for key {}: {}", topic, key, ex.getMessage(), ex);
            } else {
                log.debug("Successfully sent Kafka event to topic {} with offset: {}", topic, result.getRecordMetadata().offset());
            }
        });
    }
}
