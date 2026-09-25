package com.pmrgsolution.orderservice.repository;

import com.pmrgsolution.orderservice.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {

    List<WishlistItem> findByUserIdOrderByAddedAtDesc(UUID userId);

    Optional<WishlistItem> findByUserIdAndProductId(UUID userId, String productId);

    boolean existsByUserIdAndProductId(UUID userId, String productId);

    @Modifying
    @Transactional
    @Query("DELETE FROM WishlistItem w WHERE w.userId = :userId AND w.productId = :productId")
    void deleteByUserIdAndProductId(@Param("userId") UUID userId, @Param("productId") String productId);
}
