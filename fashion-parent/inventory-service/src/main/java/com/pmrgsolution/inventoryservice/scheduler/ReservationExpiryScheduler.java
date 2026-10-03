package com.pmrgsolution.inventoryservice.scheduler;

import com.pmrgsolution.inventoryservice.entity.InventoryItem;
import com.pmrgsolution.inventoryservice.entity.StockReservation;
import com.pmrgsolution.inventoryservice.enums.ReservationStatus;
import com.pmrgsolution.inventoryservice.repository.InventoryItemRepository;
import com.pmrgsolution.inventoryservice.repository.StockReservationRepository;
import com.pmrgsolution.inventoryservice.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpiryScheduler {

    private static final String LOCK_PREFIX = "lock:inventory:sku:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final StockReservationRepository stockReservationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final DistributedLockService distributedLockService;

    @Scheduled(fixedDelayString = "${inventory.reservation.expiry-interval-ms:60000}")
    @Transactional
    public void processExpiredReservations() {
        Instant now = Instant.now();
        List<StockReservation> expiredReservations = stockReservationRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.PENDING,
                now
        );

        if (expiredReservations.isEmpty()) {
            return;
        }

        log.info("Found {} expired stock reservations to release", expiredReservations.size());

        for (StockReservation reservation : expiredReservations) {
            String lockKey = LOCK_PREFIX + reservation.getSku();
            String lockToken = UUID.randomUUID().toString();

            boolean locked = distributedLockService.acquireLockWithRetry(lockKey, lockToken, LOCK_TTL, 3, 50);
            if (!locked) {
                log.warn("Skipping expiry release for reservation {} due to lock contention on SKU {}",
                        reservation.getId(), reservation.getSku());
                continue;
            }

            try {
                inventoryItemRepository.findBySku(reservation.getSku()).ifPresent(item -> {
                    item.setAvailableQuantity(item.getAvailableQuantity() + reservation.getQuantity());
                    item.setReservedQuantity(Math.max(0, item.getReservedQuantity() - reservation.getQuantity()));
                    inventoryItemRepository.save(item);
                });

                reservation.setStatus(ReservationStatus.EXPIRED);
                stockReservationRepository.save(reservation);
                log.info("Expired reservation {} released back to stock for orderId={}",
                        reservation.getId(), reservation.getOrderId());
            } catch (Exception e) {
                log.error("Failed to release expired reservation {}: {}", reservation.getId(), e.getMessage(), e);
            } finally {
                distributedLockService.releaseLock(lockKey, lockToken);
            }
        }
    }
}
