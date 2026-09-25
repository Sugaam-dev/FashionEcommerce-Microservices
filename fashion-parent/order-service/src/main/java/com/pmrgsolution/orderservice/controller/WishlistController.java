package com.pmrgsolution.orderservice.controller;

import com.pmrgsolution.orderservice.dto.request.AddToWishlistRequest;
import com.pmrgsolution.orderservice.dto.response.WishlistItemResponse;
import com.pmrgsolution.orderservice.security.UserPrincipal;
import com.pmrgsolution.orderservice.service.WishlistService;
import com.pmrgsolution.shared.core.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Customer wishlist management endpoints")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    @Operation(summary = "Get current user's wishlist")
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> getWishlist(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<WishlistItemResponse> wishlist = wishlistService.getWishlist(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Wishlist retrieved successfully", wishlist));
    }

    @PostMapping("/items")
    @Operation(summary = "Add product to wishlist")
    public ResponseEntity<ApiResponse<WishlistItemResponse>> addItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddToWishlistRequest req
    ) {
        WishlistItemResponse item = wishlistService.addItem(principal.getId(), req.productId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Product added to wishlist", item));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove product from wishlist")
    public ResponseEntity<Void> removeItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String productId
    ) {
        wishlistService.removeItem(principal.getId(), productId);
        return ResponseEntity.noContent().build();
    }
}
