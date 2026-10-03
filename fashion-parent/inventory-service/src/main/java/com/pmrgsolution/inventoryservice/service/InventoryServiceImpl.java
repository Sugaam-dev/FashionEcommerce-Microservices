package com.pmrgsolution.inventoryservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.pmrgsolution.inventoryservice.mapper.InventoryMapper;
import com.pmrgsolution.inventoryservice.repository.InventoryItemRepository;
import com.pmrgsolution.inventoryservice.repository.StockReservationRepository;
import com.pmrgsolution.shared.core.exception.ConflictException;
import com.pmrgsolution.shared.core.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final String IDEMPOTENCY_PREFIX = "idempotency:reservation:";
    private static final String LOCK_PREFIX = "lock:inventory:sku:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(30);
    private static final int RESERVATION_EXPIRY_MINUTES = 15;

    private final InventoryItemRepository inventoryItemRepository;
    private final StockReservationRepository stockReservationRepository;
    private final DistributedLockService distributedLockService;
    private final InventoryEventProducer inventoryEventProducer;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public InventoryItemResponse createInventoryItem(CreateInventoryItemRequest request) {
        log.info("Creating inventory item for SKU: {}", request.sku());
        if (inventoryItemRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException("Inventory item with SKU already exists: " + request.sku());
        }

        InventoryItem item = InventoryMapper.toEntity(request);
        InventoryItem savedItem = inventoryItemRepository.save(item);
        log.info("Inventory item created successfully for SKU: {}", savedItem.getSku());
        return InventoryMapper.toResponse(savedItem);
    }

    @Override
    @Transactional
    public InventoryItemResponse adjustStock(String sku, StockAdjustmentRequest request) {
        String effectiveSku = (sku != null && !sku.isBlank()) ? sku : request.sku();
        log.info("Adjusting stock for SKU: {}, delta: {}, reason: {}", effectiveSku, request.adjustmentDelta(), request.reason());
        String lockKey = LOCK_PREFIX + effectiveSku;
        String lockToken = UUID.randomUUID().toString();

        boolean locked = distributedLockService.acquireLockWithRetry(lockKey, lockToken, LOCK_TTL, 5, 50);
        if (!locked) {
            throw new ConflictException("Could not acquire lock for SKU: " + effectiveSku + ". Please retry.");
        }

        try {
            InventoryItem item = inventoryItemRepository.findBySku(effectiveSku)
                    .orElseThrow(() -> new InventoryItemNotFoundException("Inventory item not found for SKU: " + effectiveSku));

            int newAvailable = item.getAvailableQuantity() + request.adjustmentDelta();
            if (newAvailable < 0) {
                throw new InsufficientStockException("Insufficient stock for SKU: " + effectiveSku + ", adjustment would result in negative available quantity");
            }

            item.setAvailableQuantity(newAvailable);

            if (item.isLowStock()) {
                inventoryEventProducer.publishLowStockAlert(item);
            }

            InventoryItem updated = inventoryItemRepository.save(item);
            log.info("Stock adjusted for SKU: {}, new available: {}", effectiveSku, updated.getAvailableQuantity());
            return InventoryMapper.toResponse(updated);
        } finally {
            distributedLockService.releaseLock(lockKey, lockToken);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItemResponse getStockBySku(String sku) {
        log.debug("Fetching inventory for SKU: {}", sku);
        InventoryItem item = inventoryItemRepository.findBySku(sku)
                .orElseThrow(() -> new InventoryItemNotFoundException("Inventory item not found for SKU: " + sku));
        return InventoryMapper.toResponse(item);
    }

    @Override
    @Transactional(readOnly = true)
    public BulkStockCheckResponse checkStock(BulkStockCheckRequest request) {
        log.debug("Checking bulk stock for {} SKUs", request.skus() != null ? request.skus().size() : 0);
        if (request.skus() == null || request.skus().isEmpty()) {
            return new BulkStockCheckResponse(Map.of());
        }

        List<InventoryItem> items = inventoryItemRepository.findBySkuIn(request.skus());
        Map<String, Integer> availableMap = (items != null) ? items.stream()
                .collect(Collectors.toMap(InventoryItem::getSku, InventoryItem::getAvailableQuantity, (a, b) -> a))
                : Map.of();

        Map<String, Integer> stockMap = new HashMap<>();
        for (String sku : request.skus()) {
            stockMap.put(sku, availableMap.getOrDefault(sku, 0));
        }

        return new BulkStockCheckResponse(stockMap);
    }

    @Override
    @Transactional
    public ReservationResponse reserveStock(ReserveStockRequest request, String idempotencyKey) {
        String effectiveKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : request.orderId();
        String redisKey = IDEMPOTENCY_PREFIX + effectiveKey;

        // 1. Idempotency Check
        String cached = stringRedisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            try {
                log.info("Returning cached reservation response for idempotency key: {}", effectiveKey);
                return objectMapper.readValue(cached, ReservationResponse.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse cached reservation response, proceeding to reserve: {}", e.getMessage());
            }
        }

        // 2. Sort items by SKU to eliminate deadlock risk during distributed locking
        List<StockItemReserveInfo> sortedItems = request.items().stream()
                .sorted(Comparator.comparing(StockItemReserveInfo::sku))
                .toList();

        List<String> lockKeys = sortedItems.stream()
                .map(item -> LOCK_PREFIX + item.sku())
                .toList();

        String lockToken = UUID.randomUUID().toString();
        boolean locked = distributedLockService.acquireLocks(lockKeys, lockToken, LOCK_TTL, 10, 50);
        if (!locked) {
            throw new ConflictException("Could not acquire lock for inventory SKUs. Please retry.");
        }

        try {
            Instant expiresAt = Instant.now().plus(Duration.ofMinutes(RESERVATION_EXPIRY_MINUTES));
            List<StockReservation> reservationsToSave = new ArrayList<>();

            // 3. Stock validation pass
            for (StockItemReserveInfo itemInfo : sortedItems) {
                InventoryItem item = inventoryItemRepository.findBySku(itemInfo.sku())
                        .orElseThrow(() -> new InventoryItemNotFoundException("Inventory item not found for SKU: " + itemInfo.sku()));

                if (item.getAvailableQuantity() < itemInfo.quantity()) {
                    throw new InsufficientStockException("Insufficient stock for SKU: " + itemInfo.sku()
                            + ". Available: " + item.getAvailableQuantity() + ", Requested: " + itemInfo.quantity());
                }
            }

            // 4. Stock reservation pass
            for (StockItemReserveInfo itemInfo : sortedItems) {
                InventoryItem item = inventoryItemRepository.findBySku(itemInfo.sku()).orElseThrow();
                item.setAvailableQuantity(item.getAvailableQuantity() - itemInfo.quantity());
                item.setReservedQuantity(item.getReservedQuantity() + itemInfo.quantity());

                if (item.isLowStock()) {
                    inventoryEventProducer.publishLowStockAlert(item);
                }

                inventoryItemRepository.save(item);

                StockReservation reservation = StockReservation.builder()
                        .orderId(request.orderId())
                        .sku(itemInfo.sku())
                        .quantity(itemInfo.quantity())
                        .status(ReservationStatus.PENDING)
                        .expiresAt(expiresAt)
                        .build();

                reservationsToSave.add(reservation);
            }

            stockReservationRepository.saveAll(reservationsToSave);

            ReservationResponse response = new ReservationResponse(
                    request.orderId(),
                    true,
                    "Stock reserved successfully",
                    expiresAt
            );

            try {
                stringRedisTemplate.opsForValue().set(
                        redisKey,
                        objectMapper.writeValueAsString(response),
                        IDEMPOTENCY_TTL
                );
            } catch (JsonProcessingException e) {
                log.warn("Failed to cache reservation response in Redis: {}", e.getMessage());
            }

            log.info("Stock successfully reserved for orderId: {}, itemsCount: {}", request.orderId(), sortedItems.size());
            return response;
        } finally {
            distributedLockService.releaseLocks(lockKeys, lockToken);
        }
    }

    @Override
    @Transactional
    public ReservationResponse confirmStock(ConfirmStockRequest request) {
        log.info("Confirming stock reservations for orderId: {}", request.orderId());
        List<StockReservation> pendingReservations = stockReservationRepository.findByOrderIdAndStatus(
                request.orderId(),
                ReservationStatus.PENDING
        );

        if (pendingReservations.isEmpty()) {
            List<StockReservation> allReservations = stockReservationRepository.findByOrderId(request.orderId());
            if (allReservations.isEmpty()) {
                throw new ReservationNotFoundException("No stock reservation found for orderId: " + request.orderId());
            }
            log.info("Stock reservations already confirmed/processed for orderId: {}", request.orderId());
            return new ReservationResponse(request.orderId(), true, "Stock already confirmed", null);
        }

        List<String> lockKeys = pendingReservations.stream()
                .map(r -> LOCK_PREFIX + r.getSku())
                .distinct()
                .sorted()
                .toList();

        String lockToken = UUID.randomUUID().toString();
        boolean locked = distributedLockService.acquireLocks(lockKeys, lockToken, LOCK_TTL, 10, 50);
        if (!locked) {
            throw new ConflictException("Could not acquire lock for inventory SKUs. Please retry.");
        }

        try {
            for (StockReservation reservation : pendingReservations) {
                InventoryItem item = inventoryItemRepository.findBySku(reservation.getSku())
                        .orElseThrow(() -> new InventoryItemNotFoundException("Inventory item not found for SKU: " + reservation.getSku()));

                item.setReservedQuantity(Math.max(0, item.getReservedQuantity() - reservation.getQuantity()));
                inventoryItemRepository.save(item);

                reservation.setStatus(ReservationStatus.CONFIRMED);
                stockReservationRepository.save(reservation);
            }

            log.info("Stock confirmed permanently for orderId: {}", request.orderId());
            return new ReservationResponse(request.orderId(), true, "Stock reservation confirmed successfully", null);
        } finally {
            distributedLockService.releaseLocks(lockKeys, lockToken);
        }
    }

    @Override
    @Transactional
    public ReservationResponse releaseStock(ReleaseStockRequest request) {
        log.info("Releasing stock reservations for orderId: {}, reason: {}", request.orderId(), request.reason());
        List<StockReservation> pendingReservations = stockReservationRepository.findByOrderIdAndStatus(
                request.orderId(),
                ReservationStatus.PENDING
        );

        if (pendingReservations.isEmpty()) {
            List<StockReservation> allReservations = stockReservationRepository.findByOrderId(request.orderId());
            if (allReservations.isEmpty()) {
                throw new ReservationNotFoundException("No stock reservation found for orderId: " + request.orderId());
            }
            log.info("Stock reservations already released/processed for orderId: {}", request.orderId());
            return new ReservationResponse(request.orderId(), true, "Stock already released", null);
        }

        List<String> lockKeys = pendingReservations.stream()
                .map(r -> LOCK_PREFIX + r.getSku())
                .distinct()
                .sorted()
                .toList();

        String lockToken = UUID.randomUUID().toString();
        boolean locked = distributedLockService.acquireLocks(lockKeys, lockToken, LOCK_TTL, 10, 50);
        if (!locked) {
            throw new ConflictException("Could not acquire lock for inventory SKUs. Please retry.");
        }

        try {
            for (StockReservation reservation : pendingReservations) {
                InventoryItem item = inventoryItemRepository.findBySku(reservation.getSku())
                        .orElseThrow(() -> new InventoryItemNotFoundException("Inventory item not found for SKU: " + reservation.getSku()));

                item.setAvailableQuantity(item.getAvailableQuantity() + reservation.getQuantity());
                item.setReservedQuantity(Math.max(0, item.getReservedQuantity() - reservation.getQuantity()));
                inventoryItemRepository.save(item);

                reservation.setStatus(ReservationStatus.RELEASED);
                stockReservationRepository.save(reservation);
            }

            log.info("Stock released back to inventory for orderId: {}", request.orderId());
            return new ReservationResponse(request.orderId(), true, "Stock reservation released successfully", null);
        } finally {
            distributedLockService.releaseLocks(lockKeys, lockToken);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryItemResponse> getAllInventory(Pageable pageable) {
        return PageResponse.from(inventoryItemRepository.findAll(pageable).map(InventoryMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryItemResponse> getLowStockItems(Pageable pageable) {
        return PageResponse.from(inventoryItemRepository.findLowStockItems(pageable).map(InventoryMapper::toResponse));
    }
}
