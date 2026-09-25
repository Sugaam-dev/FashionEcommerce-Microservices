package com.pmrgsolution.orderservice.controller;

import com.pmrgsolution.orderservice.service.OrderService;
import com.pmrgsolution.shared.core.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/orders")
@RequiredArgsConstructor
@Tag(name = "Internal Orders", description = "Internal microservice-to-microservice order operations")
public class InternalOrderController {

    private final OrderService orderService;

    @GetMapping("/verify-purchase")
    @Operation(summary = "Verify product purchase status for another microservice")
    public ResponseEntity<ApiResponse<Boolean>> verifyPurchase(
            @RequestParam UUID userId,
            @RequestParam String productId
    ) {
        boolean hasPurchased = orderService.hasPurchasedProduct(userId, productId);
        return ResponseEntity.ok(ApiResponse.ok("Purchase verification status retrieved", hasPurchased));
    }
}
