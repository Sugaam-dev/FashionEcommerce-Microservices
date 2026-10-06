package com.pmrgsolution.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseHubServiceImpl implements SseHubService {

    private static final Logger log = LoggerFactory.getLogger(SseHubServiceImpl.class);
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @Override
    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitter.onCompletion(() -> {
            log.debug("SSE emitter completed");
            emitters.remove(emitter);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE emitter timed out");
            emitters.remove(emitter);
        });

        emitter.onError(ex -> {
            log.debug("SSE emitter error: {}", ex.getMessage());
            emitters.remove(emitter);
        });

        emitters.add(emitter);

        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected to Realtime Notification Stream"));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE handshake event", e);
            emitters.remove(emitter);
        }

        return emitter;
    }

    @Override
    public void broadcast(String eventType, Object data) {
        if (emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventType).data(data));
            } catch (IOException | IllegalStateException e) {
                log.debug("Removing broken SSE emitter after delivery failure: {}", e.getMessage());
                emitters.remove(emitter);
            }
        }
    }
}
