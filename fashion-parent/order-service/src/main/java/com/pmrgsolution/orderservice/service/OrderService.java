package com.pmrgsolution.orderservice.service;

import com.pmrgsolution.orderservice.dto.request.CancelOrderRequest;
import com.pmrgsolution.orderservice.dto.request.CreateOrderRequest;
import com.pmrgsolution.orderservice.dto.response.OrderResponse;
import com.pmrgsolution.shared.core.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(UUID userId, CreateOrderRequest req, String idempotencyKey, String clientIp);

    PageResponse<OrderResponse> getMyOrders(UUID userId, Pageable pageable);

    OrderResponse getOrderById(UUID userId, UUID orderId);

    OrderResponse cancelOrder(UUID userId, UUID orderId, CancelOrderRequest req, String clientIp);

    byte[] downloadInvoice(UUID userId, UUID orderId);

    boolean hasPurchasedProduct(UUID userId, String productId);
}
