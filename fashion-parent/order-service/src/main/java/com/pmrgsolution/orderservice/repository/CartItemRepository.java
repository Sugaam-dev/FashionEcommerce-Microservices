package com.pmrgsolution.orderservice.repository;

import com.pmrgsolution.orderservice.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<CartItem> findByUserIdAndSku(UUID userId, String sku);

    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem c WHERE c.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT c.userId FROM CartItem c WHERE c.updatedAt < :before")
    List<UUID> findDistinctUserIdsWithCartOlderThan(@Param("before") Instant before);

    @Query("SELECT COUNT(DISTINCT c.userId) FROM CartItem c WHERE c.updatedAt < :before")
    long countAbandonedCartsOlderThan(@Param("before") Instant before);

    long countByUserId(UUID userId);
}
