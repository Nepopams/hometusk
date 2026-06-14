package com.hometusk.notifications.email.service;

import com.hometusk.notifications.email.domain.EmailNotificationStatus;
import java.util.UUID;

public record EmailNotificationEnqueueResult(UUID id, EmailNotificationStatus status, boolean duplicate) {}
