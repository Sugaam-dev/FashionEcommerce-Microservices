package com.pmrgsolution.notificationservice.controller;

import com.pmrgsolution.notificationservice.dto.request.AdminReplyRequest;
import com.pmrgsolution.notificationservice.dto.request.StoreInfoSettingsUpdateRequest;
import com.pmrgsolution.notificationservice.dto.response.ContactMessageResponse;
import com.pmrgsolution.notificationservice.dto.response.EmailQuotaResponse;
import com.pmrgsolution.notificationservice.dto.response.NotificationLogResponse;
import com.pmrgsolution.notificationservice.dto.response.StoreInfoSettingsResponse;
import com.pmrgsolution.notificationservice.entity.EmailUsageQuota;
import com.pmrgsolution.notificationservice.enums.ContactMessageStatus;
import com.pmrgsolution.notificationservice.repository.EmailUsageQuotaRepository;
import com.pmrgsolution.notificationservice.repository.NotificationLogRepository;
import com.pmrgsolution.notificationservice.service.ContactMessageService;
import com.pmrgsolution.notificationservice.service.StoreInfoService;
import com.pmrgsolution.shared.core.response.ApiResponse;
import com.pmrgsolution.shared.core.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@Tag(name = "Admin Notifications", description = "Admin endpoints for logs, messages, quotas, and store settings")
@SecurityRequirement(name = "BearerAuth")
public class AdminNotificationController {

    private final NotificationLogRepository notificationLogRepository;
    private final ContactMessageService contactMessageService;
    private final EmailUsageQuotaRepository emailUsageQuotaRepository;
    private final StoreInfoService storeInfoService;

    @Value("${app.mail.daily-limit:3000}")
    private int defaultDailyLimit;

    public AdminNotificationController(
            NotificationLogRepository notificationLogRepository,
            ContactMessageService contactMessageService,
            EmailUsageQuotaRepository emailUsageQuotaRepository,
            StoreInfoService storeInfoService
    ) {
        this.notificationLogRepository = notificationLogRepository;
        this.contactMessageService = contactMessageService;
        this.emailUsageQuotaRepository = emailUsageQuotaRepository;
        this.storeInfoService = storeInfoService;
    }

    @GetMapping("/logs")
    @Operation(summary = "Get paginated notification delivery audit logs")
    public ResponseEntity<ApiResponse<PageResponse<NotificationLogResponse>>> getLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpServletRequest
    ) {
        var page = notificationLogRepository.findAll(pageable);
        var pageResponse = PageResponse.from(page.map(NotificationLogResponse::fromEntity));

        return ResponseEntity.ok(ApiResponse.success(
                200,
                "Notification logs retrieved successfully.",
                pageResponse,
                httpServletRequest.getRequestURI()
        ));
    }

    @GetMapping("/contact-messages")
    @Operation(summary = "Get paginated customer contact messages, optionally filtered by status")
    public ResponseEntity<ApiResponse<PageResponse<ContactMessageResponse>>> getContactMessages(
            @RequestParam(required = false) ContactMessageStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpServletRequest
    ) {
        PageResponse<ContactMessageResponse> response = contactMessageService.getMessages(status, pageable);

        return ResponseEntity.ok(ApiResponse.success(
                200,
                "Contact messages retrieved successfully.",
                response,
                httpServletRequest.getRequestURI()
        ));
    }

    @PatchMapping("/contact-messages/{id}/status")
    @Operation(summary = "Reply to and update the status of a customer contact message")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> updateContactMessageStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AdminReplyRequest request,
            HttpServletRequest httpServletRequest
    ) {
        ContactMessageResponse response = contactMessageService.updateMessageStatusAndReply(id, request);

        return ResponseEntity.ok(ApiResponse.success(
                200,
                "Contact message updated successfully.",
                response,
                httpServletRequest.getRequestURI()
        ));
    }

    @GetMapping("/email-quota")
    @Operation(summary = "Get today's email usage and quota statistics")
    public ResponseEntity<ApiResponse<EmailQuotaResponse>> getEmailQuota(HttpServletRequest httpServletRequest) {
        String todayKey = LocalDate.now(ZoneOffset.UTC).toString();

        EmailUsageQuota quota = emailUsageQuotaRepository.findByDateKey(todayKey)
                .orElse(EmailUsageQuota.builder()
                        .dateKey(todayKey)
                        .count(0)
                        .dailyLimit(defaultDailyLimit)
                        .lastSentAt(Instant.now())
                        .build());

        return ResponseEntity.ok(ApiResponse.success(
                200,
                "Email quota statistics retrieved successfully.",
                EmailQuotaResponse.fromEntity(quota),
                httpServletRequest.getRequestURI()
        ));
    }

    @GetMapping("/store-info")
    @Operation(summary = "Get full store info settings (Admin)")
    public ResponseEntity<ApiResponse<StoreInfoSettingsResponse>> getStoreInfo(HttpServletRequest httpServletRequest) {
        StoreInfoSettingsResponse response = storeInfoService.getStoreInfo();

        return ResponseEntity.ok(ApiResponse.success(
                200,
                "Store info retrieved successfully.",
                response,
                httpServletRequest.getRequestURI()
        ));
    }

    @PutMapping("/store-info")
    @Operation(summary = "Update store info settings (announcements, policies, contact info)")
    public ResponseEntity<ApiResponse<StoreInfoSettingsResponse>> updateStoreInfo(
            @Valid @RequestBody StoreInfoSettingsUpdateRequest request,
            HttpServletRequest httpServletRequest
    ) {
        StoreInfoSettingsResponse response = storeInfoService.updateStoreInfo(request);

        return ResponseEntity.ok(ApiResponse.success(
                200,
                "Store info updated successfully.",
                response,
                httpServletRequest.getRequestURI()
        ));
    }
}
