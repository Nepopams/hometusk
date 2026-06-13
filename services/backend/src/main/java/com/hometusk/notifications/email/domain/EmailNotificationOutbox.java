package com.hometusk.notifications.email.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_notification_outbox")
public class EmailNotificationOutbox {

    private static final int LAST_ERROR_LIMIT = 4000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "body_text", nullable = false, columnDefinition = "TEXT")
    private String bodyText;

    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private EmailNotificationStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(name = "context_type", length = 64)
    private String contextType;

    @Column(name = "context_id")
    private UUID contextId;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EmailNotificationOutbox() {}

    public EmailNotificationOutbox(
            String recipientEmail,
            String subject,
            String bodyText,
            String bodyHtml,
            String idempotencyKey,
            UUID correlationId,
            String contextType,
            UUID contextId,
            int maxAttempts,
            Instant createdAt) {
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.bodyText = bodyText;
        this.bodyHtml = bodyHtml;
        this.idempotencyKey = idempotencyKey;
        this.correlationId = correlationId;
        this.contextType = contextType;
        this.contextId = contextId;
        this.status = EmailNotificationStatus.PENDING;
        this.attemptCount = 0;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.nextAttemptAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getSubject() {
        return subject;
    }

    public String getBodyText() {
        return bodyText;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public EmailNotificationStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public String getContextType() {
        return contextType;
    }

    public UUID getContextId() {
        return contextId;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDue(Instant now) {
        return status.isDeliverable() && !nextAttemptAt.isAfter(now);
    }

    public void markDeliverySucceeded(Instant timestamp) {
        this.attemptCount++;
        this.status = EmailNotificationStatus.SENT;
        this.sentAt = timestamp;
        this.lastError = null;
        this.nextAttemptAt = timestamp;
        this.updatedAt = timestamp;
    }

    public void markDeliveryFailed(String error, Instant timestamp, Instant nextAttemptAt) {
        this.attemptCount++;
        this.lastError = truncate(error);
        this.updatedAt = timestamp;
        if (this.attemptCount >= this.maxAttempts) {
            this.status = EmailNotificationStatus.FAILED;
            this.nextAttemptAt = timestamp;
        } else {
            this.status = EmailNotificationStatus.RETRY_SCHEDULED;
            this.nextAttemptAt = nextAttemptAt;
        }
    }

    public void cancel(String reason, Instant timestamp) {
        if (this.status == EmailNotificationStatus.SENT || this.status == EmailNotificationStatus.FAILED) {
            return;
        }
        this.status = EmailNotificationStatus.CANCELLED;
        this.lastError = truncate(reason);
        this.updatedAt = timestamp;
    }

    private static String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "delivery failed";
        }
        String normalized = value.strip();
        if (normalized.length() <= LAST_ERROR_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, LAST_ERROR_LIMIT);
    }
}
