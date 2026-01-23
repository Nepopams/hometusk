package com.hometusk.notifications.service;

import com.hometusk.notifications.dto.NotificationDto;
import com.hometusk.notifications.event.NotificationCreatedEvent;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SseNotificationService.class);

    private final ConcurrentMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void register(UUID userId, UUID householdId, SseEmitter emitter) {
        String key = buildKey(userId, householdId);
        SseEmitter existing = emitters.put(key, emitter);
        if (existing != null) {
            existing.complete();
        }
        log.debug("Registered SSE emitter: {}", key);
    }

    public void remove(UUID userId, UUID householdId) {
        String key = buildKey(userId, householdId);
        emitters.remove(key);
        log.debug("Removed SSE emitter: {}", key);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        NotificationDto notification = event.notification();
        String key = buildKey(notification.userId(), notification.householdId());
        SseEmitter emitter = emitters.get(key);

        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event().name("notification").data(notification));
            log.debug("Sent SSE notification: {}", notification.id());
        } catch (IOException e) {
            log.warn("Failed to send SSE notification, removing emitter: {}", key);
            emitters.remove(key);
        }
    }

    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        Map<String, String> payload = Map.of("timestamp", Instant.now().toString());
        emitters.forEach((key, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data(payload));
            } catch (IOException e) {
                log.debug("Heartbeat failed, removing emitter: {}", key);
                emitters.remove(key);
            }
        });
    }

    private String buildKey(UUID userId, UUID householdId) {
        return userId + ":" + householdId;
    }
}
