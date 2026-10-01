package com.pmrgsolution.couponservice.controller;

import com.pmrgsolution.couponservice.dto.request.RedeemCouponRequest;
import com.pmrgsolution.couponservice.dto.request.ValidateCouponRequest;
import com.pmrgsolution.couponservice.dto.response.CouponValidationResponse;
import com.pmrgsolution.couponservice.service.CouponService;
import com.pmrgsolution.shared.core.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/coupons")
@RequiredArgsConstructor
@Tag(name = "Internal Coupon API", description = "Internal microservice communication endpoints for checkout discount evaluation and order redemption")
public class InternalCouponController {

    private final CouponService couponService;

    @PostMapping("/validate")
    @Operation(summary = "Internal query: Validate coupon and calculate discount for order checkout")
    public ResponseEntity<ApiResponse<CouponValidationResponse>> validateCoupon(
            @Valid @RequestBody ValidateCouponRequest request
    ) {
        CouponValidationResponse response = couponService.validateCoupon(request);
        return ResponseEntity.ok(ApiResponse.ok("Coupon validated successfully", response));
    }

    @PostMapping("/redeem")
    @Operation(summary = "Internal command: Redeem coupon for order with idempotency")
    public ResponseEntity<ApiResponse<CouponValidationResponse>> redeemCoupon(
            @Valid @RequestBody RedeemCouponRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        CouponValidationResponse response = couponService.redeemCoupon(request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok("Coupon redeemed successfully", response));
    }
}
