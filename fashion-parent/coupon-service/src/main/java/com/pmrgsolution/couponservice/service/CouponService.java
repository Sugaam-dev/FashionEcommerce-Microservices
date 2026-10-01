package com.pmrgsolution.couponservice.service;

import com.pmrgsolution.couponservice.dto.request.CreateCouponRequest;
import com.pmrgsolution.couponservice.dto.request.RedeemCouponRequest;
import com.pmrgsolution.couponservice.dto.request.UpdateCouponRequest;
import com.pmrgsolution.couponservice.dto.request.ValidateCouponRequest;
import com.pmrgsolution.couponservice.dto.response.CouponResponse;
import com.pmrgsolution.couponservice.dto.response.CouponUsageResponse;
import com.pmrgsolution.couponservice.dto.response.CouponValidationResponse;
import com.pmrgsolution.shared.core.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CouponService {

    CouponResponse createCoupon(CreateCouponRequest request);

    CouponResponse updateCoupon(UUID id, UpdateCouponRequest request);

    CouponResponse toggleCouponStatus(UUID id);

    void deleteCoupon(UUID id);

    CouponResponse getCouponById(UUID id);

    CouponResponse getCouponByCode(String code);

    PageResponse<CouponResponse> getAllCoupons(Pageable pageable);

    CouponValidationResponse validateCoupon(ValidateCouponRequest request);

    CouponValidationResponse redeemCoupon(RedeemCouponRequest request, String idempotencyKey);

    PageResponse<CouponUsageResponse> getCouponUsages(UUID couponId, Pageable pageable);
}
