package com.pmrgsolution.orderservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrgsolution.orderservice.dto.request.CancelOrderRequest;
import com.pmrgsolution.orderservice.dto.request.CreateOrderRequest;
import com.pmrgsolution.orderservice.dto.request.RedeemCouponRequest;
import com.pmrgsolution.orderservice.dto.request.ReleaseStockRequest;
import com.pmrgsolution.orderservice.dto.request.ReserveStockItemRequest;
import com.pmrgsolution.orderservice.dto.request.ReserveStockRequest;
import com.pmrgsolution.orderservice.dto.request.ValidateCouponRequest;
import com.pmrgsolution.orderservice.dto.response.AddressResponse;
import com.pmrgsolution.orderservice.dto.response.CouponValidationResponse;
import com.pmrgsolution.orderservice.dto.response.OrderItemResponse;
import com.pmrgsolution.orderservice.dto.response.OrderResponse;
import com.pmrgsolution.orderservice.dto.response.ProductDetailResponse;
import com.pmrgsolution.orderservice.dto.response.ReservationResponse;
import com.pmrgsolution.orderservice.dto.response.VariantResponse;
import com.pmrgsolution.orderservice.entity.CartItem;
import com.pmrgsolution.orderservice.entity.Order;
import com.pmrgsolution.orderservice.entity.OrderAddressSnapshot;
import com.pmrgsolution.orderservice.entity.OrderItem;
import com.pmrgsolution.orderservice.entity.OrderShippingConfig;
import com.pmrgsolution.orderservice.entity.OrderStatusHistory;
import com.pmrgsolution.orderservice.enums.OrderStatus;
import com.pmrgsolution.orderservice.exception.CartEmptyException;
import com.pmrgsolution.orderservice.exception.InvalidOrderStateTransitionException;
import com.pmrgsolution.orderservice.exception.OrderNotFoundException;
import com.pmrgsolution.orderservice.feign.AuthClient;
import com.pmrgsolution.orderservice.feign.CatalogClient;
import com.pmrgsolution.orderservice.feign.CouponClient;
import com.pmrgsolution.orderservice.feign.InventoryClient;
import com.pmrgsolution.orderservice.mapper.OrderMapper;
import com.pmrgsolution.orderservice.repository.CartItemRepository;
import com.pmrgsolution.orderservice.repository.OrderRepository;
import com.pmrgsolution.orderservice.repository.OrderShippingConfigRepository;
import com.pmrgsolution.orderservice.service.AuditLogService;
import com.pmrgsolution.orderservice.service.OrderService;
import com.pmrgsolution.orderservice.service.OutboxEventPublisher;
import com.pmrgsolution.orderservice.service.PdfInvoiceService;
import com.pmrgsolution.shared.core.response.PageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import com.pmrgsolution.shared.core.exception.BadRequestException;
import com.pmrgsolution.shared.core.exception.ConflictException;
import com.pmrgsolution.shared.core.exception.ForbiddenException;
import com.pmrgsolution.shared.core.exception.ResourceNotFoundException;
import com.pmrgsolution.shared.events.order.OrderCancelledEvent;
import com.pmrgsolution.shared.events.order.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderShippingConfigRepository shippingConfigRepository;
    private final OrderMapper orderMapper;
    private final PdfInvoiceService pdfInvoiceService;
    private final AuditLogService auditLogService;
    private final OutboxEventPublisher outboxEventPublisher;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private final InventoryClient inventoryClient;
    private final CouponClient couponClient;
    private final AuthClient authClient;
    private final CatalogClient catalogClient;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_NUM_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String IDEMPOTENCY_PREFIX = "order:idempotency:";

    @Value("${app.security.internal-service-key:fashion-internal-secret-key-2026-v1}")
    private String internalServiceKey;

    @Autowired(required = false)
    private TransactionTemplate transactionTemplate;

    @Override
    public OrderResponse createOrder(UUID userId, CreateOrderRequest req, String idempotencyKey, String clientIp) {
        log.info("Creating order for user: {}, idempotencyKey: {}", userId, idempotencyKey);

        // 1. Idempotency Check via Redis
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String cachedOrderJson = redisTemplate.opsForValue().get(IDEMPOTENCY_PREFIX + idempotencyKey);
            if (cachedOrderJson != null) {
                try {
                    log.info("Idempotent hit for key: {}, returning cached order", idempotencyKey);
                    return objectMapper.readValue(cachedOrderJson, OrderResponse.class);
                } catch (Exception e) {
                    log.warn("Failed to deserialize cached order response for key: {}", idempotencyKey, e);
                }
            }
        }

        // 2. Load Cart Items
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        if (cartItems.isEmpty()) {
            throw new CartEmptyException("Cart is empty. Cannot place an order.");
        }

        // 3. Fetch Shipping Address Snapshot from auth-service
        AddressResponse addressResponse;
        try {
            addressResponse = authClient.getAddress(internalServiceKey, req.addressId(), userId);
        } catch (Exception e) {
            log.error("Failed to fetch address {} for user {}: {}", req.addressId(), userId, e.getMessage());
            throw new ResourceNotFoundException("Shipping address not found or does not belong to user");
        }

        if (addressResponse == null || (addressResponse.userId() != null && !userId.equals(addressResponse.userId()))) {
            throw new ResourceNotFoundException("Shipping address not found for user: " + userId);
        }

        // 4. Load Shipping Configuration
        OrderShippingConfig shippingConfig = shippingConfigRepository.findAll()
                .stream()
                .findFirst()
                .orElse(OrderShippingConfig.builder()
                        .defaultShippingFee(new BigDecimal("50.00"))
                        .freeShippingThreshold(new BigDecimal("999.00"))
                        .estimatedDeliveryMinDays(3)
                        .estimatedDeliveryMaxDays(7)
                        .build());

        // 5. Build Order Items & Compute Subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        List<ReserveStockItemRequest> stockReserveItems = new ArrayList<>();
        List<OrderCreatedEvent.OrderItemInfo> eventItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            String title = "Fashion Item";
            String size = "Standard";
            String color = "Standard";
            String primaryImage = null;
            BigDecimal unitPrice = new BigDecimal("499.00");

            try {
                ProductDetailResponse product = catalogClient.getProductById(UUID.fromString(cartItem.getProductId()));
                if (product != null) {
                    title = product.title();
                    primaryImage = product.primaryImageUrl();
                    if (product.variants() != null) {
                        Optional<VariantResponse> variantOpt = product.variants().stream()
                                .filter(v -> cartItem.getSku().equalsIgnoreCase(v.sku()))
                                .findFirst();
                        if (variantOpt.isPresent()) {
                            VariantResponse v = variantOpt.get();
                            size = v.size();
                            color = v.color();
                            if (v.price() != null && v.price().compareTo(BigDecimal.ZERO) > 0) {
                                unitPrice = v.price();
                            }
                        } else if (product.salePrice() != null) {
                            unitPrice = product.salePrice();
                        } else if (product.basePrice() != null) {
                            unitPrice = product.basePrice();
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Catalog fetch failed for product {}: {}, using fallback pricing", cartItem.getProductId(), e.getMessage());
            }

            BigDecimal itemTotalPrice = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(itemTotalPrice);

            OrderItem orderItem = OrderItem.builder()
                    .productId(cartItem.getProductId())
                    .productTitle(title)
                    .sku(cartItem.getSku())
                    .variantSize(size)
                    .variantColor(color)
                    .primaryImageUrl(primaryImage)
                    .unitPrice(unitPrice)
                    .quantity(cartItem.getQuantity())
                    .totalPrice(itemTotalPrice)
                    .build();

            orderItems.add(orderItem);
            stockReserveItems.add(new ReserveStockItemRequest(cartItem.getSku(), cartItem.getQuantity()));
            eventItems.add(new OrderCreatedEvent.OrderItemInfo(
                    cartItem.getSku(),
                    cartItem.getProductId(),
                    title,
                    cartItem.getQuantity(),
                    unitPrice
            ));
        }

        // 6. Validate Coupon (if provided)
        BigDecimal discountAmount = BigDecimal.ZERO;
        String couponCode = (req.couponCode() != null && !req.couponCode().isBlank()) ? req.couponCode().trim().toUpperCase() : null;
        if (couponCode != null) {
            try {
                CouponValidationResponse couponRes = couponClient.validateCoupon(new ValidateCouponRequest(couponCode, subtotal, userId));
                if (couponRes != null && couponRes.valid()) {
                    discountAmount = couponRes.discountAmount() != null ? couponRes.discountAmount() : BigDecimal.ZERO;
                } else {
                    String msg = couponRes != null ? couponRes.message() : "Coupon is invalid";
                    throw new BadRequestException("Coupon validation failed: " + msg);
                }
            } catch (BadRequestException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error communicating with coupon-service: {}", e.getMessage());
                throw new BadRequestException("Failed to validate coupon: " + couponCode);
            }
        }

        // 7. Calculate Shipping & Tax & Total
        BigDecimal shippingFee = BigDecimal.ZERO;
        if (subtotal.compareTo(shippingConfig.getFreeShippingThreshold()) < 0) {
            shippingFee = shippingConfig.getDefaultShippingFee();
        }

        // Default Tax (5% GST calculated on taxable subtotal after discount)
        BigDecimal taxableAmount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);
        BigDecimal taxAmount = taxableAmount.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = taxableAmount.add(shippingFee).add(taxAmount);

        // 8. Generate Order Number: ORD-YYYYMMDD-XXXX
        String datePart = LocalDate.now().format(DATE_NUM_FORMATTER);
        int randomDigits = 1000 + RANDOM.nextInt(9000);
        String orderNumber = "ORD-" + datePart + "-" + randomDigits;

        // 9. Reserve Stock via inventory-service
        try {
            String reserveIdempotencyKey = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
            ReservationResponse reservationResponse = inventoryClient.reserveStock(
                    internalServiceKey,
                    reserveIdempotencyKey,
                    new ReserveStockRequest(stockReserveItems, orderNumber)
            );
            if (reservationResponse != null && !reservationResponse.success()) {
                throw new ConflictException("Stock reservation failed: " + reservationResponse.message());
            }
        } catch (ConflictException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to reserve stock with inventory-service for order {}: {}", orderNumber, e.getMessage());
            throw new ConflictException("Unable to reserve inventory for selected items. Please try again.");
        }

        // 10. Persist Order, Items, Address Snapshot, Status History and Clear Cart in minimal transactional boundary
        final BigDecimal finalSubtotal = subtotal;
        final BigDecimal finalDiscountAmount = discountAmount;
        final BigDecimal finalShippingFee = shippingFee;
        final BigDecimal finalTaxAmount = taxAmount;
        Order savedOrder;
        try {
            if (transactionTemplate != null) {
                savedOrder = transactionTemplate.execute(status -> persistOrderAndClearCart(
                        orderNumber, userId, req, finalSubtotal, finalDiscountAmount, finalShippingFee, finalTaxAmount,
                        totalAmount, couponCode, orderItems, addressResponse, eventItems
                ));
            } else {
                savedOrder = persistOrderAndClearCart(
                        orderNumber, userId, req, finalSubtotal, finalDiscountAmount, finalShippingFee, finalTaxAmount,
                        totalAmount, couponCode, orderItems, addressResponse, eventItems
                );
            }
        } catch (Exception e) {
            log.error("Failed to persist order {}: {}, releasing inventory reservation", orderNumber, e.getMessage());
            try {
                inventoryClient.releaseReservation(internalServiceKey, new ReleaseStockRequest(orderNumber));
            } catch (Exception rex) {
                log.warn("Failed to release inventory reservation for order {}: {}", orderNumber, rex.getMessage());
            }
            throw e;
        }

        // 11. Redeem Coupon if applied (non-blocking post-persistence)
        if (couponCode != null) {
            try {
                couponClient.redeemCoupon(
                        orderNumber,
                        new RedeemCouponRequest(couponCode, subtotal, userId, savedOrder.getId().toString())
                );
            } catch (Exception e) {
                log.warn("Coupon redeem call warning for order {}: {}", orderNumber, e.getMessage());
            }
        }

        // 14. Map Response & Cache for Idempotency
        OrderResponse response = orderMapper.toOrderResponse(savedOrder);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                redisTemplate.opsForValue().set(
                        IDEMPOTENCY_PREFIX + idempotencyKey,
                        objectMapper.writeValueAsString(response),
                        Duration.ofHours(1)
                );
            } catch (Exception e) {
                log.warn("Failed to cache idempotency result for key: {}", idempotencyKey, e);
            }
        }

        // 15. Audit Log
        auditLogService.log("Order", savedOrder.getId().toString(), "CREATED", null, response,
                userId, "ROLE_CUSTOMER", clientIp);

        log.info("Order successfully placed: {} for user: {}", orderNumber, userId);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(UUID userId, Pageable pageable) {
        log.debug("Fetching orders for user: {}", userId);
        Page<Order> page = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<OrderResponse> list = page.getContent().stream().map(orderMapper::toOrderResponse).toList();
        return new PageResponse<>(list, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID userId, UUID orderId) {
        log.debug("Fetching order {} for user: {}", orderId, userId);
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        return orderMapper.toOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID userId, UUID orderId, CancelOrderRequest req, String clientIp) {
        log.info("User {} requested cancellation for order {}", userId, orderId);

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.PAYMENT_CONFIRMED) {
            throw new InvalidOrderStateTransitionException("Order cannot be cancelled in status: " + order.getStatus());
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        order.setCancellationReason(req.cancellationReason());

        OrderStatusHistory history = OrderStatusHistory.builder()
                .status(OrderStatus.CANCELLED)
                .comments("Cancelled by customer: " + req.cancellationReason())
                .changedBy("CUSTOMER")
                .build();
        order.addStatusHistory(history);

        Order savedOrder = orderRepository.save(order);

        // Release inventory reservation
        try {
            inventoryClient.releaseReservation(internalServiceKey, new ReleaseStockRequest(order.getOrderNumber()));
        } catch (Exception e) {
            log.error("Failed to release inventory for cancelled order {}: {}", order.getOrderNumber(), e.getMessage());
        }

        // Publish OrderCancelledEvent via Outbox
        OrderCancelledEvent cancelledEvent = new OrderCancelledEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                savedOrder.getId().toString(),
                userId.toString(),
                req.cancellationReason()
        );
        outboxEventPublisher.saveEvent("Order", savedOrder.getId().toString(), "order.cancelled.v1", cancelledEvent);

        auditLogService.log("Order", savedOrder.getId().toString(), "CANCELLED", oldStatus.name(),
                OrderStatus.CANCELLED.name(), userId, "ROLE_CUSTOMER", clientIp);

        return orderMapper.toOrderResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadInvoice(UUID userId, UUID orderId) {
        log.info("Downloading invoice for order {} by user {}", orderId, userId);

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT || order.getStatus() == OrderStatus.CANCELLED) {
            throw new ForbiddenException("Invoice is available only after payment confirmation.");
        }

        return pdfInvoiceService.generateInvoice(order);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPurchasedProduct(UUID userId, String productId) {
        return orderRepository.hasPurchasedProduct(userId, productId);
    }

    private Order persistOrderAndClearCart(
            String orderNumber,
            UUID userId,
            CreateOrderRequest req,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal shippingFee,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String couponCode,
            List<OrderItem> orderItems,
            AddressResponse addressResponse,
            List<OrderCreatedEvent.OrderItemInfo> eventItems
    ) {
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(userId)
                .status(OrderStatus.PENDING_PAYMENT)
                .paymentType(req.paymentType())
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .shippingFee(shippingFee)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .couponCode(couponCode)
                .customerNotes(req.customerNotes())
                .build();

        for (OrderItem item : orderItems) {
            order.addItem(item);
        }

        OrderAddressSnapshot addressSnapshot = OrderAddressSnapshot.builder()
                .recipientName(addressResponse.recipientName())
                .recipientPhone(addressResponse.recipientPhone())
                .addressLine1(addressResponse.addressLine1())
                .addressLine2(addressResponse.addressLine2())
                .landmark(addressResponse.landmark())
                .city(addressResponse.city())
                .state(addressResponse.state())
                .postalCode(addressResponse.postalCode())
                .country(addressResponse.country())
                .build();
        order.setAddressSnapshot(addressSnapshot);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .status(OrderStatus.PENDING_PAYMENT)
                .comments("Order created, awaiting payment confirmation")
                .changedBy("CUSTOMER")
                .build();
        order.addStatusHistory(history);

        Order savedOrder = orderRepository.save(order);

        // Clear User Cart
        cartItemRepository.deleteByUserId(userId);

        // Stage Transactional Outbox Event
        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                savedOrder.getId().toString(),
                userId.toString(),
                savedOrder.getTotalAmount(),
                eventItems
        );
        outboxEventPublisher.saveEvent("Order", savedOrder.getId().toString(), "order.created.v1", orderCreatedEvent);

        return savedOrder;
    }
}
