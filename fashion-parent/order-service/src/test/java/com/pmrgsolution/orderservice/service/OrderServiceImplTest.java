package com.pmrgsolution.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrgsolution.orderservice.dto.request.CancelOrderRequest;
import com.pmrgsolution.orderservice.dto.request.CreateOrderRequest;
import com.pmrgsolution.orderservice.dto.request.ReleaseStockRequest;
import com.pmrgsolution.orderservice.dto.request.ReserveStockRequest;
import com.pmrgsolution.orderservice.dto.response.AddressResponse;
import com.pmrgsolution.orderservice.dto.response.CouponValidationResponse;
import com.pmrgsolution.orderservice.dto.response.OrderResponse;
import com.pmrgsolution.orderservice.dto.response.ProductDetailResponse;
import com.pmrgsolution.orderservice.dto.response.ReservationResponse;
import com.pmrgsolution.orderservice.dto.response.VariantResponse;
import com.pmrgsolution.orderservice.entity.CartItem;
import com.pmrgsolution.orderservice.entity.Order;
import com.pmrgsolution.orderservice.entity.OrderAddressSnapshot;
import com.pmrgsolution.orderservice.entity.OrderItem;
import com.pmrgsolution.orderservice.entity.OrderShippingConfig;
import com.pmrgsolution.orderservice.enums.OrderPaymentType;
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
import com.pmrgsolution.orderservice.service.impl.OrderServiceImpl;
import com.pmrgsolution.shared.core.exception.BadRequestException;
import com.pmrgsolution.shared.core.exception.ConflictException;
import com.pmrgsolution.shared.core.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private OrderShippingConfigRepository shippingConfigRepository;

    @Spy
    private OrderMapper orderMapper = new OrderMapper();

    @Mock
    private PdfInvoiceService pdfInvoiceService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private CouponClient couponClient;

    @Mock
    private AuthClient authClient;

    @Mock
    private CatalogClient catalogClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID userId;
    private UUID addressId;
    private UUID productId;
    private CartItem cartItem;
    private AddressResponse addressResponse;
    private OrderShippingConfig shippingConfig;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        addressId = UUID.randomUUID();
        productId = UUID.randomUUID();

        cartItem = CartItem.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .sku("TSHIRT-RED-L")
                .productId(productId.toString())
                .quantity(2)
                .build();

        addressResponse = new AddressResponse(
                addressId,
                userId,
                "SHIPPING",
                "John Doe",
                "+919876543210",
                "123 Fashion Street",
                "Suite 4B",
                "Near City Mall",
                "Mumbai",
                "Maharashtra",
                "400001",
                "India",
                true
        );

        shippingConfig = OrderShippingConfig.builder()
                .id(UUID.randomUUID())
                .defaultShippingFee(new BigDecimal("50.00"))
                .freeShippingThreshold(new BigDecimal("999.00"))
                .estimatedDeliveryMinDays(3)
                .estimatedDeliveryMaxDays(7)
                .build();
    }

    @Test
    @DisplayName("createOrder - with empty cart throws CartEmptyException")
    void createOrder_withEmptyCart_throwsCartEmptyException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(cartItemRepository.findByUserIdOrderByUpdatedAtDesc(userId)).thenReturn(Collections.emptyList());

        CreateOrderRequest req = new CreateOrderRequest(addressId, OrderPaymentType.ONLINE_GATEWAY, null, null);

        assertThatThrownBy(() -> orderService.createOrder(userId, req, "idem-1", "127.0.0.1"))
                .isInstanceOf(CartEmptyException.class);
    }

    @Test
    @DisplayName("createOrder - with invalid coupon throws BadRequestException")
    void createOrder_withInvalidCoupon_throwsBadRequestException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(cartItemRepository.findByUserIdOrderByUpdatedAtDesc(userId)).thenReturn(List.of(cartItem));
        when(authClient.getAddress(any(), eq(addressId), eq(userId))).thenReturn(addressResponse);
        when(shippingConfigRepository.findAll()).thenReturn(List.of(shippingConfig));

        when(catalogClient.getProductById(productId)).thenReturn(new ProductDetailResponse(
                productId, "Silk Shirt", "silk-shirt", "SKU-", "Short desc", "Desc", "Brand", "ACTIVE",
                new BigDecimal("500.00"), new BigDecimal("500.00"), List.of(), List.of(), null, 4.5, 10,
                List.of(new VariantResponse(UUID.randomUUID(), "TSHIRT-RED-L", "L", "Red", "#FF0000", new BigDecimal("500.00"), null, null, true))
        ));

        when(couponClient.validateCoupon(any())).thenReturn(new CouponValidationResponse(
                false, "INVALID10", "FLAT", BigDecimal.ZERO, BigDecimal.ZERO, "Coupon has expired"
        ));

        CreateOrderRequest req = new CreateOrderRequest(addressId, OrderPaymentType.ONLINE_GATEWAY, "INVALID10", null);

        assertThatThrownBy(() -> orderService.createOrder(userId, req, "idem-1", "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Coupon validation failed");
    }

    @Test
    @DisplayName("createOrder - when inventory reservation fails throws ConflictException")
    void createOrder_inventoryReserveFails_throwsConflictException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(cartItemRepository.findByUserIdOrderByUpdatedAtDesc(userId)).thenReturn(List.of(cartItem));
        when(authClient.getAddress(any(), eq(addressId), eq(userId))).thenReturn(addressResponse);
        when(shippingConfigRepository.findAll()).thenReturn(List.of(shippingConfig));

        when(catalogClient.getProductById(productId)).thenReturn(new ProductDetailResponse(
                productId, "Silk Shirt", "silk-shirt", "SKU-", "Short desc", "Desc", "Brand", "ACTIVE",
                new BigDecimal("500.00"), new BigDecimal("500.00"), List.of(), List.of(), null, 4.5, 10,
                List.of(new VariantResponse(UUID.randomUUID(), "TSHIRT-RED-L", "L", "Red", "#FF0000", new BigDecimal("500.00"), null, null, true))
        ));

        when(inventoryClient.reserveStock(any(), any(), any(ReserveStockRequest.class)))
                .thenReturn(new ReservationResponse(false, "Item TSHIRT-RED-L is out of stock", null));

        CreateOrderRequest req = new CreateOrderRequest(addressId, OrderPaymentType.ONLINE_GATEWAY, null, null);

        assertThatThrownBy(() -> orderService.createOrder(userId, req, "idem-1", "127.0.0.1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Stock reservation failed");
    }

    @Test
    @DisplayName("createOrder - with valid idempotencyKey in cache returns cached response without DB hit")
    void createOrder_withIdempotencyKey_returnsCachedOrder() throws Exception {
        OrderResponse cached = new OrderResponse(
                UUID.randomUUID(), "ORD-20261008-1111", userId, OrderStatus.PENDING_PAYMENT,
                OrderPaymentType.ONLINE_GATEWAY, new BigDecimal("1000.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("50.00"), new BigDecimal("1050.00"),
                null, null, null, null, null, List.of(), null, List.of(),
                null, null, null, null, null, Instant.now(), Instant.now()
        );

        String cachedJson = objectMapper.writeValueAsString(cached);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("order:idempotency:idem-cached")).thenReturn(cachedJson);

        CreateOrderRequest req = new CreateOrderRequest(addressId, OrderPaymentType.ONLINE_GATEWAY, null, null);
        OrderResponse result = orderService.createOrder(userId, req, "idem-cached", "127.0.0.1");

        assertThat(result.orderNumber()).isEqualTo("ORD-20261008-1111");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOrder - successful checkout flow persists order, clears cart, and stages outbox event")
    void createOrder_successfulFlow_persistsOrderAndStagesOutbox() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(cartItemRepository.findByUserIdOrderByUpdatedAtDesc(userId)).thenReturn(List.of(cartItem));
        when(authClient.getAddress(any(), eq(addressId), eq(userId))).thenReturn(addressResponse);
        when(shippingConfigRepository.findAll()).thenReturn(List.of(shippingConfig));

        when(catalogClient.getProductById(productId)).thenReturn(new ProductDetailResponse(
                productId, "Silk Shirt", "silk-shirt", "SKU-", "Short desc", "Desc", "Brand", "ACTIVE",
                new BigDecimal("500.00"), new BigDecimal("500.00"), List.of(), List.of(), null, 4.5, 10,
                List.of(new VariantResponse(UUID.randomUUID(), "TSHIRT-RED-L", "L", "Red", "#FF0000", new BigDecimal("500.00"), null, null, true))
        ));

        when(inventoryClient.reserveStock(any(), any(), any(ReserveStockRequest.class)))
                .thenReturn(new ReservationResponse(true, "Stock reserved", Instant.now().plusSeconds(900)));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        CreateOrderRequest req = new CreateOrderRequest(addressId, OrderPaymentType.ONLINE_GATEWAY, null, "Please pack carefully");
        OrderResponse response = orderService.createOrder(userId, req, "idem-fresh", "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.subtotal()).isEqualByComparingTo("1000.00");
        assertThat(response.shippingFee()).isEqualByComparingTo("0.00"); // 1000 >= 999 free threshold

        verify(orderRepository).save(any(Order.class));
        verify(cartItemRepository).deleteByUserId(userId);
        verify(outboxEventPublisher).saveEvent(eq("Order"), any(), eq("order.created.v1"), any());
    }

    @Test
    @DisplayName("cancelOrder - non-cancellable state (SHIPPED/DELIVERED) throws InvalidOrderStateTransitionException")
    void cancelOrder_inNonCancellableState_throwsInvalidOrderStateTransitionException() {
        Order shippedOrder = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .orderNumber("ORD-20261008-2222")
                .status(OrderStatus.SHIPPED)
                .build();

        when(orderRepository.findByIdAndUserId(shippedOrder.getId(), userId)).thenReturn(Optional.of(shippedOrder));

        CancelOrderRequest req = new CancelOrderRequest("Changed my mind");

        assertThatThrownBy(() -> orderService.cancelOrder(userId, shippedOrder.getId(), req, "127.0.0.1"))
                .isInstanceOf(InvalidOrderStateTransitionException.class);
    }

    @Test
    @DisplayName("cancelOrder - in PENDING_PAYMENT releases inventory and publishes event")
    void cancelOrder_inPendingPayment_releasesInventoryAndPublishesEvent() {
        Order pendingOrder = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .orderNumber("ORD-20261008-3333")
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        when(orderRepository.findByIdAndUserId(pendingOrder.getId(), userId)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder);

        CancelOrderRequest req = new CancelOrderRequest("Ordered wrong size");
        OrderResponse response = orderService.cancelOrder(userId, pendingOrder.getId(), req, "127.0.0.1");

        assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(pendingOrder.getCancellationReason()).isEqualTo("Ordered wrong size");

        verify(inventoryClient).releaseReservation(any(), any(ReleaseStockRequest.class));
        verify(outboxEventPublisher).saveEvent(eq("Order"), eq(pendingOrder.getId().toString()), eq("order.cancelled.v1"), any());
    }

    @Test
    @DisplayName("downloadInvoice - for PENDING_PAYMENT order throws ForbiddenException")
    void downloadInvoice_forPendingOrder_throwsForbiddenException() {
        Order pendingOrder = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        when(orderRepository.findByIdAndUserId(pendingOrder.getId(), userId)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.downloadInvoice(userId, pendingOrder.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("downloadInvoice - for PAYMENT_CONFIRMED order returns PDF byte array")
    void downloadInvoice_forPaidOrder_returnsPdfBytes() {
        Order paidOrder = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(OrderStatus.PAYMENT_CONFIRMED)
                .build();

        byte[] samplePdf = "%PDF-1.4 dummy invoice".getBytes();
        when(orderRepository.findByIdAndUserId(paidOrder.getId(), userId)).thenReturn(Optional.of(paidOrder));
        when(pdfInvoiceService.generateInvoice(paidOrder)).thenReturn(samplePdf);

        byte[] result = orderService.downloadInvoice(userId, paidOrder.getId());

        assertThat(result).isEqualTo(samplePdf);
    }

    @Test
    @DisplayName("hasPurchasedProduct - returns boolean from repository check")
    void hasPurchasedProduct_returnsTrueWhenPurchasedAndDelivered() {
        when(orderRepository.hasPurchasedProduct(userId, "prod-1")).thenReturn(true);

        boolean result = orderService.hasPurchasedProduct(userId, "prod-1");

        assertThat(result).isTrue();
    }
}
