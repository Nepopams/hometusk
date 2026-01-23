package com.hometusk.notifications.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.hometusk.notifications.dto.NotificationDto;
import com.hometusk.notifications.dto.NotificationPayloadDto;
import com.hometusk.notifications.event.NotificationCreatedEvent;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseNotificationServiceTest {

    @Test
    void register_shouldStoreEmitter() throws Exception {
        SseNotificationService service = new SseNotificationService();
        UUID userId = UUID.randomUUID();
        UUID householdId = UUID.randomUUID();

        service.register(userId, householdId, new TestEmitter());

        ConcurrentMap<String, SseEmitter> emitters = getEmitters(service);
        assertThat(emitters).containsKey(userId + ":" + householdId);
    }

    @Test
    void remove_shouldRemoveEmitter() throws Exception {
        SseNotificationService service = new SseNotificationService();
        UUID userId = UUID.randomUUID();
        UUID householdId = UUID.randomUUID();

        service.register(userId, householdId, new TestEmitter());
        service.remove(userId, householdId);

        ConcurrentMap<String, SseEmitter> emitters = getEmitters(service);
        assertThat(emitters).isEmpty();
    }

    @Test
    void handleNotificationCreated_shouldSendToRegisteredEmitter() {
        SseNotificationService service = new SseNotificationService();
        UUID userId = UUID.randomUUID();
        UUID householdId = UUID.randomUUID();
        TestEmitter emitter = new TestEmitter();

        service.register(userId, householdId, emitter);
        NotificationCreatedEvent event = new NotificationCreatedEvent(sampleNotification(userId, householdId));

        service.handleNotificationCreated(event);

        assertThat(emitter.getSendCount()).isEqualTo(1);
    }

    @Test
    void handleNotificationCreated_shouldNotFailIfNoEmitter() {
        SseNotificationService service = new SseNotificationService();
        NotificationCreatedEvent event =
                new NotificationCreatedEvent(sampleNotification(UUID.randomUUID(), UUID.randomUUID()));

        assertThatNoException().isThrownBy(() -> service.handleNotificationCreated(event));
    }

    @Test
    void handleNotificationCreated_shouldRemoveEmitterOnError() throws Exception {
        SseNotificationService service = new SseNotificationService();
        UUID userId = UUID.randomUUID();
        UUID householdId = UUID.randomUUID();

        service.register(userId, householdId, new TestEmitter(true));
        NotificationCreatedEvent event = new NotificationCreatedEvent(sampleNotification(userId, householdId));

        service.handleNotificationCreated(event);

        ConcurrentMap<String, SseEmitter> emitters = getEmitters(service);
        assertThat(emitters).isEmpty();
    }

    @Test
    void sendHeartbeat_shouldSendToAllEmitters() {
        SseNotificationService service = new SseNotificationService();
        TestEmitter emitterA = new TestEmitter();
        TestEmitter emitterB = new TestEmitter();

        service.register(UUID.randomUUID(), UUID.randomUUID(), emitterA);
        service.register(UUID.randomUUID(), UUID.randomUUID(), emitterB);

        service.sendHeartbeat();

        assertThat(emitterA.getSendCount()).isEqualTo(1);
        assertThat(emitterB.getSendCount()).isEqualTo(1);
    }

    private static NotificationDto sampleNotification(UUID userId, UUID householdId) {
        NotificationPayloadDto payload =
                new NotificationPayloadDto(UUID.randomUUID(), UUID.randomUUID(), "task", "Test");
        return new NotificationDto(
                UUID.randomUUID(),
                householdId,
                userId,
                "task_assigned",
                payload,
                Instant.now(),
                null,
                UUID.randomUUID());
    }

    @SuppressWarnings("unchecked")
    private static ConcurrentMap<String, SseEmitter> getEmitters(SseNotificationService service) throws Exception {
        Field field = SseNotificationService.class.getDeclaredField("emitters");
        field.setAccessible(true);
        return (ConcurrentMap<String, SseEmitter>) field.get(service);
    }

    private static final class TestEmitter extends SseEmitter {

        private final AtomicInteger sendCount = new AtomicInteger();
        private final boolean throwOnSend;

        private TestEmitter() {
            this(false);
        }

        private TestEmitter(boolean throwOnSend) {
            super(0L);
            this.throwOnSend = throwOnSend;
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (throwOnSend) {
                throw new IOException("Simulated send failure");
            }
            sendCount.incrementAndGet();
        }

        private int getSendCount() {
            return sendCount.get();
        }
    }
}
