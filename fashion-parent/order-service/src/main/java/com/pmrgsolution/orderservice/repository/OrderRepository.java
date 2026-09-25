package com.pmrgsolution.orderservice.repository;

import com.pmrgsolution.orderservice.entity.Order;
import com.pmrgsolution.orderservice.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderNumber(String orderNumber);

    @EntityGraph(attributePaths = {"items", "addressSnapshot", "statusHistory"})
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    @EntityGraph(attributePaths = {"items", "addressSnapshot", "statusHistory"})
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findWithDetailsById(@Param("id") UUID id);

    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("""
            SELECT o FROM Order o
            WHERE (:lastId IS NULL OR o.id < :lastId)
              AND (:status IS NULL OR o.status = :status)
            ORDER BY o.id DESC
            """)
    List<Order> findAllByKeysetPagination(
            @Param("lastId") UUID lastId,
            @Param("status") OrderStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END
            FROM Order o JOIN o.items i
            WHERE o.userId = :userId AND i.productId = :productId AND o.status = 'DELIVERED'
            """)
    boolean hasPurchasedProduct(@Param("userId") UUID userId, @Param("productId") String productId);

    long countByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status != 'CANCELLED' AND o.status != 'REFUNDED'")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.createdAt >= :startOfDay AND o.status != 'CANCELLED' AND o.status != 'REFUNDED'")
    BigDecimal sumRevenueSince(@Param("startOfDay") Instant startOfDay);

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, Instant before);
}
