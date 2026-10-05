package com.pmrgsolution.catalogservice.repository;

import com.pmrgsolution.catalogservice.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    List<ProductVariant> findByProductIdAndIsActiveTrue(UUID productId);

    List<ProductVariant> findByProductId(UUID productId);
}
