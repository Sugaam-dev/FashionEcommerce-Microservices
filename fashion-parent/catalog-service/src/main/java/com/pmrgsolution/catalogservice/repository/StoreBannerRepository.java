package com.pmrgsolution.catalogservice.repository;

import com.pmrgsolution.catalogservice.entity.StoreBanner;
import com.pmrgsolution.catalogservice.enums.BannerPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoreBannerRepository extends JpaRepository<StoreBanner, UUID> {

    List<StoreBanner> findByPositionAndIsActiveTrueOrderByDisplayOrderAsc(BannerPosition position);

    List<StoreBanner> findAllByOrderByDisplayOrderAsc();
}
