package com.pmrgsolution.orderservice.service.impl;

import com.pmrgsolution.orderservice.dto.request.AddToCartRequest;
import com.pmrgsolution.orderservice.dto.response.CartItemResponse;
import com.pmrgsolution.orderservice.entity.CartItem;
import com.pmrgsolution.orderservice.exception.CartItemNotFoundException;
import com.pmrgsolution.orderservice.mapper.OrderMapper;
import com.pmrgsolution.orderservice.repository.CartItemRepository;
import com.pmrgsolution.orderservice.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CartItemResponse> getCart(UUID userId) {
        log.debug("Fetching cart items for user: {}", userId);
        return cartItemRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(orderMapper::toCartItemResponse)
                .toList();
    }

    @Override
    @Transactional
    public CartItemResponse addItem(UUID userId, AddToCartRequest req) {
        log.info("Adding item to cart for user: {}, SKU: {}, qty: {}", userId, req.sku(), req.quantity());

        Optional<CartItem> existingItemOpt = cartItemRepository.findByUserIdAndSku(userId, req.sku());

        CartItem savedItem;
        if (existingItemOpt.isPresent()) {
            CartItem existing = existingItemOpt.get();
            existing.setQuantity(existing.getQuantity() + req.quantity());
            existing.setUpdatedAt(Instant.now());
            savedItem = cartItemRepository.save(existing);
        } else {
            CartItem newItem = CartItem.builder()
                    .userId(userId)
                    .sku(req.sku())
                    .productId(req.productId())
                    .quantity(req.quantity())
                    .build();
            savedItem = cartItemRepository.save(newItem);
        }

        return orderMapper.toCartItemResponse(savedItem);
    }

    @Override
    @Transactional
    public CartItemResponse updateQuantity(UUID userId, UUID itemId, int quantity) {
        log.info("Updating cart item {} quantity to {} for user: {}", itemId, quantity, userId);

        CartItem item = cartItemRepository.findById(itemId)
                .filter(ci -> ci.getUserId().equals(userId))
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found: " + itemId));

        item.setQuantity(quantity);
        item.setUpdatedAt(Instant.now());
        CartItem updated = cartItemRepository.save(item);

        return orderMapper.toCartItemResponse(updated);
    }

    @Override
    @Transactional
    public void removeItem(UUID userId, UUID itemId) {
        log.info("Removing cart item {} for user: {}", itemId, userId);

        CartItem item = cartItemRepository.findById(itemId)
                .filter(ci -> ci.getUserId().equals(userId))
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found: " + itemId));

        cartItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void clearCart(UUID userId) {
        log.info("Clearing all cart items for user: {}", userId);
        cartItemRepository.deleteByUserId(userId);
    }
}
