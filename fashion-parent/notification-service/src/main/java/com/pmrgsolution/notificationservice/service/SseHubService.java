package com.pmrgsolution.notificationservice.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseHubService {

    SseEmitter createEmitter();

    void broadcast(String eventType, Object data);
}
