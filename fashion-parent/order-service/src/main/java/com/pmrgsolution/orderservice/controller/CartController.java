package com.pmrgsolution.orderservice.controller;

import com.pmrgsolution.orderservice.dto.request.AddToCartRequest;
import com.pmrgsolution.orderservice.dto.request.UpdateCartQuantityRequest;
import com.pmrgsolution.orderservice.dto.response.CartItemResponse;
import com.pmrgsolution.orderservice.security.UserPrincipal;
import com.pmrgsolution.orderservice.service.CartService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management endpoints")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get current user's cart")
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> getCart(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<CartItemResponse> cart = cartService.getCart(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Cart retrieved successfully", cart));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartItemResponse>> addItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddToCartRequest req
    ) {
        CartItemResponse item = cartService.addItem(principal.getId(), req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Item added to cart", item));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<ApiResponse<CartItemResponse>> updateQuantity(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartQuantityRequest req
    ) {
        CartItemResponse item = cartService.updateQuantity(principal.getId(), itemId, req.quantity());
        return ResponseEntity.ok(ApiResponse.ok("Cart item updated successfully", item));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<Void> removeItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID itemId
    ) {
        cartService.removeItem(principal.getId(), itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Clear all items from cart")
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        cartService.clearCart(principal.getId());
        return ResponseEntity.noContent().build();
    }
}
