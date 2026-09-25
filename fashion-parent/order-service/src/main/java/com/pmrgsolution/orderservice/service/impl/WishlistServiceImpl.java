package com.pmrgsolution.orderservice.service.impl;

import com.pmrgsolution.orderservice.dto.response.WishlistItemResponse;
import com.pmrgsolution.orderservice.entity.WishlistItem;
import com.pmrgsolution.orderservice.mapper.OrderMapper;
import com.pmrgsolution.orderservice.repository.WishlistItemRepository;
import com.pmrgsolution.orderservice.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlist(UUID userId) {
        log.debug("Fetching wishlist for user: {}", userId);
        return wishlistItemRepository.findByUserIdOrderByAddedAtDesc(userId)
                .stream()
                .map(orderMapper::toWishlistItemResponse)
                .toList();
    }

    @Override
    @Transactional
    public WishlistItemResponse addItem(UUID userId, String productId) {
        log.info("Adding product {} to wishlist for user: {}", productId, userId);

        Optional<WishlistItem> existingOpt = wishlistItemRepository.findByUserIdAndProductId(userId, productId);
        if (existingOpt.isPresent()) {
            return orderMapper.toWishlistItemResponse(existingOpt.get());
        }

        WishlistItem item = WishlistItem.builder()
                .userId(userId)
                .productId(productId)
                .build();
        WishlistItem saved = wishlistItemRepository.save(item);

        return orderMapper.toWishlistItemResponse(saved);
    }

    @Override
    @Transactional
    public void removeItem(UUID userId, String productId) {
        log.info("Removing product {} from wishlist for user: {}", productId, userId);
        wishlistItemRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInWishlist(UUID userId, String productId) {
        return wishlistItemRepository.existsByUserIdAndProductId(userId, productId);
    }
}
