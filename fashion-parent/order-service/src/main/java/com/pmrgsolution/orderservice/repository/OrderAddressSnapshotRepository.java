package com.pmrgsolution.orderservice.repository;

import com.pmrgsolution.orderservice.entity.OrderAddressSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderAddressSnapshotRepository extends JpaRepository<OrderAddressSnapshot, UUID> {
    Optional<OrderAddressSnapshot> findByOrderId(UUID orderId);
}
