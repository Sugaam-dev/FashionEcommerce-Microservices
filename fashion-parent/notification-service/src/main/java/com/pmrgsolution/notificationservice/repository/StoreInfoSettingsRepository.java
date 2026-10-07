package com.pmrgsolution.notificationservice.repository;

import com.pmrgsolution.notificationservice.entity.StoreInfoSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreInfoSettingsRepository extends JpaRepository<StoreInfoSettings, UUID> {

    Optional<StoreInfoSettings> findFirstByOrderByUpdatedAtDesc();
}
