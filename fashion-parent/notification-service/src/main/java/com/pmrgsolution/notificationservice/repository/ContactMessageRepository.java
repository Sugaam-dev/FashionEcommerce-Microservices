package com.pmrgsolution.notificationservice.repository;

import com.pmrgsolution.notificationservice.entity.ContactMessage;
import com.pmrgsolution.notificationservice.enums.ContactMessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, UUID> {

    Page<ContactMessage> findByStatus(ContactMessageStatus status, Pageable pageable);

    Page<ContactMessage> findByEmailContainingIgnoreCase(String email, Pageable pageable);
}
