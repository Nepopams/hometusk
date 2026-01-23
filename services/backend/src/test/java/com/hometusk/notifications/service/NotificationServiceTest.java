package com.hometusk.notifications.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.households.domain.Household;
import com.hometusk.notifications.domain.Notification;
import com.hometusk.notifications.domain.NotificationType;
import com.hometusk.notifications.dto.NotificationDto;
import com.hometusk.notifications.dto.NotificationPayloadDto;
import com.hometusk.notifications.repository.NotificationRepository;
import com.hometusk.users.domain.User;
import com.hometusk.users.repository.MembershipRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository, membershipRepository, new ObjectMapper(), eventPublisher);
    }

    @Test
    @DisplayName("generateIdempotencyKey includes type, entityId, userId, and window start")
    void generateIdempotencyKey_shouldIncludeAllComponents() throws Exception {
        NotificationType type = NotificationType.TASK_ASSIGNED;
        UUID entityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2024-01-22T10:02:30Z");

        long windowStart = (timestamp.toEpochMilli() / 300000) * 300000;
        String expected = String.format("%s:%s:%s:%d", type.name().toLowerCase(), entityId, userId, windowStart);

        String actual = invokeGenerateKey(notificationService, type, entityId, userId, timestamp);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("generateIdempotencyKey returns same key within the same window")
    void generateIdempotencyKey_sameInputsSameWindow_shouldReturnSameKey() throws Exception {
        NotificationType type = NotificationType.TASK_ASSIGNED;
        UUID entityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Instant t1 = Instant.parse("2024-01-22T10:01:00Z");
        Instant t2 = Instant.parse("2024-01-22T10:04:59Z");

        String key1 = invokeGenerateKey(notificationService, type, entityId, userId, t1);
        String key2 = invokeGenerateKey(notificationService, type, entityId, userId, t2);

        assertThat(key1).isEqualTo(key2);
    }

    @Test
    @DisplayName("generateIdempotencyKey returns different key for different windows")
    void generateIdempotencyKey_differentWindow_shouldReturnDifferentKey() throws Exception {
        NotificationType type = NotificationType.TASK_ASSIGNED;
        UUID entityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Instant t1 = Instant.parse("2024-01-22T10:01:00Z");
        Instant t2 = Instant.parse("2024-01-22T10:06:00Z");

        String key1 = invokeGenerateKey(notificationService, type, entityId, userId, t1);
        String key2 = invokeGenerateKey(notificationService, type, entityId, userId, t2);

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("createNotification returns existing notification when duplicate key found")
    void createNotification_duplicateKey_shouldReturnExisting() throws Exception {
        Household household = new Household("Test Household");
        User recipient = new User("user-ext", "user@test.local", "User");
        setId(recipient, UUID.randomUUID());

        NotificationPayloadDto payload =
                new NotificationPayloadDto(UUID.randomUUID(), UUID.randomUUID(), "task", "Task assigned");

        Notification existing = new Notification(
                household, recipient, NotificationType.TASK_ASSIGNED, "{}", UUID.randomUUID(), "dup-key");

        when(notificationRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.of(existing));

        NotificationDto result = invokeCreateNotification(
                notificationService, household, recipient, NotificationType.TASK_ASSIGNED, payload);

        assertThat(result).isNotNull();
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("createNotification saves and publishes when key is new")
    void createNotification_newKey_shouldSaveAndPublish() throws Exception {
        Household household = new Household("Test Household");
        User recipient = new User("user-ext", "user@test.local", "User");
        setId(recipient, UUID.randomUUID());

        NotificationPayloadDto payload =
                new NotificationPayloadDto(UUID.randomUUID(), UUID.randomUUID(), "task", "Task assigned");

        Notification saved = new Notification(
                household, recipient, NotificationType.TASK_ASSIGNED, "{}", UUID.randomUUID(), "new-key");

        when(notificationRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        NotificationDto result = invokeCreateNotification(
                notificationService, household, recipient, NotificationType.TASK_ASSIGNED, payload);

        assertThat(result).isNotNull();
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("createNotification returns existing on race condition")
    void createNotification_raceCondition_shouldReturnExisting() throws Exception {
        Household household = new Household("Test Household");
        User recipient = new User("user-ext", "user@test.local", "User");
        setId(recipient, UUID.randomUUID());

        NotificationPayloadDto payload =
                new NotificationPayloadDto(UUID.randomUUID(), UUID.randomUUID(), "task", "Task assigned");

        Notification existing = new Notification(
                household, recipient, NotificationType.TASK_ASSIGNED, "{}", UUID.randomUUID(), "dup-key");

        when(notificationRepository.findByIdempotencyKey(anyString()))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        NotificationDto result = invokeCreateNotification(
                notificationService, household, recipient, NotificationType.TASK_ASSIGNED, payload);

        assertThat(result).isNotNull();
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    private static String invokeGenerateKey(
            NotificationService service, NotificationType type, UUID entityId, UUID userId, Instant timestamp)
            throws Exception {
        Method method = NotificationService.class.getDeclaredMethod(
                "generateIdempotencyKey", NotificationType.class, UUID.class, UUID.class, Instant.class);
        method.setAccessible(true);
        return (String) method.invoke(service, type, entityId, userId, timestamp);
    }

    private static NotificationDto invokeCreateNotification(
            NotificationService service,
            Household household,
            User recipient,
            NotificationType type,
            NotificationPayloadDto payload)
            throws Exception {
        Method method = NotificationService.class.getDeclaredMethod(
                "createNotification",
                Household.class,
                User.class,
                NotificationType.class,
                NotificationPayloadDto.class,
                UUID.class);
        method.setAccessible(true);
        return (NotificationDto) method.invoke(service, household, recipient, type, payload, UUID.randomUUID());
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}
