package com.pmrgsolution.authservice.repository;

import com.pmrgsolution.authservice.entity.EmailOtp;
import com.pmrgsolution.authservice.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, UUID> {

    @Transactional(readOnly = true)
    Optional<EmailOtp> findTopByEmailAndOtpTypeAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email, OtpType otpType, Instant now);

    @Transactional
    @Modifying
    @Query("UPDATE EmailOtp o SET o.consumed = true WHERE o.email = :email AND o.otpType = :otpType AND o.consumed = false")
    void invalidateActiveOtps(@Param("email") String email, @Param("otpType") OtpType otpType);

    @Transactional
    @Modifying
    @Query("DELETE FROM EmailOtp o WHERE o.expiresAt < :now OR o.consumed = true")
    int purgeExpiredAndConsumedOtps(@Param("now") Instant now);
}
