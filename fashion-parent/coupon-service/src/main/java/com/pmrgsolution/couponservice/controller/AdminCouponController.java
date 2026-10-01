package com.pmrgsolution.couponservice.controller;

import com.pmrgsolution.couponservice.dto.request.CreateCouponRequest;
import com.pmrgsolution.couponservice.dto.request.UpdateCouponRequest;
import com.pmrgsolution.couponservice.dto.response.CouponResponse;
import com.pmrgsolution.couponservice.dto.response.CouponUsageResponse;
import com.pmrgsolution.couponservice.service.CouponService;
import com.pmrgsolution.shared.core.response.ApiResponse;
import com.pmrgsolution.shared.core.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
@Tag(name = "Admin Coupon API", description = "Endpoints for creating and managing coupons and viewing redemption audit logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {

    private final CouponService couponService;

    @PostMapping
    @Operation(summary = "Create a new coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(
            @Valid @RequestBody CreateCouponRequest request
    ) {
        CouponResponse response = couponService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Coupon created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get paginated list of all coupons")
    public ResponseEntity<ApiResponse<PageResponse<CouponResponse>>> getAllCoupons(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<CouponResponse> response = couponService.getAllCoupons(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Coupons fetched successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get coupon details by ID")
    public ResponseEntity<ApiResponse<CouponResponse>> getCouponById(@PathVariable UUID id) {
        CouponResponse response = couponService.getCouponById(id);
        return ResponseEntity.ok(ApiResponse.ok("Coupon fetched successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update coupon configuration")
    public ResponseEntity<ApiResponse<CouponResponse>> updateCoupon(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCouponRequest request
    ) {
        CouponResponse response = couponService.updateCoupon(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Coupon updated successfully", response));
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Toggle coupon active/inactive status")
    public ResponseEntity<ApiResponse<CouponResponse>> toggleCouponStatus(@PathVariable UUID id) {
        CouponResponse response = couponService.toggleCouponStatus(id);
        return ResponseEntity.ok(ApiResponse.ok("Coupon status toggled successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete coupon by ID")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable UUID id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok(ApiResponse.ok("Coupon deleted successfully", null));
    }

    @GetMapping("/{id}/usages")
    @Operation(summary = "Get paginated audit history of coupon redemptions")
    public ResponseEntity<ApiResponse<PageResponse<CouponUsageResponse>>> getCouponUsages(
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "usedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<CouponUsageResponse> response = couponService.getCouponUsages(id, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Coupon usages fetched successfully", response));
    }
}
