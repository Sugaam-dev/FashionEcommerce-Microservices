package com.pmrgsolution.paymentservice.repository;

import com.pmrgsolution.paymentservice.domain.entity.PaymentTransaction;
import com.pmrgsolution.paymentservice.domain.enums.PaymentMethod;
import com.pmrgsolution.paymentservice.domain.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID>, JpaSpecificationExecutor<PaymentTransaction> {

    @Transactional(readOnly = true)
    Optional<PaymentTransaction> findByOrderId(String orderId);

    @Transactional(readOnly = true)
    Optional<PaymentTransaction> findByOrderIdAndStatus(String orderId, PaymentStatus status);

    @Transactional(readOnly = true)
    boolean existsByGatewayPaymentId(String gatewayPaymentId);

    @Transactional(readOnly = true)
    Page<PaymentTransaction> findByStatus(PaymentStatus status, Pageable pageable);

    @Transactional(readOnly = true)
    Page<PaymentTransaction> findByPaymentMethodAndStatus(PaymentMethod paymentMethod, PaymentStatus status, Pageable pageable);

    @Transactional(readOnly = true)
    Page<PaymentTransaction> findByUserId(UUID userId, Pageable pageable);

    @Transactional(readOnly = true)
    @Query("""
        SELECT p FROM PaymentTransaction p
        WHERE p.status IN ('VERIFIED', 'REJECTED')
          AND p.manualUpiProofImageUrl IS NOT NULL
          AND p.updatedAt < :cutoffDate
    """)
    List<PaymentTransaction> findStaleManualProofTransactions(@Param("cutoffDate") Instant cutoffDate);
}
