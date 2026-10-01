package com.pmrgsolution.couponservice.service;

import com.pmrgsolution.couponservice.dto.request.CreateCouponRequest;
import com.pmrgsolution.couponservice.dto.request.RedeemCouponRequest;
import com.pmrgsolution.couponservice.dto.request.UpdateCouponRequest;
import com.pmrgsolution.couponservice.dto.request.ValidateCouponRequest;
import com.pmrgsolution.couponservice.dto.response.CouponResponse;
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
import com.pmrgsolution.couponservice.repository.CouponRepository;
import com.pmrgsolution.couponservice.repository.CouponUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponUsageRepository couponUsageRepository;

    @Mock
    private CouponEventProducer couponEventProducer;

    @InjectMocks
    private CouponServiceImpl couponService;

    private UUID couponId;
    private Coupon percentageCoupon;
    private Coupon flatCoupon;

    @BeforeEach
    void setUp() {
        couponId = UUID.randomUUID();

        percentageCoupon = Coupon.builder()
                .id(couponId)
                .code("SUMMER20")
                .description("20% off up to 500")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20.00"))
                .maxDiscountAmount(new BigDecimal("500.00"))
                .minOrderAmount(new BigDecimal("1000.00"))
                .usageLimitTotal(100)
                .usageLimitPerUser(1)
                .timesUsed(5)
                .isActive(true)
                .startsAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();

        flatCoupon = Coupon.builder()
                .id(UUID.randomUUID())
                .code("FLAT100")
                .description("Flat 100 off on any purchase")
                .discountType(DiscountType.FLAT)
                .discountValue(new BigDecimal("100.00"))
                .maxDiscountAmount(null)
                .minOrderAmount(new BigDecimal("200.00"))
                .usageLimitTotal(0)
                .usageLimitPerUser(2)
                .timesUsed(0)
                .isActive(true)
                .startsAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();
    }

    @Test
    @DisplayName("createCoupon: Success")
    void testCreateCoupon_Success() {
        CreateCouponRequest request = new CreateCouponRequest(
                "WINTER50",
                "50% off winter sale",
                DiscountType.PERCENTAGE,
                new BigDecimal("50.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("500.00"),
                500,
                2,
                Instant.now(),
                Instant.now().plus(60, ChronoUnit.DAYS)
        );

        when(couponRepository.existsByCodeIgnoreCase("WINTER50")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> {
            Coupon c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CouponResponse response = couponService.createCoupon(request);

        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo("WINTER50");
        assertThat(response.discountValue()).isEqualTo(new BigDecimal("50.00"));
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    @DisplayName("createCoupon: Throws DuplicateCouponCodeException when code exists")
    void testCreateCoupon_DuplicateCode() {
        CreateCouponRequest request = new CreateCouponRequest(
                "SUMMER20",
                "Duplicate",
                DiscountType.PERCENTAGE,
                new BigDecimal("20.00"),
                null,
                BigDecimal.ZERO,
                0,
                1,
                Instant.now(),
                Instant.now().plus(10, ChronoUnit.DAYS)
        );

        when(couponRepository.existsByCodeIgnoreCase("SUMMER20")).thenReturn(true);

        assertThatThrownBy(() -> couponService.createCoupon(request))
                .isInstanceOf(DuplicateCouponCodeException.class)
                .hasMessageContaining("already exists");

        verify(couponRepository, never()).save(any());
    }

    @Test
    @DisplayName("validateCoupon: Success with percentage discount capped at maxDiscountAmount")
    void testValidateCoupon_Percentage_CapsAtMaxDiscount() {
        // Order: 4000. 20% of 4000 = 800. Capped at maxDiscountAmount 500.
        ValidateCouponRequest request = new ValidateCouponRequest("SUMMER20", new BigDecimal("4000.00"), UUID.randomUUID());
        when(couponRepository.findByCodeIgnoreCase("SUMMER20")).thenReturn(Optional.of(percentageCoupon));
        when(couponUsageRepository.countByCouponIdAndUserId(percentageCoupon.getId(), request.userId())).thenReturn(0L);

        CouponValidationResponse response = couponService.validateCoupon(request);

        assertThat(response.valid()).isTrue();
        assertThat(response.discountAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(response.finalAmount()).isEqualByComparingTo(new BigDecimal("3500.00"));
    }

    @Test
    @DisplayName("validateCoupon: Flat discount caps at order amount")
    void testValidateCoupon_Flat_CapsAtOrderAmount() {
        // Flat discount 100 on order 250
        ValidateCouponRequest request = new ValidateCouponRequest("FLAT100", new BigDecimal("250.00"), UUID.randomUUID());
        when(couponRepository.findByCodeIgnoreCase("FLAT100")).thenReturn(Optional.of(flatCoupon));
        when(couponUsageRepository.countByCouponIdAndUserId(flatCoupon.getId(), request.userId())).thenReturn(0L);

        CouponValidationResponse response = couponService.validateCoupon(request);

        assertThat(response.valid()).isTrue();
        assertThat(response.discountAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.finalAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("validateCoupon: Throws CouponExpiredException when expired")
    void testValidateCoupon_Expired() {
        percentageCoupon.setExpiresAt(Instant.now().minus(2, ChronoUnit.DAYS));
        ValidateCouponRequest request = new ValidateCouponRequest("SUMMER20", new BigDecimal("2000.00"), UUID.randomUUID());
        when(couponRepository.findByCodeIgnoreCase("SUMMER20")).thenReturn(Optional.of(percentageCoupon));

        assertThatThrownBy(() -> couponService.validateCoupon(request))
                .isInstanceOf(CouponExpiredException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("validateCoupon: Throws CouponInactiveException when inactive")
    void testValidateCoupon_Inactive() {
        percentageCoupon.setActive(false);
        ValidateCouponRequest request = new ValidateCouponRequest("SUMMER20", new BigDecimal("2000.00"), UUID.randomUUID());
        when(couponRepository.findByCodeIgnoreCase("SUMMER20")).thenReturn(Optional.of(percentageCoupon));

        assertThatThrownBy(() -> couponService.validateCoupon(request))
                .isInstanceOf(CouponInactiveException.class)
                .hasMessageContaining("not active");
    }

    @Test
    @DisplayName("validateCoupon: Throws MinimumOrderAmountNotMetException when below min amount")
    void testValidateCoupon_BelowMinOrderAmount() {
        // Min order: 1000. Order: 500.
        ValidateCouponRequest request = new ValidateCouponRequest("SUMMER20", new BigDecimal("500.00"), UUID.randomUUID());
        when(couponRepository.findByCodeIgnoreCase("SUMMER20")).thenReturn(Optional.of(percentageCoupon));

        assertThatThrownBy(() -> couponService.validateCoupon(request))
                .isInstanceOf(MinimumOrderAmountNotMetException.class)
                .hasMessageContaining("Minimum order amount");
    }

    @Test
    @DisplayName("validateCoupon: Throws CouponUsageLimitExceededException when total limit reached")
    void testValidateCoupon_TotalLimitReached() {
        percentageCoupon.setUsageLimitTotal(10);
        percentageCoupon.setTimesUsed(10);
        ValidateCouponRequest request = new ValidateCouponRequest("SUMMER20", new BigDecimal("2000.00"), UUID.randomUUID());
        when(couponRepository.findByCodeIgnoreCase("SUMMER20")).thenReturn(Optional.of(percentageCoupon));

        assertThatThrownBy(() -> couponService.validateCoupon(request))
                .isInstanceOf(CouponUsageLimitExceededException.class)
                .hasMessageContaining("global usage limit");
    }

    @Test
    @DisplayName("validateCoupon: Throws CouponUsageLimitExceededException when per-user limit reached")
    void testValidateCoupon_PerUserLimitReached() {
        UUID userId = UUID.randomUUID();
        ValidateCouponRequest request = new ValidateCouponRequest("SUMMER20", new BigDecimal("2000.00"), userId);
        when(couponRepository.findByCodeIgnoreCase("SUMMER20")).thenReturn(Optional.of(percentageCoupon));
        when(couponUsageRepository.countByCouponIdAndUserId(percentageCoupon.getId(), userId)).thenReturn(1L);

        assertThatThrownBy(() -> couponService.validateCoupon(request))
                .isInstanceOf(CouponUsageLimitExceededException.class)
                .hasMessageContaining("per user");
    }

    @Test
    @DisplayName("redeemCoupon: Success increments timesUsed, saves usage and stages outbox event")
    void testRedeemCoupon_Success() {
        UUID userId = UUID.randomUUID();
        String orderId = "ORD-2026-001";
        RedeemCouponRequest request = new RedeemCouponRequest("SUMMER20", new BigDecimal("2000.00"), userId, orderId);

        when(couponRepository.findByCodeIgnoreCase("SUMMER20")).thenReturn(Optional.of(percentageCoupon));
        when(couponUsageRepository.findByCouponIdAndOrderId(percentageCoupon.getId(), orderId)).thenReturn(Optional.empty());
        when(couponUsageRepository.countByCouponIdAndUserId(percentageCoupon.getId(), userId)).thenReturn(0L);

        CouponValidationResponse response = couponService.redeemCoupon(request, "IDEMP-KEY-1");

        assertThat(response.valid()).isTrue();
        assertThat(response.discountAmount()).isEqualByComparingTo(new BigDecimal("400.00")); // 20% of 2000 = 400 <= 500
        verify(couponRepository).incrementUsage(percentageCoupon.getId());
        verify(couponUsageRepository).save(any(CouponUsage.class));
        verify(couponEventProducer).publishCouponRedeemed(any(Coupon.class), any(CouponUsage.class));
    }

    @Test
    @DisplayName("redeemCoupon: Idempotent re-redemption returns previous result without double charge")
    void testRedeemCoupon_Idempotent() {
        UUID userId = UUID.randomUUID();
        String orderId = "ORD-2026-001";
        RedeemCouponRequest request = new RedeemCouponRequest("SUMMER20", new BigDecimal("2000.00"), userId, orderId);

        CouponUsage existingUsage = CouponUsage.builder()
                .id(UUID.randomUUID())
                .coupon(percentageCoupon)
                .userId(userId)
                .orderId(orderId)
                .discountAmount(new BigDecimal("400.00"))
                .usedAt(Instant.now())
                .build();

        when(couponRepository.findByCodeIgnoreCase("SUMMER20")).thenReturn(Optional.of(percentageCoupon));
        when(couponUsageRepository.findByCouponIdAndOrderId(percentageCoupon.getId(), orderId)).thenReturn(Optional.of(existingUsage));

        CouponValidationResponse response = couponService.redeemCoupon(request, "IDEMP-KEY-1");

        assertThat(response.valid()).isTrue();
        assertThat(response.discountAmount()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(response.message()).contains("already redeemed");
        verify(couponRepository, never()).incrementUsage(any());
        verify(couponUsageRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggleCouponStatus: Inverts active flag")
    void testToggleCouponStatus() {
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(percentageCoupon));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        CouponResponse response = couponService.toggleCouponStatus(couponId);

        assertThat(response.isActive()).isFalse();
    }

    @Test
    @DisplayName("deleteCoupon: Success")
    void testDeleteCoupon_Success() {
        when(couponRepository.existsById(couponId)).thenReturn(true);

        couponService.deleteCoupon(couponId);

        verify(couponRepository).deleteById(couponId);
    }

    @Test
    @DisplayName("deleteCoupon: Throws CouponNotFoundException when not found")
    void testDeleteCoupon_NotFound() {
        UUID unknownId = UUID.randomUUID();
        when(couponRepository.existsById(unknownId)).thenReturn(false);

        assertThatThrownBy(() -> couponService.deleteCoupon(unknownId))
                .isInstanceOf(CouponNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("getAllCoupons: Returns paginated response")
    void testGetAllCoupons() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Coupon> page = new PageImpl<>(List.of(percentageCoupon, flatCoupon), pageable, 2);
        when(couponRepository.findAll(pageable)).thenReturn(page);

        var response = couponService.getAllCoupons(pageable);

        assertThat(response).isNotNull();
        assertThat(response.content()).hasSize(2);
        assertThat(response.totalElements()).isEqualTo(2);
    }
}
