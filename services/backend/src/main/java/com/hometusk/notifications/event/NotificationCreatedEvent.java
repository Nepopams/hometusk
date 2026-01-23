package com.hometusk.notifications.event;

import com.hometusk.notifications.dto.NotificationDto;

public record NotificationCreatedEvent(NotificationDto notification) {}
