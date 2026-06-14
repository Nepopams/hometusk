package com.hometusk.notifications.email.service;

import java.util.UUID;

public record EmailNotificationRequest(
        String recipientEmail,
        String subject,
        String bodyText,
        String bodyHtml,
        String idempotencyKey,
        UUID correlationId,
        String contextType,
        UUID contextId) {}
