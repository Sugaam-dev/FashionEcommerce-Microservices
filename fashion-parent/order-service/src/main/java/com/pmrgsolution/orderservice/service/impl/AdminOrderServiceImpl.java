package com.pmrgsolution.orderservice.service.impl;

import com.pmrgsolution.orderservice.dto.request.AddressRequest;
import com.pmrgsolution.orderservice.dto.request.AdminCreateOfflineOrderRequest;
import com.pmrgsolution.orderservice.dto.request.OfflineOrderItemRequest;
import com.pmrgsolution.orderservice.dto.request.ReleaseStockRequest;
import com.pmrgsolution.orderservice.dto.request.ReserveStockItemRequest;
import com.pmrgsolution.orderservice.dto.request.ReserveStockRequest;
import com.pmrgsolution.orderservice.dto.request.UpdateOrderStatusRequest;
import com.pmrgsolution.orderservice.dto.request.UpdateShippingConfigRequest;
import com.pmrgsolution.orderservice.dto.response.AdminDashboardStatsResponse;
import com.pmrgsolution.orderservice.dto.response.KeysetPageResponse;
import com.pmrgsolution.orderservice.dto.response.OrderResponse;
import com.pmrgsolution.orderservice.dto.response.OrderShippingConfigResponse;
import com.pmrgsolution.orderservice.dto.response.ReservationResponse;
import com.pmrgsolution.orderservice.entity.Order;
import com.pmrgsolution.orderservice.entity.OrderAddressSnapshot;
import com.pmrgsolution.orderservice.entity.OrderItem;
import com.pmrgsolution.orderservice.entity.OrderShippingConfig;
import com.pmrgsolution.orderservice.entity.OrderStatusHistory;
import com.pmrgsolution.orderservice.enums.OrderPaymentType;
import com.pmrgsolution.orderservice.enums.OrderStatus;
import com.pmrgsolution.orderservice.exception.InvalidOrderStateTransitionException;
import com.pmrgsolution.orderservice.exception.OrderNotFoundException;
import com.pmrgsolution.orderservice.feign.InventoryClient;
import com.pmrgsolution.orderservice.mapper.OrderMapper;
import com.pmrgsolution.orderservice.repository.CartItemRepository;
import com.pmrgsolution.orderservice.repository.OrderRepository;
import com.pmrgsolution.orderservice.repository.OrderShippingConfigRepository;
import com.pmrgsolution.orderservice.service.AdminOrderService;
import com.pmrgsolution.orderservice.service.AuditLogService;
import com.pmrgsolution.orderservice.service.OutboxEventPublisher;
import com.pmrgsolution.shared.core.response.PageResponse;
import com.pmrgsolution.shared.core.exception.ConflictException;
import com.pmrgsolution.shared.events.order.OrderCancelledEvent;
import com.pmrgsolution.shared.events.order.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderShippingConfigRepository shippingConfigRepository;
    private final OrderMapper orderMapper;
    private final AuditLogService auditLogService;
    private final OutboxEventPublisher outboxEventPublisher;
    private final InventoryClient inventoryClient;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_NUM_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${app.security.internal-service-key:fashion-internal-secret-key-2026-v1}")
    private String internalServiceKey;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> searchOrders(Specification<Order> spec, Pageable pageable) {
        log.debug("Admin searching orders with pagination: {}", pageable);
        Page<Order> page = orderRepository.findAll(spec, pageable);
        List<OrderResponse> content = page.getContent().stream().map(orderMapper::toOrderResponse).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public KeysetPageResponse<OrderResponse> getOrdersByCursor(UUID cursor, OrderStatus status, int size) {
        int limit = Math.min(Math.max(size, 1), 100);
        List<Order> orders = orderRepository.findAllByKeysetPagination(cursor, status, PageRequest.of(0, limit + 1));

        boolean hasMore = orders.size() > limit;
        List<Order> pageContent = hasMore ? orders.subList(0, limit) : orders;
        UUID nextCursorId = pageContent.isEmpty() ? null : pageContent.get(pageContent.size() - 1).getId();

        List<OrderResponse> dtos = pageContent.stream().map(orderMapper::toOrderResponse).toList();
        return new KeysetPageResponse<>(dtos, nextCursorId, hasMore);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        log.debug("Admin fetching order details for: {}", orderId);
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        return orderMapper.toOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest req, UUID adminId, String clientIp) {
        log.info("Admin {} updating order {} status to {}", adminId, orderId, req.status());

        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        validateStateTransition(order.getStatus(), req.status());

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(req.status());

        if (req.trackingNumber() != null && !req.trackingNumber().isBlank()) {
            order.setTrackingNumber(req.trackingNumber());
        }
        if (req.courierName() != null && !req.courierName().isBlank()) {
            order.setCourierName(req.courierName());
        }

        Instant now = Instant.now();
        switch (req.status()) {
            case PAYMENT_CONFIRMED -> {
                if (order.getPaidAt() == null) order.setPaidAt(now);
            }
            case SHIPPED -> {
                if (order.getShippedAt() == null) order.setShippedAt(now);
            }
            case DELIVERED -> {
                if (order.getDeliveredAt() == null) order.setDeliveredAt(now);
            }
            case CANCELLED -> {
                if (order.getCancelledAt() == null) order.setCancelledAt(now);
                try {
                    inventoryClient.releaseReservation(internalServiceKey, new ReleaseStockRequest(order.getOrderNumber()));
                } catch (Exception e) {
                    log.error("Failed to release stock for cancelled order {}: {}", order.getOrderNumber(), e.getMessage());
                }
            }
            default -> {}
        }

        String comment = req.statusNotes() != null && !req.statusNotes().isBlank()
                ? req.statusNotes()
                : "Status changed from " + oldStatus + " to " + req.status();

        OrderStatusHistory history = OrderStatusHistory.builder()
                .status(req.status())
                .comments(comment)
                .changedBy(adminId != null ? "ADMIN:" + adminId : "ADMIN")
                .build();
        order.addStatusHistory(history);

        Order saved = orderRepository.save(order);

        auditLogService.log("Order", saved.getId().toString(), "STATUS_UPDATED", oldStatus.name(),
                req.status().name(), adminId, "ROLE_ADMIN", clientIp);

        return orderMapper.toOrderResponse(saved);
    }

    @Override
    @Transactional
    public OrderResponse createOfflineOrder(AdminCreateOfflineOrderRequest req, UUID adminId, String clientIp) {
        log.info("Admin {} creating offline order for user {}", adminId, req.userId());

        String datePart = LocalDate.now().format(DATE_NUM_FORMATTER);
        int randomDigits = 1000 + RANDOM.nextInt(9000);
        String orderNumber = "ORD-" + datePart + "-" + randomDigits;

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        List<ReserveStockItemRequest> stockReserveItems = new ArrayList<>();
        List<OrderCreatedEvent.OrderItemInfo> eventItems = new ArrayList<>();

        for (OfflineOrderItemRequest itemReq : req.items()) {
            BigDecimal itemTotal = itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));
            subtotal = subtotal.add(itemTotal);

            OrderItem orderItem = OrderItem.builder()
                    .productId(itemReq.productId())
                    .productTitle(itemReq.title())
                    .sku(itemReq.sku())
                    .variantSize(itemReq.variantSize())
                    .variantColor(itemReq.variantColor())
                    .primaryImageUrl(itemReq.primaryImageUrl())
                    .unitPrice(itemReq.unitPrice())
                    .quantity(itemReq.quantity())
                    .totalPrice(itemTotal)
                    .build();

            orderItems.add(orderItem);
            stockReserveItems.add(new ReserveStockItemRequest(itemReq.sku(), itemReq.quantity()));
            eventItems.add(new OrderCreatedEvent.OrderItemInfo(
                    itemReq.sku(),
                    itemReq.productId(),
                    itemReq.title(),
                    itemReq.quantity(),
                    itemReq.unitPrice()
            ));
        }

        // Reserve stock in inventory
        try {
            ReservationResponse res = inventoryClient.reserveStock(
                    internalServiceKey,
                    UUID.randomUUID().toString(),
                    new ReserveStockRequest(stockReserveItems, orderNumber)
            );
            if (res != null && !res.success()) {
                throw new ConflictException("Stock reservation failed for offline order: " + res.message());
            }
        } catch (ConflictException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to reserve stock for offline order {}: {}", orderNumber, e.getMessage());
            throw new ConflictException("Inventory reservation failed for offline order items");
        }

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(req.userId())
                .status(OrderStatus.PAYMENT_CONFIRMED)
                .paymentType(OrderPaymentType.ADMIN_OFFLINE)
                .subtotal(subtotal)
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(req.totalAmount())
                .adminNotes(req.adminNotes())
                .paidAt(Instant.now())
                .build();

        for (OrderItem oi : orderItems) {
            order.addItem(oi);
        }

        AddressRequest addr = req.shippingAddress();
        OrderAddressSnapshot snapshot = OrderAddressSnapshot.builder()
                .recipientName(addr.recipientName())
                .recipientPhone(addr.recipientPhone())
                .addressLine1(addr.addressLine1())
                .addressLine2(addr.addressLine2())
                .landmark(addr.landmark())
                .city(addr.city())
                .state(addr.state())
                .postalCode(addr.postalCode())
                .country(addr.country())
                .build();
        order.setAddressSnapshot(snapshot);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .status(OrderStatus.PAYMENT_CONFIRMED)
                .comments("Offline order created directly by Admin: " + (adminId != null ? adminId : "SYSTEM"))
                .changedBy("ADMIN")
                .build();
        order.addStatusHistory(history);

        Order saved = orderRepository.save(order);

        // Stage Outbox event
        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                saved.getId().toString(),
                req.userId().toString(),
                saved.getTotalAmount(),
                eventItems
        );
        outboxEventPublisher.saveEvent("Order", saved.getId().toString(), "order.created.v1", orderCreatedEvent);

        auditLogService.log("Order", saved.getId().toString(), "CREATED_OFFLINE", null,
                orderMapper.toOrderResponse(saved), adminId, "ROLE_ADMIN", clientIp);

        return orderMapper.toOrderResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getDashboardStats() {
        log.debug("Generating admin dashboard statistics");

        long totalOrders = orderRepository.count();
        BigDecimal totalRevenue = orderRepository.sumTotalRevenue();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT);
        long processingOrders = orderRepository.countByStatus(OrderStatus.PROCESSING);
        long deliveredOrders = orderRepository.countByStatus(OrderStatus.DELIVERED);
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);

        Instant startOfDay = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
        BigDecimal revenueToday = orderRepository.sumRevenueSince(startOfDay);

        Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        long abandonedCartCount = cartItemRepository.countAbandonedCartsOlderThan(twentyFourHoursAgo);

        return new AdminDashboardStatsResponse(
                totalOrders,
                totalRevenue,
                pendingOrders,
                processingOrders,
                deliveredOrders,
                cancelledOrders,
                abandonedCartCount,
                revenueToday
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OrderShippingConfigResponse getShippingConfig() {
        OrderShippingConfig config = shippingConfigRepository.findAll()
                .stream()
                .findFirst()
                .orElseGet(() -> shippingConfigRepository.save(OrderShippingConfig.builder()
                        .defaultShippingFee(new BigDecimal("50.00"))
                        .freeShippingThreshold(new BigDecimal("999.00"))
                        .estimatedDeliveryMinDays(3)
                        .estimatedDeliveryMaxDays(7)
                        .build()));
        return orderMapper.toOrderShippingConfigResponse(config);
    }

    @Override
    @Transactional
    public OrderShippingConfigResponse updateShippingConfig(UpdateShippingConfigRequest req, UUID adminId, String clientIp) {
        log.info("Admin {} updating shipping configuration", adminId);

        OrderShippingConfig config = shippingConfigRepository.findAll()
                .stream()
                .findFirst()
                .orElse(OrderShippingConfig.builder().build());

        OrderShippingConfig oldConfig = OrderShippingConfig.builder()
                .defaultShippingFee(config.getDefaultShippingFee())
                .freeShippingThreshold(config.getFreeShippingThreshold())
                .estimatedDeliveryMinDays(config.getEstimatedDeliveryMinDays())
                .estimatedDeliveryMaxDays(config.getEstimatedDeliveryMaxDays())
                .build();

        config.setDefaultShippingFee(req.defaultShippingFee());
        config.setFreeShippingThreshold(req.freeShippingThreshold());
        config.setEstimatedDeliveryMinDays(req.estimatedDeliveryMinDays());
        config.setEstimatedDeliveryMaxDays(req.estimatedDeliveryMaxDays());

        OrderShippingConfig saved = shippingConfigRepository.save(config);

        auditLogService.log("OrderShippingConfig", saved.getId().toString(), "UPDATED",
                oldConfig, saved, adminId, "ROLE_ADMIN", clientIp);

        return orderMapper.toOrderShippingConfigResponse(saved);
    }

    private void validateStateTransition(OrderStatus current, OrderStatus next) {
        if (current == next) return;

        boolean valid = switch (current) {
            case PENDING_PAYMENT -> next == OrderStatus.PAYMENT_CONFIRMED || next == OrderStatus.CANCELLED;
            case PAYMENT_CONFIRMED -> next == OrderStatus.PROCESSING || next == OrderStatus.CANCELLED || next == OrderStatus.REFUNDED;
            case PROCESSING -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED || next == OrderStatus.REFUNDED;
            case SHIPPED -> next == OrderStatus.DELIVERED || next == OrderStatus.REFUNDED;
            case DELIVERED -> next == OrderStatus.REFUNDED;
            case CANCELLED, REFUNDED -> false;
        };

        if (!valid) {
            throw new InvalidOrderStateTransitionException("Invalid state transition from " + current + " to " + next);
        }
    }
}
