package com.pmrgsolution.authservice.repository;

import com.pmrgsolution.authservice.entity.UserAddress;
import com.pmrgsolution.authservice.enums.AddressType;
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
public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {

    @Transactional(readOnly = true)
    List<UserAddress> findByUserIdOrderByIsDefaultDescCreatedAtDesc(UUID userId);

    @Transactional(readOnly = true)
    Optional<UserAddress> findByIdAndUserId(UUID id, UUID userId);

    @Transactional(readOnly = true)
    long countByUserId(UUID userId);

    @Transactional
    @Modifying
    @Query("UPDATE UserAddress a SET a.isDefault = false WHERE a.user.id = :userId AND a.addressType = :type")
    void clearDefaultForUserAndType(@Param("userId") UUID userId, @Param("type") AddressType type);

    @Transactional
    @Modifying
    @Query("UPDATE UserAddress a SET a.isDefault = false WHERE a.user.id = :userId")
    void clearAllDefaultForUser(@Param("userId") UUID userId);
}
