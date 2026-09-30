package com.pmrgsolution.paymentservice.repository;

import com.pmrgsolution.paymentservice.domain.entity.PaymentGatewaySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentGatewaySettingsRepository extends JpaRepository<PaymentGatewaySettings, UUID> {

    @Transactional(readOnly = true)
    Optional<PaymentGatewaySettings> findFirstByOrderByIdAsc();
}
