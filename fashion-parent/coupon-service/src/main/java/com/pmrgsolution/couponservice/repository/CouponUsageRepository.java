package com.pmrgsolution.couponservice.repository;

import com.pmrgsolution.couponservice.entity.CouponUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, UUID> {

    @Transactional(readOnly = true)
    long countByCouponIdAndUserId(UUID couponId, UUID userId);

    @Transactional(readOnly = true)
    boolean existsByCouponIdAndOrderId(UUID couponId, String orderId);

    @Transactional(readOnly = true)
    Optional<CouponUsage> findByCouponIdAndOrderId(UUID couponId, String orderId);

    @Transactional(readOnly = true)
    Page<CouponUsage> findByCouponIdOrderByUsedAtDesc(UUID couponId, Pageable pageable);

    @Transactional(readOnly = true)
    List<CouponUsage> findByOrderId(String orderId);
}
