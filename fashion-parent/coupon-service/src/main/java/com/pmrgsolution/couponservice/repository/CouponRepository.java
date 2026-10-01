package com.pmrgsolution.couponservice.repository;

import com.pmrgsolution.couponservice.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    @Transactional(readOnly = true)
    Optional<Coupon> findByCodeIgnoreCase(String code);

    @Transactional(readOnly = true)
    boolean existsByCodeIgnoreCase(String code);

    @Transactional
    @Modifying
    @Query("UPDATE Coupon c SET c.timesUsed = c.timesUsed + 1 WHERE c.id = :id")
    void incrementUsage(@Param("id") UUID id);
}
