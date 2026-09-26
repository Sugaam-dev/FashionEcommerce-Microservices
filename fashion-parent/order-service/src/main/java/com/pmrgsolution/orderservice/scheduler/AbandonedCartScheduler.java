package com.pmrgsolution.orderservice.scheduler;

import com.pmrgsolution.orderservice.kafka.producer.OrderEventProducer;
import com.pmrgsolution.orderservice.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AbandonedCartScheduler {

    private final CartItemRepository cartItemRepository;
    private final OrderEventProducer orderEventProducer;

    @Scheduled(cron = "0 0 */2 * * *") // Every 2 hours
    @Transactional(readOnly = true)
    public void notifyAbandonedCarts() {
        log.info("Running abandoned cart notification job");

        Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        List<UUID> abandonedUserIds = cartItemRepository.findDistinctUserIdsWithCartOlderThan(twentyFourHoursAgo);

        if (abandonedUserIds.isEmpty()) {
            log.debug("No abandoned carts found");
            return;
        }

        log.info("Found {} users with abandoned carts", abandonedUserIds.size());

        for (UUID userId : abandonedUserIds) {
            long itemCount = cartItemRepository.countByUserId(userId);
            if (itemCount == 0) continue;

            Map<String, Object> notificationPayload = new HashMap<>();
            notificationPayload.put("eventId", UUID.randomUUID().toString());
            notificationPayload.put("recipient", userId.toString());
            notificationPayload.put("templateId", "abandoned-cart");
            notificationPayload.put("channel", "EMAIL");
            notificationPayload.put("variables", Map.of(
                    "userId", userId.toString(),
                    "cartItemCount", String.valueOf(itemCount)
            ));

            try {
                orderEventProducer.publishRaw("notification.request.v1", userId.toString(), notificationPayload);
                log.info("Queued abandoned cart reminder for user: {}", userId);
            } catch (Exception e) {
                log.error("Failed to queue abandoned cart notification for user {}: {}", userId, e.getMessage());
            }
        }
    }
}
