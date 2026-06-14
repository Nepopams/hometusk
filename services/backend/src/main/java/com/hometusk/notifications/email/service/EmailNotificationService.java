package com.hometusk.notifications.email.service;

import com.hometusk.notifications.email.config.EmailNotificationProperties;
import com.hometusk.notifications.email.domain.EmailNotificationOutbox;
import com.hometusk.notifications.email.metrics.EmailNotificationMetrics;
import com.hometusk.notifications.email.repository.EmailNotificationOutboxRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final Pattern SIMPLE_EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final EmailNotificationOutboxRepository repository;
    private final EmailNotificationProperties properties;
    private final EmailNotificationMetrics metrics;

    public EmailNotificationService(
            EmailNotificationOutboxRepository repository,
            EmailNotificationProperties properties,
            EmailNotificationMetrics metrics) {
        this.repository = repository;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Transactional
    public EmailNotificationEnqueueResult enqueue(EmailNotificationRequest request) {
        NormalizedEmailNotificationRequest normalized = normalize(request);
        var existing = repository.findByIdempotencyKey(normalized.idempotencyKey());
        if (existing.isPresent()) {
            metrics.recordEnqueued(true);
            return toResult(existing.get(), true);
        }

        Instant now = Instant.now();
        EmailNotificationOutbox notification = new EmailNotificationOutbox(
                normalized.recipientEmail(),
                normalized.subject(),
                normalized.bodyText(),
                normalized.bodyHtml(),
                normalized.idempotencyKey(),
                normalized.correlationId(),
                normalized.contextType(),
                normalized.contextId(),
                properties.getMaxAttempts(),
                now);

        try {
            EmailNotificationOutbox saved = repository.saveAndFlush(notification);
            metrics.recordEnqueued(false);
            log.info(
                    "Email notification enqueued: notificationId={}, contextType={}, contextId={}, correlationId={}",
                    saved.getId(),
                    saved.getContextType(),
                    saved.getContextId(),
                    saved.getCorrelationId());
            return toResult(saved, false);
        } catch (DataIntegrityViolationException e) {
            EmailNotificationOutbox duplicate =
                    repository.findByIdempotencyKey(normalized.idempotencyKey()).orElseThrow(() -> e);
            metrics.recordEnqueued(true);
            return toResult(duplicate, true);
        }
    }

    private static EmailNotificationEnqueueResult toResult(EmailNotificationOutbox notification, boolean duplicate) {
        return new EmailNotificationEnqueueResult(notification.getId(), notification.getStatus(), duplicate);
    }

    private static NormalizedEmailNotificationRequest normalize(EmailNotificationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Email notification request is required");
        }

        String recipientEmail =
                requireText(request.recipientEmail(), "recipientEmail").toLowerCase(Locale.ROOT);
        if (recipientEmail.length() > 255
                || !SIMPLE_EMAIL_PATTERN.matcher(recipientEmail).matches()) {
            throw new IllegalArgumentException("recipientEmail must be a valid email address");
        }

        String subject = requireText(request.subject(), "subject");
        if (subject.length() > 255 || subject.contains("\r") || subject.contains("\n")) {
            throw new IllegalArgumentException("subject must be 255 characters or fewer and contain no line breaks");
        }

        String bodyText = requireText(request.bodyText(), "bodyText");
        String idempotencyKey = requireText(request.idempotencyKey(), "idempotencyKey");
        if (idempotencyKey.length() > 255) {
            throw new IllegalArgumentException("idempotencyKey must be 255 characters or fewer");
        }

        String bodyHtml = blankToNull(request.bodyHtml());
        String contextType = blankToNull(request.contextType());
        if (contextType != null && contextType.length() > 64) {
            throw new IllegalArgumentException("contextType must be 64 characters or fewer");
        }

        return new NormalizedEmailNotificationRequest(
                recipientEmail,
                subject,
                bodyText,
                bodyHtml,
                idempotencyKey,
                request.correlationId(),
                contextType,
                request.contextId());
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private record NormalizedEmailNotificationRequest(
            String recipientEmail,
            String subject,
            String bodyText,
            String bodyHtml,
            String idempotencyKey,
            UUID correlationId,
            String contextType,
            UUID contextId) {}
}
