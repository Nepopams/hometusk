package com.hometusk.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Notification payload")
public record NotificationPayloadDto(
        @Schema(description = "User who triggered the event") UUID actorUserId,
        @Schema(description = "Entity ID for this notification") UUID entityId,
        @Schema(description = "Entity type (task, shopping_item, invite)") String entityType,
        @Schema(description = "Human-readable summary") String summary) {}
