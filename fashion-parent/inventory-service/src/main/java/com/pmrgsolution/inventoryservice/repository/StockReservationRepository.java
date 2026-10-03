package com.pmrgsolution.inventoryservice.repository;

import com.pmrgsolution.inventoryservice.entity.StockReservation;
import com.pmrgsolution.inventoryservice.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    List<StockReservation> findByOrderIdAndStatus(String orderId, ReservationStatus status);

    List<StockReservation> findByOrderId(String orderId);

    List<StockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);
}
