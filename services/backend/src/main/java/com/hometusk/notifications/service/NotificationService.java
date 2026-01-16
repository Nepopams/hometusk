package com.hometusk.notifications.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.households.domain.Household;
import com.hometusk.households.domain.HouseholdInvite;
import com.hometusk.notifications.domain.Notification;
import com.hometusk.notifications.domain.NotificationType;
import com.hometusk.notifications.dto.NotificationDto;
import com.hometusk.notifications.dto.NotificationPayloadDto;
import com.hometusk.notifications.repository.NotificationRepository;
import com.hometusk.shared.exception.ErrorCode;
import com.hometusk.shared.exception.NotFoundException;
import com.hometusk.shopping.domain.ShoppingItem;
import com.hometusk.tasks.domain.Task;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.User;
import com.hometusk.users.repository.MembershipRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String ENTITY_TYPE_TASK = "task";
    private static final String ENTITY_TYPE_SHOPPING_ITEM = "shopping_item";
    private static final String ENTITY_TYPE_INVITE = "invite";

    private final NotificationRepository notificationRepository;
    private final MembershipRepository membershipRepository;
    private final ObjectMapper objectMapper;

    public NotificationService(
            NotificationRepository notificationRepository,
            MembershipRepository membershipRepository,
            ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.membershipRepository = membershipRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> listNotifications(UUID householdId, UUID userId, Instant since, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Notification> notifications = since != null
                ? notificationRepository.findByHouseholdIdAndUserIdAndCreatedAtAfter(householdId, userId, since, pageable)
                : notificationRepository.findByHouseholdIdAndUserId(householdId, userId, pageable);

        return notifications.stream().map(this::toDto).toList();
    }

    @Transactional
    public NotificationDto markRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(
                        () -> new NotFoundException(ErrorCode.NOTIFICATION_NOT_FOUND, "Notification not found: " + notificationId));

        if (notification.getReadAt() == null) {
            notification.markRead(Instant.now());
            notificationRepository.save(notification);
        }

        return toDto(notification);
    }

    @Transactional
    public void notifyInviteAccepted(HouseholdInvite invite, User acceptedBy, UUID correlationId) {
        if (invite.getCreatedByUser() == null || acceptedBy == null) {
            return;
        }
        if (invite.getCreatedByUser().getId().equals(acceptedBy.getId())) {
            return;
        }

        String summary = "Invite accepted by " + safeDisplayName(acceptedBy);
        NotificationPayloadDto payload =
                new NotificationPayloadDto(acceptedBy.getId(), invite.getId(), ENTITY_TYPE_INVITE, summary);

        createNotification(invite.getHousehold(), invite.getCreatedByUser(), NotificationType.INVITE_ACCEPTED, payload, correlationId);
    }

    @Transactional
    public void notifyTaskAssigned(Task task, User actor, UUID correlationId) {
        if (task == null || task.getAssignee() == null) {
            return;
        }
        if (actor != null && task.getAssigneeId() != null && task.getAssigneeId().equals(actor.getId())) {
            return;
        }

        String summary = "Task assigned: " + safeText(task.getTitle());
        NotificationPayloadDto payload =
                new NotificationPayloadDto(actor != null ? actor.getId() : null, task.getId(), ENTITY_TYPE_TASK, summary);

        createNotification(task.getHousehold(), task.getAssignee(), NotificationType.TASK_ASSIGNED, payload, correlationId);
    }

    @Transactional
    public void notifyTaskCompleted(Task task, User actor, UUID correlationId) {
        if (task == null) {
            return;
        }

        String summary = "Task completed: " + safeText(task.getTitle());
        NotificationPayloadDto payload =
                new NotificationPayloadDto(actor != null ? actor.getId() : null, task.getId(), ENTITY_TYPE_TASK, summary);

        Set<UUID> recipients = new HashSet<>();
        if (task.getCreatedBy() != null) {
            recipients.add(task.getCreatedById());
        }
        if (task.getAssignee() != null) {
            recipients.add(task.getAssigneeId());
        }
        if (actor != null) {
            recipients.remove(actor.getId());
        }

        for (UUID recipientId : recipients) {
            User recipient = recipientId.equals(task.getCreatedById()) ? task.getCreatedBy() : task.getAssignee();
            if (recipient != null && recipient.getId().equals(recipientId)) {
                createNotification(task.getHousehold(), recipient, NotificationType.TASK_COMPLETED, payload, correlationId);
            }
        }
    }

    @Transactional
    public void notifyShoppingItemAdded(ShoppingItem item, User actor, UUID correlationId) {
        if (item == null) {
            return;
        }
        Household household = item.getShoppingList().getHousehold();
        NotificationPayloadDto payload = new NotificationPayloadDto(
                actor != null ? actor.getId() : null,
                item.getId(),
                ENTITY_TYPE_SHOPPING_ITEM,
                "Shopping item added: " + safeText(item.getName()));

        notifyHouseholdMembers(household, actor != null ? actor.getId() : null, NotificationType.SHOPPING_ITEM_ADDED, payload, correlationId);
    }

    @Transactional
    public void notifyShoppingItemPurchased(ShoppingItem item, User actor, UUID correlationId) {
        if (item == null) {
            return;
        }
        Household household = item.getShoppingList().getHousehold();
        NotificationPayloadDto payload = new NotificationPayloadDto(
                actor != null ? actor.getId() : null,
                item.getId(),
                ENTITY_TYPE_SHOPPING_ITEM,
                "Shopping item purchased: " + safeText(item.getName()));

        notifyHouseholdMembers(
                household,
                actor != null ? actor.getId() : null,
                NotificationType.SHOPPING_ITEM_PURCHASED,
                payload,
                correlationId);
    }

    private void notifyHouseholdMembers(
            Household household,
            UUID actorId,
            NotificationType type,
            NotificationPayloadDto payload,
            UUID correlationId) {
        List<Membership> memberships = membershipRepository.findByHouseholdId(household.getId());

        for (Membership membership : memberships) {
            User recipient = membership.getUser();
            if (recipient == null) {
                continue;
            }
            if (actorId != null && actorId.equals(recipient.getId())) {
                continue;
            }
            createNotification(household, recipient, type, payload, correlationId);
        }
    }

    private void createNotification(
            Household household,
            User recipient,
            NotificationType type,
            NotificationPayloadDto payload,
            UUID correlationId) {
        String payloadJson = toJson(payload);
        Notification notification = new Notification(household, recipient, type, payloadJson, correlationId);
        notificationRepository.save(notification);
    }

    private NotificationDto toDto(Notification notification) {
        return NotificationDto.from(notification, readPayload(notification.getPayloadJson()));
    }

    private String toJson(NotificationPayloadDto payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize notification payload", e);
            return "{}";
        }
    }

    private NotificationPayloadDto readPayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payloadJson, NotificationPayloadDto.class);
        } catch (Exception e) {
            log.warn("Failed to parse notification payload", e);
            return null;
        }
    }

    private String safeText(String value) {
        return value != null ? value : "";
    }

    private String safeDisplayName(User user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        return user.getId().toString();
    }
}
