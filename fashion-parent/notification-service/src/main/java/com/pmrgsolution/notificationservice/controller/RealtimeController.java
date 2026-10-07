package com.pmrgsolution.notificationservice.controller;

import com.pmrgsolution.notificationservice.service.SseHubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/realtime")
@Tag(name = "Realtime Streaming", description = "Server-Sent Events (SSE) stream for admin dashboard")
@SecurityRequirement(name = "BearerAuth")
public class RealtimeController {

    private final SseHubService sseHubService;

    public RealtimeController(SseHubService sseHubService) {
        this.sseHubService = sseHubService;
    }

    @GetMapping(value = "/admin/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to live admin event stream (New Orders, Inquiries, Stock alerts)")
    public SseEmitter subscribeAdminStream() {
        return sseHubService.createEmitter();
    }
}
