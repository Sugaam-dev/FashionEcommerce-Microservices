package com.pmrgsolution.inventoryservice.repository;

import com.pmrgsolution.inventoryservice.entity.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    Optional<InventoryItem> findBySku(String sku);

    boolean existsBySku(String sku);

    List<InventoryItem> findBySkuIn(List<String> skus);

    @Query("SELECT i FROM InventoryItem i WHERE i.availableQuantity <= i.lowStockThreshold")
    List<InventoryItem> findAllLowStockItems();

    @Query("SELECT i FROM InventoryItem i WHERE i.availableQuantity <= i.lowStockThreshold")
    Page<InventoryItem> findLowStockItems(Pageable pageable);
}
