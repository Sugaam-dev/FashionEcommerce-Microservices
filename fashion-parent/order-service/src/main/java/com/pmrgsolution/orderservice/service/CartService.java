package com.pmrgsolution.orderservice.service;

import com.pmrgsolution.orderservice.dto.request.AddToCartRequest;
import com.pmrgsolution.orderservice.dto.response.CartItemResponse;

import java.util.List;
import java.util.UUID;

public interface CartService {

    List<CartItemResponse> getCart(UUID userId);

    CartItemResponse addItem(UUID userId, AddToCartRequest req);

    CartItemResponse updateQuantity(UUID userId, UUID itemId, int quantity);

    void removeItem(UUID userId, UUID itemId);

    void clearCart(UUID userId);
}
