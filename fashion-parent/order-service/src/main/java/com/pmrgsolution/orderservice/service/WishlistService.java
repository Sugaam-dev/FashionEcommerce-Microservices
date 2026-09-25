package com.pmrgsolution.orderservice.service;

import com.pmrgsolution.orderservice.dto.response.WishlistItemResponse;

import java.util.List;
import java.util.UUID;

public interface WishlistService {

    List<WishlistItemResponse> getWishlist(UUID userId);

    WishlistItemResponse addItem(UUID userId, String productId);

    void removeItem(UUID userId, String productId);

    boolean isInWishlist(UUID userId, String productId);
}
