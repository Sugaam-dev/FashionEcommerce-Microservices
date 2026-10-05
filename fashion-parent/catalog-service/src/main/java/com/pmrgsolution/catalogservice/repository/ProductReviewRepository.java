package com.pmrgsolution.catalogservice.repository;

import com.pmrgsolution.catalogservice.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {

    Page<ProductReview> findByProductIdAndApprovedTrueOrderByCreatedAtDesc(UUID productId, Pageable pageable);

    Page<ProductReview> findByApprovedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<ProductReview> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByProductIdAndUserId(UUID productId, UUID userId);

    @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.product.id = :pid AND r.approved = true")
    Optional<Double> findAverageRating(@Param("pid") UUID productId);

    @Query("SELECT COUNT(r) FROM ProductReview r WHERE r.product.id = :pid AND r.approved = true")
    long countApprovedReviews(@Param("pid") UUID productId);
}
