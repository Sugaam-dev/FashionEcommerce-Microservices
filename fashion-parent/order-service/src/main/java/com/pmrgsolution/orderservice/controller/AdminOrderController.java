package com.pmrgsolution.orderservice.controller;

import com.pmrgsolution.orderservice.dto.request.AdminCreateOfflineOrderRequest;
import com.pmrgsolution.orderservice.dto.request.UpdateOrderStatusRequest;
import com.pmrgsolution.orderservice.dto.request.UpdateShippingConfigRequest;
import com.pmrgsolution.orderservice.dto.response.AdminDashboardStatsResponse;
import com.pmrgsolution.orderservice.dto.response.KeysetPageResponse;
import com.pmrgsolution.orderservice.dto.response.OrderResponse;
import com.pmrgsolution.orderservice.dto.response.OrderShippingConfigResponse;
import com.pmrgsolution.orderservice.entity.Order;
import com.pmrgsolution.orderservice.enums.OrderStatus;
import com.pmrgsolution.orderservice.security.UserPrincipal;
import com.pmrgsolution.orderservice.service.AdminOrderService;
import com.pmrgsolution.shared.core.response.PageResponse;
import com.pmrgsolution.shared.core.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Orders", description = "Administrative order operations, status workflows, and statistics")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    @Operation(summary = "Search and filter all orders (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> searchOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, boundedSize, sort);

        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (orderNumber != null && !orderNumber.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("orderNumber")), "%" + orderNumber.toLowerCase().trim() + "%"));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageResponse<OrderResponse> response = adminOrderService.searchOrders(spec, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Orders retrieved successfully", response));
    }

    @GetMapping("/cursor")
    @Operation(summary = "Keyset cursor-based pagination for high-volume order queries")
    public ResponseEntity<ApiResponse<KeysetPageResponse<OrderResponse>>> getOrdersByCursor(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "50") int size
    ) {
        KeysetPageResponse<OrderResponse> page = adminOrderService.getOrdersByCursor(cursor, status, size);
        return ResponseEntity.ok(ApiResponse.ok("Keyset page retrieved", page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full order details with status transition history")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable UUID id) {
        OrderResponse order = adminOrderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.ok("Order details retrieved successfully", order));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status, courier info, tracking number")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest req,
            HttpServletRequest request
    ) {
        String clientIp = request.getRemoteAddr();
        UUID adminId = principal != null ? principal.getId() : null;
        OrderResponse order = adminOrderService.updateOrderStatus(id, req, adminId, clientIp);
        return ResponseEntity.ok(ApiResponse.ok("Order status updated successfully", order));
    }

    @PostMapping("/offline")
    @Operation(summary = "Create an offline / manual counter order")
    public ResponseEntity<ApiResponse<OrderResponse>> createOfflineOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AdminCreateOfflineOrderRequest req,
            HttpServletRequest request
    ) {
        String clientIp = request.getRemoteAddr();
        UUID adminId = principal != null ? principal.getId() : null;
        OrderResponse order = adminOrderService.createOfflineOrder(req, adminId, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Offline order created successfully", order));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard order and revenue metrics")
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getDashboardStats() {
        AdminDashboardStatsResponse stats = adminOrderService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.ok("Dashboard statistics retrieved", stats));
    }

    @GetMapping("/shipping-config")
    @Operation(summary = "Get current global shipping configuration")
    public ResponseEntity<ApiResponse<OrderShippingConfigResponse>> getShippingConfig() {
        OrderShippingConfigResponse config = adminOrderService.getShippingConfig();
        return ResponseEntity.ok(ApiResponse.ok("Shipping configuration retrieved", config));
    }

    @PutMapping("/shipping-config")
    @Operation(summary = "Update global shipping fee and threshold rules")
    public ResponseEntity<ApiResponse<OrderShippingConfigResponse>> updateShippingConfig(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateShippingConfigRequest req,
            HttpServletRequest request
    ) {
        String clientIp = request.getRemoteAddr();
        UUID adminId = principal != null ? principal.getId() : null;
        OrderShippingConfigResponse config = adminOrderService.updateShippingConfig(req, adminId, clientIp);
        return ResponseEntity.ok(ApiResponse.ok("Shipping configuration updated successfully", config));
    }
}
