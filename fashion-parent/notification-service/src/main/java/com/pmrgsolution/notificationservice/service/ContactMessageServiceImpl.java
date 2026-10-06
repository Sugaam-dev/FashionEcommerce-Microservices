package com.pmrgsolution.notificationservice.service;

import com.pmrgsolution.notificationservice.dto.request.AdminReplyRequest;
import com.pmrgsolution.notificationservice.dto.request.ContactMessageRequest;
import com.pmrgsolution.notificationservice.dto.response.ContactMessageResponse;
import com.pmrgsolution.notificationservice.entity.ContactMessage;
import com.pmrgsolution.notificationservice.enums.ContactMessageStatus;
import com.pmrgsolution.notificationservice.exception.ContactMessageNotFoundException;
import com.pmrgsolution.notificationservice.repository.ContactMessageRepository;
import com.pmrgsolution.shared.core.response.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class ContactMessageServiceImpl implements ContactMessageService {

    private static final Logger log = LoggerFactory.getLogger(ContactMessageServiceImpl.class);

    private final ContactMessageRepository contactMessageRepository;
    private final EmailSenderService emailSenderService;
    private final SseHubService sseHubService;

    @Value("${app.admin.notification-email:admin@fashionecommerce.com}")
    private String adminNotificationEmail;

    public ContactMessageServiceImpl(
            ContactMessageRepository contactMessageRepository,
            EmailSenderService emailSenderService,
            SseHubService sseHubService
    ) {
        this.contactMessageRepository = contactMessageRepository;
        this.emailSenderService = emailSenderService;
        this.sseHubService = sseHubService;
    }

    @Override
    @Transactional
    public ContactMessageResponse submitMessage(ContactMessageRequest request) {
        ContactMessage contactMessage = ContactMessage.builder()
                .name(request.name().trim())
                .email(request.email().trim().toLowerCase())
                .phone(request.phone() != null ? request.phone().trim() : null)
                .subject(request.subject().trim())
                .message(request.message().trim())
                .status(ContactMessageStatus.NEW)
                .build();

        ContactMessage saved = contactMessageRepository.save(contactMessage);
        log.info("New contact message submitted: {} from {}", saved.getId(), saved.getEmail());

        // 1. Send alert email to Admin
        Map<String, Object> adminVars = Map.of(
                "senderName", saved.getName(),
                "senderEmail", saved.getEmail(),
                "subject", saved.getSubject(),
                "preview", saved.getMessage()
        );
        emailSenderService.sendEmail(
                adminNotificationEmail,
                "New Customer Inquiry: " + saved.getSubject(),
                "admin/new-inquiry-alert",
                adminVars
        );

        // 2. Broadcast via SSE for Admin real-time dashboard
        ContactMessageResponse response = ContactMessageResponse.fromEntity(saved);
        sseHubService.broadcast("NEW_INQUIRY", response);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContactMessageResponse> getMessages(ContactMessageStatus status, Pageable pageable) {
        Page<ContactMessage> page = (status != null)
                ? contactMessageRepository.findByStatus(status, pageable)
                : contactMessageRepository.findAll(pageable);

        return PageResponse.from(page.map(ContactMessageResponse::fromEntity));
    }

    @Override
    @Transactional
    public ContactMessageResponse updateMessageStatusAndReply(UUID id, AdminReplyRequest request) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ContactMessageNotFoundException("Contact message not found with ID: " + id));

        message.setStatus(request.status());
        message.setAdminReplyNotes(request.replyNotes());
        message.setRepliedAt(Instant.now());

        ContactMessage updated = contactMessageRepository.save(message);
        log.info("Contact message {} updated by admin to status {}", id, request.status());

        return ContactMessageResponse.fromEntity(updated);
    }
}
