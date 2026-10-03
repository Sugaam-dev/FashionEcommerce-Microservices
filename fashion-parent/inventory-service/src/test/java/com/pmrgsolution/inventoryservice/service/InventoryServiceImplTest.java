package com.pmrgsolution.inventoryservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pmrgsolution.inventoryservice.dto.request.BulkStockCheckRequest;
import com.pmrgsolution.inventoryservice.dto.request.ConfirmStockRequest;
import com.pmrgsolution.inventoryservice.dto.request.CreateInventoryItemRequest;
import com.pmrgsolution.inventoryservice.dto.request.ReleaseStockRequest;
import com.pmrgsolution.inventoryservice.dto.request.ReserveStockRequest;
import com.pmrgsolution.inventoryservice.dto.request.StockAdjustmentRequest;
import com.pmrgsolution.inventoryservice.dto.request.StockItemReserveInfo;
import com.pmrgsolution.inventoryservice.dto.response.BulkStockCheckResponse;
import com.pmrgsolution.inventoryservice.dto.response.InventoryItemResponse;
import com.pmrgsolution.inventoryservice.dto.response.ReservationResponse;
import com.pmrgsolution.inventoryservice.entity.InventoryItem;
import com.pmrgsolution.inventoryservice.entity.StockReservation;
import com.pmrgsolution.inventoryservice.enums.ReservationStatus;
import com.pmrgsolution.inventoryservice.exception.DuplicateSkuException;
import com.pmrgsolution.inventoryservice.exception.InsufficientStockException;
import com.pmrgsolution.inventoryservice.exception.InventoryItemNotFoundException;
import com.pmrgsolution.inventoryservice.exception.ReservationNotFoundException;
import com.pmrgsolution.inventoryservice.kafka.producer.InventoryEventProducer;
import com.pmrgsolution.inventoryservice.repository.InventoryItemRepository;
import com.pmrgsolution.inventoryservice.repository.StockReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

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

    private InventoryItem testItem;
    private UUID itemId;

    @BeforeEach
    void setUp() {
        itemId = UUID.randomUUID();
        testItem = InventoryItem.builder()
                .id(itemId)
                .sku("SKU-RED-SHIRT-M")
                .productId("PROD-101")
                .productName("Classic Red Shirt")
                .availableQuantity(50)
                .reservedQuantity(5)
                .lowStockThreshold(10)
                .warehouseCode("WH-MAIN")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(distributedLockService.acquireLockWithRetry(anyString(), anyString(), any(Duration.class), anyInt(), anyLong()))
                .thenReturn(true);
        lenient().when(distributedLockService.acquireLocks(anyList(), anyString(), any(Duration.class), anyInt(), anyLong()))
                .thenReturn(true);
    }

    @Test
    @DisplayName("createInventoryItem: Success")
    void testCreateInventoryItem_Success() {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
                "SKU-NEW-01", "PROD-202", "Blue Jeans", 100, 15, "WH-EAST"
        );

        when(inventoryItemRepository.existsBySku("SKU-NEW-01")).thenReturn(false);
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> {
            InventoryItem item = inv.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        InventoryItemResponse response = inventoryService.createInventoryItem(request);

        assertThat(response).isNotNull();
        assertThat(response.sku()).isEqualTo("SKU-NEW-01");
        assertThat(response.availableQuantity()).isEqualTo(100);
        assertThat(response.warehouseCode()).isEqualTo("WH-EAST");
        verify(inventoryItemRepository).save(any(InventoryItem.class));
    }

    @Test
    @DisplayName("createInventoryItem: Throws DuplicateSkuException when SKU already exists")
    void testCreateInventoryItem_DuplicateSku() {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
                "SKU-RED-SHIRT-M", "PROD-101", "Classic Red Shirt", 50, 10, "WH-MAIN"
        );

        when(inventoryItemRepository.existsBySku("SKU-RED-SHIRT-M")).thenReturn(true);

        assertThatThrownBy(() -> inventoryService.createInventoryItem(request))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessageContaining("already exists");

        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("getStockBySku: Success")
    void testGetStockBySku_Success() {
        when(inventoryItemRepository.findBySku("SKU-RED-SHIRT-M")).thenReturn(Optional.of(testItem));

        InventoryItemResponse response = inventoryService.getStockBySku("SKU-RED-SHIRT-M");

        assertThat(response).isNotNull();
        assertThat(response.sku()).isEqualTo("SKU-RED-SHIRT-M");
        assertThat(response.availableQuantity()).isEqualTo(50);
        assertThat(response.reservedQuantity()).isEqualTo(5);
        assertThat(response.totalQuantity()).isEqualTo(55);
    }

    @Test
    @DisplayName("getStockBySku: Throws InventoryItemNotFoundException when SKU not found")
    void testGetStockBySku_NotFound() {
        when(inventoryItemRepository.findBySku("NON-EXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getStockBySku("NON-EXISTENT"))
                .isInstanceOf(InventoryItemNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("adjustStock: Increase stock successfully")
    void testAdjustStock_Increase_Success() {
        StockAdjustmentRequest request = new StockAdjustmentRequest("SKU-RED-SHIRT-M", 20, "Restock");
        when(inventoryItemRepository.findBySku("SKU-RED-SHIRT-M")).thenReturn(Optional.of(testItem));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryItemResponse response = inventoryService.adjustStock("SKU-RED-SHIRT-M", request);

        assertThat(response).isNotNull();
        assertThat(response.availableQuantity()).isEqualTo(70);
        verify(distributedLockService).releaseLock(eq("lock:inventory:sku:SKU-RED-SHIRT-M"), anyString());
    }

    @Test
    @DisplayName("adjustStock: Throws InsufficientStockException on excessive decrement")
    void testAdjustStock_Decrease_InsufficientStock() {
        StockAdjustmentRequest request = new StockAdjustmentRequest("SKU-RED-SHIRT-M", -60, "Damaged items");
        when(inventoryItemRepository.findBySku("SKU-RED-SHIRT-M")).thenReturn(Optional.of(testItem));

        assertThatThrownBy(() -> inventoryService.adjustStock("SKU-RED-SHIRT-M", request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");

        verify(distributedLockService).releaseLock(eq("lock:inventory:sku:SKU-RED-SHIRT-M"), anyString());
    }

    @Test
    @DisplayName("checkStock: Bulk check returns available stock per SKU")
    void testCheckStock_Bulk() {
        BulkStockCheckRequest request = new BulkStockCheckRequest(List.of("SKU-RED-SHIRT-M", "SKU-UNKNOWN"));
        when(inventoryItemRepository.findBySkuIn(anyList())).thenReturn(List.of(testItem));

        BulkStockCheckResponse response = inventoryService.checkStock(request);

        assertThat(response).isNotNull();
        assertThat(response.availableStockBySku()).containsEntry("SKU-RED-SHIRT-M", 50);
        assertThat(response.availableStockBySku()).containsEntry("SKU-UNKNOWN", 0);
    }

    @Test
    @DisplayName("reserveStock: Success reserves available stock and creates StockReservation")
    void testReserveStock_Success() {
        ReserveStockRequest request = new ReserveStockRequest(
                "ORD-9999",
                List.of(new StockItemReserveInfo("SKU-RED-SHIRT-M", 3))
        );

        when(inventoryItemRepository.findBySku("SKU-RED-SHIRT-M")).thenReturn(Optional.of(testItem));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse response = inventoryService.reserveStock(request, "IDEMP-KEY-1");

        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();
        assertThat(response.orderId()).isEqualTo("ORD-9999");
        assertThat(testItem.getAvailableQuantity()).isEqualTo(47);
        assertThat(testItem.getReservedQuantity()).isEqualTo(8);

        verify(stockReservationRepository).saveAll(anyList());
        verify(distributedLockService).releaseLocks(anyList(), anyString());
    }

    @Test
    @DisplayName("reserveStock: Returns cached response when Idempotency-Key is found")
    void testReserveStock_CachedIdempotency() throws Exception {
        ReserveStockRequest request = new ReserveStockRequest(
                "ORD-9999",
                List.of(new StockItemReserveInfo("SKU-RED-SHIRT-M", 3))
        );

        ReservationResponse cachedResponse = new ReservationResponse(
                "ORD-9999", true, "Stock reserved successfully", Instant.now().plus(Duration.ofMinutes(15))
        );
        String json = objectMapper.writeValueAsString(cachedResponse);
        when(valueOperations.get("idempotency:reservation:IDEMP-KEY-1")).thenReturn(json);

        ReservationResponse response = inventoryService.reserveStock(request, "IDEMP-KEY-1");

        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo("ORD-9999");
        verify(inventoryItemRepository, never()).save(any());
        verify(stockReservationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("reserveStock: Throws InsufficientStockException when requested > available")
    void testReserveStock_InsufficientStock() {
        ReserveStockRequest request = new ReserveStockRequest(
                "ORD-9999",
                List.of(new StockItemReserveInfo("SKU-RED-SHIRT-M", 100))
        );

        when(inventoryItemRepository.findBySku("SKU-RED-SHIRT-M")).thenReturn(Optional.of(testItem));

        assertThatThrownBy(() -> inventoryService.reserveStock(request, "IDEMP-KEY-2"))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");

        verify(stockReservationRepository, never()).saveAll(any());
        verify(distributedLockService).releaseLocks(anyList(), anyString());
    }

    @Test
    @DisplayName("confirmStock: Success sets status to CONFIRMED and clears reserved quantity")
    void testConfirmStock_Success() {
        ConfirmStockRequest request = new ConfirmStockRequest("ORD-9999");
        StockReservation reservation = StockReservation.builder()
                .id(UUID.randomUUID())
                .orderId("ORD-9999")
                .sku("SKU-RED-SHIRT-M")
                .quantity(5)
                .status(ReservationStatus.PENDING)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .build();

        when(stockReservationRepository.findByOrderIdAndStatus("ORD-9999", ReservationStatus.PENDING))
                .thenReturn(List.of(reservation));
        when(inventoryItemRepository.findBySku("SKU-RED-SHIRT-M")).thenReturn(Optional.of(testItem));

        ReservationResponse response = inventoryService.confirmStock(request);

        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(testItem.getReservedQuantity()).isEqualTo(0);
        verify(stockReservationRepository).save(reservation);
        verify(distributedLockService).releaseLocks(anyList(), anyString());
    }

    @Test
    @DisplayName("confirmStock: Throws ReservationNotFoundException when order has no reservations")
    void testConfirmStock_NotFound() {
        ConfirmStockRequest request = new ConfirmStockRequest("ORD-NON-EXISTENT");
        when(stockReservationRepository.findByOrderIdAndStatus("ORD-NON-EXISTENT", ReservationStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(stockReservationRepository.findByOrderId("ORD-NON-EXISTENT"))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> inventoryService.confirmStock(request))
                .isInstanceOf(ReservationNotFoundException.class)
                .hasMessageContaining("No stock reservation found");
    }

    @Test
    @DisplayName("releaseStock: Success restores available stock and sets status to RELEASED")
    void testReleaseStock_Success() {
        ReleaseStockRequest request = new ReleaseStockRequest("ORD-9999", "Order Cancelled");
        StockReservation reservation = StockReservation.builder()
                .id(UUID.randomUUID())
                .orderId("ORD-9999")
                .sku("SKU-RED-SHIRT-M")
                .quantity(5)
                .status(ReservationStatus.PENDING)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .build();

        when(stockReservationRepository.findByOrderIdAndStatus("ORD-9999", ReservationStatus.PENDING))
                .thenReturn(List.of(reservation));
        when(inventoryItemRepository.findBySku("SKU-RED-SHIRT-M")).thenReturn(Optional.of(testItem));

        ReservationResponse response = inventoryService.releaseStock(request);

        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(testItem.getAvailableQuantity()).isEqualTo(55);
        assertThat(testItem.getReservedQuantity()).isEqualTo(0);
        verify(stockReservationRepository).save(reservation);
        verify(distributedLockService).releaseLocks(anyList(), anyString());
    }

    @Test
    @DisplayName("getAllInventory: Returns paginated response")
    void testGetAllInventory() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<InventoryItem> page = new PageImpl<>(List.of(testItem), pageable, 1);
        when(inventoryItemRepository.findAll(pageable)).thenReturn(page);

        var response = inventoryService.getAllInventory(pageable);

        assertThat(response).isNotNull();
        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }
}
