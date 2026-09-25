package com.pmrgsolution.orderservice.service;

import com.pmrgsolution.orderservice.dto.request.AdminCreateOfflineOrderRequest;
import com.pmrgsolution.orderservice.dto.request.UpdateOrderStatusRequest;
import com.pmrgsolution.orderservice.dto.request.UpdateShippingConfigRequest;
import com.pmrgsolution.orderservice.dto.response.AdminDashboardStatsResponse;
import com.pmrgsolution.orderservice.dto.response.KeysetPageResponse;
import com.pmrgsolution.orderservice.dto.response.OrderResponse;
import com.pmrgsolution.orderservice.dto.response.OrderShippingConfigResponse;
import com.pmrgsolution.orderservice.entity.Order;
import com.pmrgsolution.orderservice.enums.OrderStatus;
import com.pmrgsolution.shared.core.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public interface AdminOrderService {

    PageResponse<OrderResponse> searchOrders(Specification<Order> spec, Pageable pageable);

    KeysetPageResponse<OrderResponse> getOrdersByCursor(UUID cursor, OrderStatus status, int size);

    OrderResponse getOrderById(UUID orderId);

    OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest req, UUID adminId, String clientIp);

    OrderResponse createOfflineOrder(AdminCreateOfflineOrderRequest req, UUID adminId, String clientIp);

    AdminDashboardStatsResponse getDashboardStats();

    OrderShippingConfigResponse getShippingConfig();

    OrderShippingConfigResponse updateShippingConfig(UpdateShippingConfigRequest req, UUID adminId, String clientIp);
}
