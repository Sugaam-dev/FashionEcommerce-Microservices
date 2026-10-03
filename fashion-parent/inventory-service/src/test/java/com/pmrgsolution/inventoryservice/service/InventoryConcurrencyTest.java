package com.pmrgsolution.inventoryservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pmrgsolution.inventoryservice.dto.request.ReserveStockRequest;
import com.pmrgsolution.inventoryservice.dto.request.StockItemReserveInfo;
import com.pmrgsolution.inventoryservice.entity.InventoryItem;
import com.pmrgsolution.inventoryservice.exception.InsufficientStockException;
import com.pmrgsolution.inventoryservice.kafka.producer.InventoryEventProducer;
import com.pmrgsolution.inventoryservice.repository.InventoryItemRepository;
import com.pmrgsolution.inventoryservice.repository.StockReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryConcurrencyTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @Mock
    private DistributedLockService distributedLockService;

    @Mock
    private InventoryEventProducer inventoryEventProducer;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Test
    @DisplayName("Concurrency: 100 concurrent reservation threads for limited stock prevents overselling")
    void testConcurrentReservations_NoOverselling() throws InterruptedException {
        int initialStock = 20;
        int totalThreads = 100;
        int requestedPerThread = 1;

        InventoryItem sharedItem = InventoryItem.builder()
                .id(UUID.randomUUID())
                .sku("SKU-CONCURRENT-001")
                .productId("PROD-HOT-DEAL")
                .productName("Hot Deal Item")
                .availableQuantity(initialStock)
                .reservedQuantity(0)
                .lowStockThreshold(5)
                .warehouseCode("WH-MAIN")
                .build();

        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        ReentrantLock skuLock = new ReentrantLock();
        lenient().when(distributedLockService.acquireLocks(anyList(), anyString(), any(Duration.class), anyInt(), anyLong()))
                .thenAnswer(inv -> {
                    skuLock.lock();
                    return true;
                });
        lenient().doAnswer(inv -> {
            if (skuLock.isHeldByCurrentThread()) {
                skuLock.unlock();
            }
            return null;
        }).when(distributedLockService).releaseLocks(anyList(), anyString());

        when(inventoryItemRepository.findBySku("SKU-CONCURRENT-001")).thenReturn(Optional.of(sharedItem));

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(totalThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            final String orderId = "ORD-CONC-" + i;
            executor.submit(() -> {
                try {
                    ReserveStockRequest request = new ReserveStockRequest(
                            orderId,
                            List.of(new StockItemReserveInfo("SKU-CONCURRENT-001", requestedPerThread))
                    );
                    inventoryService.reserveStock(request, "IDEMP-" + orderId);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(initialStock);
        assertThat(failureCount.get()).isEqualTo(totalThreads - initialStock);
        assertThat(sharedItem.getAvailableQuantity()).isZero();
        assertThat(sharedItem.getReservedQuantity()).isEqualTo(initialStock);
    }
}
