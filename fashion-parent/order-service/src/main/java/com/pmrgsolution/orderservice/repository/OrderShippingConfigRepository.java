package com.pmrgsolution.orderservice.repository;

import com.pmrgsolution.orderservice.entity.OrderShippingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderShippingConfigRepository extends JpaRepository<OrderShippingConfig, UUID> {
}
