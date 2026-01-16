package com.hometusk.notifications.dto;

import com.hometusk.notifications.domain.Notification;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Notification")
public record NotificationDto(
        @Schema(description = "Notification ID") UUID id,
        @Schema(description = "Household ID") UUID householdId,
        @Schema(description = "Recipient user ID") UUID userId,
        @Schema(description = "Notification type") String type,
        @Schema(description = "Notification payload") NotificationPayloadDto payload,
        @Schema(description = "Creation timestamp") Instant createdAt,
        @Schema(description = "Read timestamp") Instant readAt,
        @Schema(description = "Correlation ID for traceability") UUID correlationId) {

    public static NotificationDto from(Notification notification, NotificationPayloadDto payload) {
        return new NotificationDto(
                notification.getId(),
                notification.getHouseholdId(),
                notification.getUserId(),
                notification.getType().name().toLowerCase(),
                payload,
                notification.getCreatedAt(),
                notification.getReadAt(),
                notification.getCorrelationId());
    }
}
