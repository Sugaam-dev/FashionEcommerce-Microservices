package com.pmrgsolution.authservice;

import com.pmrgsolution.authservice.entity.OutboxEvent;
import com.pmrgsolution.authservice.enums.OutboxStatus;
import com.pmrgsolution.authservice.kafka.producer.AuthEventProducer;
import com.pmrgsolution.authservice.repository.OutboxEventRepository;
import com.pmrgsolution.authservice.scheduler.OutboxPollingScheduler;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPollingSchedulerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private AuthEventProducer authEventProducer;

    @InjectMocks
    private OutboxPollingScheduler outboxPollingScheduler;

    @Test
    @DisplayName("Outbox Poller: Publishes pending event and updates status to PUBLISHED")
    void testProcessOutboxEvents_Success() {
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("User")
                .aggregateId("user-123")
                .eventType("USER_REGISTERED")
                .topic("auth.user-registered.v1")
                .payload("{\"userId\":\"user-123\"}")
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        when(outboxEventRepository.findTop50ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(anyList(), anyInt()))
                .thenReturn(List.of(event));

        RecordMetadata metadata = new RecordMetadata(new TopicPartition("auth.user-registered.v1", 0), 0, 0, 0, 0, 0);
        SendResult<String, String> sendResult = new SendResult<>(null, metadata);
        when(authEventProducer.sendEvent(eq("auth.user-registered.v1"), eq("user-123"), eq("{\"userId\":\"user-123\"}"), eq("USER_REGISTERED"), eq(null)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        outboxPollingScheduler.processOutboxEvents();

        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
        assertNotNull(event.getPublishedAt());
        verify(outboxEventRepository).save(event);
    }
}
