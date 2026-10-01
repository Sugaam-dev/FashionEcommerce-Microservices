package com.pmrgsolution.couponservice.service;

import com.pmrgsolution.couponservice.dto.request.CreateCouponRequest;
import com.pmrgsolution.couponservice.dto.request.RedeemCouponRequest;
import com.pmrgsolution.couponservice.dto.request.UpdateCouponRequest;
import com.pmrgsolution.couponservice.dto.request.ValidateCouponRequest;
import com.pmrgsolution.couponservice.dto.response.CouponResponse;
import com.pmrgsolution.couponservice.dto.response.CouponUsageResponse;
import com.pmrgsolution.couponservice.dto.response.CouponValidationResponse;
import com.pmrgsolution.couponservice.entity.Coupon;
import com.pmrgsolution.couponservice.entity.CouponUsage;
import com.pmrgsolution.couponservice.enums.DiscountType;
import com.pmrgsolution.couponservice.exception.CouponExpiredException;
import com.pmrgsolution.couponservice.exception.CouponInactiveException;
import com.pmrgsolution.couponservice.exception.CouponNotFoundException;
import com.pmrgsolution.couponservice.exception.CouponUsageLimitExceededException;
import com.pmrgsolution.couponservice.exception.DuplicateCouponCodeException;
import com.pmrgsolution.couponservice.exception.MinimumOrderAmountNotMetException;
import com.pmrgsolution.couponservice.kafka.producer.CouponEventProducer;
import com.pmrgsolution.couponservice.mapper.CouponMapper;
import com.pmrgsolution.couponservice.repository.CouponRepository;
import com.pmrgsolution.couponservice.repository.CouponUsageRepository;
import com.pmrgsolution.shared.core.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final CouponEventProducer couponEventProducer;

    @Override
    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {
        String normalizedCode = request.code().trim().toUpperCase();
        log.info("Creating coupon with code: {}", normalizedCode);

        if (couponRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new DuplicateCouponCodeException("Coupon with code already exists: " + normalizedCode);
        }

        if (request.expiresAt().isBefore(request.startsAt())) {
            throw new IllegalArgumentException("Expiry date cannot be before start date");
        }

        Coupon coupon = CouponMapper.toEntity(request);
        Coupon savedCoupon = couponRepository.save(coupon);
        log.info("Coupon created successfully with id: {}, code: {}", savedCoupon.getId(), savedCoupon.getCode());
        return CouponMapper.toResponse(savedCoupon);
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(UUID id, UpdateCouponRequest request) {
        log.info("Updating coupon with id: {}", id);
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found with id: " + id));

        if (request.expiresAt().isBefore(request.startsAt())) {
            throw new IllegalArgumentException("Expiry date cannot be before start date");
        }

        CouponMapper.updateEntity(coupon, request);
        Coupon updated = couponRepository.save(coupon);
        log.info("Coupon updated successfully for code: {}", updated.getCode());
        return CouponMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public CouponResponse toggleCouponStatus(UUID id) {
        log.info("Toggling status for coupon id: {}", id);
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found with id: " + id));

        coupon.setActive(!coupon.isActive());
        Coupon updated = couponRepository.save(coupon);
        log.info("Coupon id: {} active status changed to: {}", id, updated.isActive());
        return CouponMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteCoupon(UUID id) {
        log.info("Deleting coupon with id: {}", id);
        if (!couponRepository.existsById(id)) {
            throw new CouponNotFoundException("Coupon not found with id: " + id);
        }
        couponRepository.deleteById(id);
        log.info("Coupon deleted successfully for id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCouponById(UUID id) {
        log.debug("Fetching coupon with id: {}", id);
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found with id: " + id));
        return CouponMapper.toResponse(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCouponByCode(String code) {
        String normalizedCode = code.trim().toUpperCase();
        log.debug("Fetching coupon with code: {}", normalizedCode);
        Coupon coupon = couponRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found with code: " + normalizedCode));
        return CouponMapper.toResponse(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CouponResponse> getAllCoupons(Pageable pageable) {
        log.debug("Fetching paginated list of coupons");
        return PageResponse.from(couponRepository.findAll(pageable).map(CouponMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(ValidateCouponRequest request) {
        String normalizedCode = request.code().trim().toUpperCase();
        log.info("Validating coupon code: {} for orderAmount: {}, userId: {}",
                normalizedCode, request.orderAmount(), request.userId());

        Coupon coupon = couponRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found with code: " + normalizedCode));

        return evaluateCoupon(coupon, request.orderAmount(), request.userId());
    }

    @Override
    @Transactional
    public CouponValidationResponse redeemCoupon(RedeemCouponRequest request, String idempotencyKey) {
        String normalizedCode = request.code().trim().toUpperCase();
        log.info("Redeeming coupon code: {} for orderId: {}, userId: {}",
                normalizedCode, request.orderId(), request.userId());

        Coupon coupon = couponRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found with code: " + normalizedCode));

        // Idempotency: If this coupon was already redeemed for this exact order, return the previous successful result
        Optional<CouponUsage> existingUsage = couponUsageRepository.findByCouponIdAndOrderId(coupon.getId(), request.orderId());
        if (existingUsage.isPresent()) {
            CouponUsage usage = existingUsage.get();
            log.info("Coupon {} already redeemed for orderId: {}. Returning existing redemption result.",
                    normalizedCode, request.orderId());
            BigDecimal finalAmount = request.orderAmount().subtract(usage.getDiscountAmount()).max(BigDecimal.ZERO);
            return new CouponValidationResponse(
                    true,
                    coupon.getCode(),
                    coupon.getDiscountType(),
                    usage.getDiscountAmount(),
                    finalAmount,
                    "Coupon already redeemed for this order"
            );
        }

        // Validate coupon constraints
        CouponValidationResponse validation = evaluateCoupon(coupon, request.orderAmount(), request.userId());

        // Increment usage count on coupon
        couponRepository.incrementUsage(coupon.getId());

        // Record coupon usage
        CouponUsage usage = CouponUsage.builder()
                .coupon(coupon)
                .userId(request.userId())
                .orderId(request.orderId())
                .discountAmount(validation.discountAmount())
                .build();

        couponUsageRepository.save(usage);

        // Publish event via transactional outbox
        couponEventProducer.publishCouponRedeemed(coupon, usage);

        log.info("Coupon {} successfully redeemed for orderId: {}, discountAmount: {}",
                normalizedCode, request.orderId(), validation.discountAmount());

        return validation;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CouponUsageResponse> getCouponUsages(UUID couponId, Pageable pageable) {
        log.debug("Fetching usage history for coupon id: {}", couponId);
        if (!couponRepository.existsById(couponId)) {
            throw new CouponNotFoundException("Coupon not found with id: " + couponId);
        }
        return PageResponse.from(couponUsageRepository.findByCouponIdOrderByUsedAtDesc(couponId, pageable)
                .map(CouponMapper::toUsageResponse));
    }

    private CouponValidationResponse evaluateCoupon(Coupon coupon, BigDecimal orderAmount, UUID userId) {
        if (!coupon.isActive()) {
            throw new CouponInactiveException("Coupon is not active");
        }

        Instant now = Instant.now();
        if (now.isBefore(coupon.getStartsAt()) || now.isAfter(coupon.getExpiresAt())) {
            throw new CouponExpiredException("Coupon has expired or is not yet active");
        }

        if (coupon.getMinOrderAmount() != null &&
                coupon.getMinOrderAmount().compareTo(BigDecimal.ZERO) > 0 &&
                orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new MinimumOrderAmountNotMetException("Minimum order amount of " +
                    coupon.getMinOrderAmount() + " required to use this coupon");
        }

        if (coupon.getUsageLimitTotal() > 0 && coupon.getTimesUsed() >= coupon.getUsageLimitTotal()) {
            throw new CouponUsageLimitExceededException("Coupon global usage limit has been reached");
        }

        if (coupon.getUsageLimitPerUser() > 0 && userId != null) {
            long userUsageCount = couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), userId);
            if (userUsageCount >= coupon.getUsageLimitPerUser()) {
                throw new CouponUsageLimitExceededException("Coupon usage limit per user has been reached");
            }
        }

        BigDecimal discountAmount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal calculated = orderAmount.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscountAmount() != null && calculated.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discountAmount = coupon.getMaxDiscountAmount();
            } else {
                discountAmount = calculated;
            }
        } else {
            discountAmount = coupon.getDiscountValue().min(orderAmount);
        }

        BigDecimal finalAmount = orderAmount.subtract(discountAmount).max(BigDecimal.ZERO);

        return new CouponValidationResponse(
                true,
                coupon.getCode(),
                coupon.getDiscountType(),
                discountAmount,
                finalAmount,
                "Coupon is valid"
        );
    }
}
