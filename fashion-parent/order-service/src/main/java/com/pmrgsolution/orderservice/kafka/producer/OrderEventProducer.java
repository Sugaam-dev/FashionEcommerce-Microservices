package com.pmrgsolution.orderservice.kafka.producer;

import com.pmrgsolution.shared.events.order.OrderCancelledEvent;
import com.pmrgsolution.shared.events.order.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent to Kafka topic: order.created.v1 for order: {}", event.orderId());
        kafkaTemplate.send("order.created.v1", event.orderId(), event);
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        log.info("Publishing OrderCancelledEvent to Kafka topic: order.cancelled.v1 for order: {}", event.orderId());
        kafkaTemplate.send("order.cancelled.v1", event.orderId(), event);
    }

    public void publishRaw(String topic, String key, Object payload) {
        log.info("Publishing event to topic: {} with key: {}", topic, key);
        kafkaTemplate.send(topic, key, payload);
    }
}
