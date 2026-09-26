package com.pmrgsolution.orderservice.service;

import com.pmrgsolution.orderservice.dto.request.AddressRequest;
import com.pmrgsolution.orderservice.dto.request.AdminCreateOfflineOrderRequest;
import com.pmrgsolution.orderservice.dto.request.OfflineOrderItemRequest;
import com.pmrgsolution.orderservice.dto.request.ReleaseStockRequest;
import com.pmrgsolution.orderservice.dto.request.ReserveStockRequest;
import com.pmrgsolution.orderservice.dto.request.UpdateOrderStatusRequest;
import com.pmrgsolution.orderservice.dto.request.UpdateShippingConfigRequest;
import com.pmrgsolution.orderservice.dto.response.AdminDashboardStatsResponse;
import com.pmrgsolution.orderservice.dto.response.OrderResponse;
import com.pmrgsolution.orderservice.dto.response.OrderShippingConfigResponse;
import com.pmrgsolution.orderservice.dto.response.ReservationResponse;
import com.pmrgsolution.orderservice.entity.Order;
import com.pmrgsolution.orderservice.entity.OrderShippingConfig;
import com.pmrgsolution.orderservice.enums.OrderStatus;
import com.pmrgsolution.orderservice.exception.InvalidOrderStateTransitionException;
import com.pmrgsolution.orderservice.feign.InventoryClient;
import com.pmrgsolution.orderservice.mapper.OrderMapper;
import com.pmrgsolution.orderservice.repository.CartItemRepository;
import com.pmrgsolution.orderservice.repository.OrderRepository;
import com.pmrgsolution.orderservice.repository.OrderShippingConfigRepository;
import com.pmrgsolution.orderservice.service.impl.AdminOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private OrderShippingConfigRepository shippingConfigRepository;

    @Spy
    private OrderMapper orderMapper = new OrderMapper();

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private AdminOrderServiceImpl adminOrderService;

    private UUID adminId;
    private UUID orderId;
    private Order order;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        order = Order.builder()
                .id(orderId)
                .orderNumber("ORD-20261008-9999")
                .userId(UUID.randomUUID())
                .status(OrderStatus.PAYMENT_CONFIRMED)
                .subtotal(new BigDecimal("1000.00"))
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .taxAmount(new BigDecimal("50.00"))
                .totalAmount(new BigDecimal("1050.00"))
                .build();
    }

    @Test
    @DisplayName("updateOrderStatus - valid transition (PAYMENT_CONFIRMED -> PROCESSING) updates status and adds history")
    void updateOrderStatus_validTransition_updatesAndSavesHistory() {
        when(orderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        UpdateOrderStatusRequest req = new UpdateOrderStatusRequest(
                OrderStatus.PROCESSING, null, null, "Started packing items"
        );

        OrderResponse response = adminOrderService.updateOrderStatus(orderId, req, adminId, "127.0.0.1");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(order.getStatusHistory()).hasSize(1);
        assertThat(order.getStatusHistory().get(0).getComments()).isEqualTo("Started packing items");
        verify(auditLogService).log(eq("Order"), eq(orderId.toString()), eq("STATUS_UPDATED"), any(), any(), eq(adminId), eq("ROLE_ADMIN"), any());
    }

    @Test
    @DisplayName("updateOrderStatus - invalid transition (DELIVERED -> PROCESSING) throws InvalidOrderStateTransitionException")
    void updateOrderStatus_invalidTransition_throwsException() {
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));

        UpdateOrderStatusRequest req = new UpdateOrderStatusRequest(
                OrderStatus.PROCESSING, null, null, "Invalid backtrack"
        );

        assertThatThrownBy(() -> adminOrderService.updateOrderStatus(orderId, req, adminId, "127.0.0.1"))
                .isInstanceOf(InvalidOrderStateTransitionException.class);
    }

    @Test
    @DisplayName("updateOrderStatus - transitioning to CANCELLED triggers inventory release")
    void updateOrderStatus_toCancelled_releasesInventory() {
        when(orderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        UpdateOrderStatusRequest req = new UpdateOrderStatusRequest(
                OrderStatus.CANCELLED, null, null, "Admin cancelled order"
        );

        adminOrderService.updateOrderStatus(orderId, req, adminId, "127.0.0.1");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryClient).releaseReservation(any(), any(ReleaseStockRequest.class));
    }

    @Test
    @DisplayName("createOfflineOrder - reserves inventory and persists order with PAYMENT_CONFIRMED")
    void createOfflineOrder_reservesStockAndPersistsOrder() {
        OfflineOrderItemRequest item = new OfflineOrderItemRequest(
                "SKU-MANUAL-1", "prod-1", "Offline Silk Scarf", "M", "Blue", null, 2, new BigDecimal("300.00")
        );
        AddressRequest addr = new AddressRequest(
                "Walk-in Customer", "+919876543210", "Store Counter 1", null, null,
                "New Delhi", "Delhi", "110001", "India"
        );
        AdminCreateOfflineOrderRequest req = new AdminCreateOfflineOrderRequest(
                UUID.randomUUID(), List.of(item), addr, new BigDecimal("600.00"), "CASH-REC-001", "Counter sale"
        );

        when(inventoryClient.reserveStock(any(), any(), any(ReserveStockRequest.class)))
                .thenReturn(new ReservationResponse(true, "Reserved", Instant.now().plusSeconds(900)));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        OrderResponse response = adminOrderService.createOfflineOrder(req, adminId, "127.0.0.1");

        assertThat(response.status()).isEqualTo(OrderStatus.PAYMENT_CONFIRMED);
        assertThat(response.totalAmount()).isEqualByComparingTo("600.00");
        verify(outboxEventPublisher).saveEvent(eq("Order"), any(), eq("order.created.v1"), any());
    }

    @Test
    @DisplayName("getDashboardStats - aggregates orders, revenues, and abandoned carts")
    void getDashboardStats_aggregatesMetrics() {
        when(orderRepository.count()).thenReturn(150L);
        when(orderRepository.sumTotalRevenue()).thenReturn(new BigDecimal("75000.00"));
        when(orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT)).thenReturn(10L);
        when(orderRepository.countByStatus(OrderStatus.PROCESSING)).thenReturn(25L);
        when(orderRepository.countByStatus(OrderStatus.DELIVERED)).thenReturn(100L);
        when(orderRepository.countByStatus(OrderStatus.CANCELLED)).thenReturn(15L);
        when(orderRepository.sumRevenueSince(any())).thenReturn(new BigDecimal("5000.00"));
        when(cartItemRepository.countAbandonedCartsOlderThan(any())).thenReturn(12L);

        AdminDashboardStatsResponse stats = adminOrderService.getDashboardStats();

        assertThat(stats.totalOrders()).isEqualTo(150L);
        assertThat(stats.totalRevenue()).isEqualByComparingTo("75000.00");
        assertThat(stats.deliveredOrders()).isEqualTo(100L);
        assertThat(stats.abandonedCartCount()).isEqualTo(12L);
        assertThat(stats.revenueToday()).isEqualByComparingTo("5000.00");
    }

    @Test
    @DisplayName("updateShippingConfig - updates global shipping rules and logs audit")
    void updateShippingConfig_updatesRulesAndLogsAudit() {
        OrderShippingConfig currentConfig = OrderShippingConfig.builder()
                .id(UUID.randomUUID())
                .defaultShippingFee(new BigDecimal("50.00"))
                .freeShippingThreshold(new BigDecimal("999.00"))
                .estimatedDeliveryMinDays(3)
                .estimatedDeliveryMaxDays(7)
                .build();

        when(shippingConfigRepository.findAll()).thenReturn(List.of(currentConfig));
        when(shippingConfigRepository.save(any(OrderShippingConfig.class))).thenReturn(currentConfig);

        UpdateShippingConfigRequest req = new UpdateShippingConfigRequest(
                new BigDecimal("60.00"), new BigDecimal("1200.00"), 2, 5
        );

        OrderShippingConfigResponse response = adminOrderService.updateShippingConfig(req, adminId, "127.0.0.1");

        assertThat(currentConfig.getDefaultShippingFee()).isEqualByComparingTo("60.00");
        assertThat(currentConfig.getFreeShippingThreshold()).isEqualByComparingTo("1200.00");
        assertThat(response.defaultShippingFee()).isEqualByComparingTo("60.00");
        verify(auditLogService).log(eq("OrderShippingConfig"), any(), eq("UPDATED"), any(), any(), eq(adminId), eq("ROLE_ADMIN"), any());
    }
}
