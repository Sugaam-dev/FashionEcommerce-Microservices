package com.pmrgsolution.couponservice.mapper;

import com.pmrgsolution.couponservice.dto.request.CreateCouponRequest;
import com.pmrgsolution.couponservice.dto.request.UpdateCouponRequest;
import com.pmrgsolution.couponservice.dto.response.CouponResponse;
import com.pmrgsolution.couponservice.dto.response.CouponUsageResponse;
import com.pmrgsolution.couponservice.entity.Coupon;
import com.pmrgsolution.couponservice.entity.CouponUsage;

import java.math.BigDecimal;

public final class CouponMapper {

    private CouponMapper() {
    }

    public static Coupon toEntity(CreateCouponRequest request) {
        if (request == null) {
            return null;
        }

        return Coupon.builder()
                .code(request.code().trim().toUpperCase())
                .description(request.description() != null ? request.description().trim() : null)
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .maxDiscountAmount(request.maxDiscountAmount())
                .minOrderAmount(request.minOrderAmount() != null ? request.minOrderAmount() : BigDecimal.ZERO)
                .usageLimitTotal(request.usageLimitTotal())
                .usageLimitPerUser(request.usageLimitPerUser())
                .timesUsed(0)
                .isActive(true)
                .startsAt(request.startsAt())
                .expiresAt(request.expiresAt())
                .build();
    }

    public static void updateEntity(Coupon coupon, UpdateCouponRequest request) {
        if (coupon == null || request == null) {
            return;
        }

        coupon.setDescription(request.description() != null ? request.description().trim() : null);
        coupon.setDiscountType(request.discountType());
        coupon.setDiscountValue(request.discountValue());
        coupon.setMaxDiscountAmount(request.maxDiscountAmount());
        coupon.setMinOrderAmount(request.minOrderAmount() != null ? request.minOrderAmount() : BigDecimal.ZERO);
        coupon.setUsageLimitTotal(request.usageLimitTotal());
        coupon.setUsageLimitPerUser(request.usageLimitPerUser());
        if (request.isActive() != null) {
            coupon.setActive(request.isActive());
        }
        coupon.setStartsAt(request.startsAt());
        coupon.setExpiresAt(request.expiresAt());
    }

    public static CouponResponse toResponse(Coupon coupon) {
        if (coupon == null) {
            return null;
        }

        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDescription(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMaxDiscountAmount(),
                coupon.getMinOrderAmount(),
                coupon.getTimesUsed(),
                coupon.getUsageLimitTotal(),
                coupon.getUsageLimitPerUser(),
                coupon.isActive(),
                coupon.getStartsAt(),
                coupon.getExpiresAt(),
                coupon.getCreatedAt(),
                coupon.getUpdatedAt()
        );
    }

    public static CouponUsageResponse toUsageResponse(CouponUsage usage) {
        if (usage == null) {
            return null;
        }

        return new CouponUsageResponse(
                usage.getId(),
                usage.getCoupon().getId(),
                usage.getCoupon().getCode(),
                usage.getUserId(),
                usage.getOrderId(),
                usage.getDiscountAmount(),
                usage.getUsedAt()
        );
    }
}
