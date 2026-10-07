package com.pmrgsolution.notificationservice.controller;

import com.pmrgsolution.notificationservice.dto.response.EmailQuotaResponse;
import com.pmrgsolution.notificationservice.entity.EmailUsageQuota;
import com.pmrgsolution.notificationservice.repository.EmailUsageQuotaRepository;
import com.pmrgsolution.shared.core.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1/email")
@Tag(name = "Email Status", description = "Public email service status and quota availability")
public class EmailStatusController {

    private final EmailUsageQuotaRepository emailUsageQuotaRepository;

    @Value("${app.mail.daily-limit:3000}")
    private int defaultDailyLimit;

    public EmailStatusController(EmailUsageQuotaRepository emailUsageQuotaRepository) {
        this.emailUsageQuotaRepository = emailUsageQuotaRepository;
    }

    @GetMapping("/status")
    @Operation(summary = "Get current daily email quota status")
    public ResponseEntity<ApiResponse<EmailQuotaResponse>> getEmailStatus(HttpServletRequest httpServletRequest) {
        String todayKey = LocalDate.now(ZoneOffset.UTC).toString();

        EmailUsageQuota quota = emailUsageQuotaRepository.findByDateKey(todayKey)
                .orElse(EmailUsageQuota.builder()
                        .dateKey(todayKey)
                        .count(0)
                        .dailyLimit(defaultDailyLimit)
                        .lastSentAt(Instant.now())
                        .build());

        EmailQuotaResponse response = EmailQuotaResponse.fromEntity(quota);

        return ResponseEntity.ok(ApiResponse.success(
                200,
                "Email quota status retrieved successfully.",
                response,
                httpServletRequest.getRequestURI()
        ));
    }
}
