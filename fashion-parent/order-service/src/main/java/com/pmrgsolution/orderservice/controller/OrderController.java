package com.pmrgsolution.orderservice.controller;

import com.pmrgsolution.orderservice.dto.request.CancelOrderRequest;
import com.pmrgsolution.orderservice.dto.request.CreateOrderRequest;
import com.pmrgsolution.orderservice.dto.response.OrderResponse;
import com.pmrgsolution.orderservice.security.UserPrincipal;
import com.pmrgsolution.orderservice.service.OrderService;
import com.pmrgsolution.shared.core.response.PageResponse;
import com.pmrgsolution.shared.core.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Customer order placement and management endpoints")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place a new order from cart items")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest req,
            HttpServletRequest request
    ) {
        String clientIp = request.getRemoteAddr();
        OrderResponse order = orderService.createOrder(principal.getId(), req, idempotencyKey, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Order placed successfully", order));
    }

    @GetMapping
    @Operation(summary = "Get current user's order history")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        int boundedSize = Math.min(Math.max(size, 1), 50);
        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, boundedSize, sort);

        PageResponse<OrderResponse> orders = orderService.getMyOrders(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok("Orders retrieved successfully", orders));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        OrderResponse order = orderService.getOrderById(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Order details retrieved successfully", order));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody CancelOrderRequest req,
            HttpServletRequest request
    ) {
        String clientIp = request.getRemoteAddr();
        OrderResponse order = orderService.cancelOrder(principal.getId(), id, req, clientIp);
        return ResponseEntity.ok(ApiResponse.ok("Order cancelled successfully", order));
    }

    @GetMapping("/{id}/invoice")
    @Operation(summary = "Download PDF invoice for order")
    public ResponseEntity<byte[]> downloadInvoice(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        byte[] pdfBytes = orderService.downloadInvoice(principal.getId(), id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/verify-purchase")
    @Operation(summary = "Verify if a user has completed a purchase of a product (used for verified reviews)")
    public ResponseEntity<ApiResponse<Boolean>> verifyPurchase(
            @RequestParam UUID userId,
            @RequestParam String productId
    ) {
        boolean purchased = orderService.hasPurchasedProduct(userId, productId);
        return ResponseEntity.ok(ApiResponse.ok("Purchase verification status retrieved", purchased));
    }
}
