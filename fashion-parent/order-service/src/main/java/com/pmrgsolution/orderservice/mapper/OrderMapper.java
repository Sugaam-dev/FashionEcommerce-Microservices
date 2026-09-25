package com.pmrgsolution.orderservice.mapper;

import com.pmrgsolution.orderservice.dto.response.AddressSnapshotResponse;
import com.pmrgsolution.orderservice.dto.response.CartItemResponse;
import com.pmrgsolution.orderservice.dto.response.OrderItemResponse;
import com.pmrgsolution.orderservice.dto.response.OrderResponse;
import com.pmrgsolution.orderservice.dto.response.OrderShippingConfigResponse;
import com.pmrgsolution.orderservice.dto.response.OrderStatusHistoryResponse;
import com.pmrgsolution.orderservice.dto.response.WishlistItemResponse;
import com.pmrgsolution.orderservice.entity.CartItem;
import com.pmrgsolution.orderservice.entity.Order;
import com.pmrgsolution.orderservice.entity.OrderAddressSnapshot;
import com.pmrgsolution.orderservice.entity.OrderItem;
import com.pmrgsolution.orderservice.entity.OrderShippingConfig;
import com.pmrgsolution.orderservice.entity.OrderStatusHistory;
import com.pmrgsolution.orderservice.entity.WishlistItem;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class OrderMapper {

    public CartItemResponse toCartItemResponse(CartItem item) {
        if (item == null) {
            return null;
        }
        return new CartItemResponse(
                item.getId(),
                item.getSku(),
                item.getProductId(),
                item.getQuantity(),
                item.getUpdatedAt()
        );
    }

    public WishlistItemResponse toWishlistItemResponse(WishlistItem item) {
        if (item == null) {
            return null;
        }
        return new WishlistItemResponse(
                item.getId(),
                item.getProductId(),
                item.getAddedAt()
        );
    }

    public OrderItemResponse toOrderItemResponse(OrderItem item) {
        if (item == null) {
            return null;
        }
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductTitle(),
                item.getSku(),
                item.getVariantSize(),
                item.getVariantColor(),
                item.getPrimaryImageUrl(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getTotalPrice()
        );
    }

    public AddressSnapshotResponse toAddressSnapshotResponse(OrderAddressSnapshot address) {
        if (address == null) {
            return null;
        }
        return new AddressSnapshotResponse(
                address.getRecipientName(),
                address.getRecipientPhone(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getLandmark(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry()
        );
    }

    public OrderStatusHistoryResponse toOrderStatusHistoryResponse(OrderStatusHistory history) {
        if (history == null) {
            return null;
        }
        return new OrderStatusHistoryResponse(
                history.getId(),
                history.getStatus(),
                history.getComments(),
                history.getChangedBy(),
                history.getCreatedAt()
        );
    }

    public OrderResponse toOrderResponse(Order order) {
        if (order == null) {
            return null;
        }

        List<OrderItemResponse> itemResponses = order.getItems() != null
                ? order.getItems().stream().map(this::toOrderItemResponse).toList()
                : Collections.emptyList();

        List<OrderStatusHistoryResponse> historyResponses = order.getStatusHistory() != null
                ? order.getStatusHistory().stream().map(this::toOrderStatusHistoryResponse).toList()
                : Collections.emptyList();

        AddressSnapshotResponse addressResponse = toAddressSnapshotResponse(order.getAddressSnapshot());

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                order.getStatus(),
                order.getPaymentType(),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getShippingFee(),
                order.getTaxAmount(),
                order.getTotalAmount(),
                order.getCouponCode(),
                order.getTrackingNumber(),
                order.getCourierName(),
                order.getAdminNotes(),
                order.getCustomerNotes(),
                itemResponses,
                addressResponse,
                historyResponses,
                order.getPaidAt(),
                order.getShippedAt(),
                order.getDeliveredAt(),
                order.getCancelledAt(),
                order.getCancellationReason(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public OrderShippingConfigResponse toOrderShippingConfigResponse(OrderShippingConfig config) {
        if (config == null) {
            return null;
        }
        return new OrderShippingConfigResponse(
                config.getId(),
                config.getDefaultShippingFee(),
                config.getFreeShippingThreshold(),
                config.getEstimatedDeliveryMinDays(),
                config.getEstimatedDeliveryMaxDays()
        );
    }
}
