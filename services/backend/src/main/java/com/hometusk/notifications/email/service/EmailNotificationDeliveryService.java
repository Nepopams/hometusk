package com.hometusk.notifications.email.service;

import com.hometusk.notifications.email.config.EmailNotificationProperties;
import com.hometusk.notifications.email.domain.EmailNotificationOutbox;
import com.hometusk.notifications.email.domain.EmailNotificationStatus;
import com.hometusk.notifications.email.metrics.EmailNotificationMetrics;
import com.hometusk.notifications.email.repository.EmailNotificationOutboxRepository;
import com.hometusk.notifications.email.sender.EmailMessage;
import com.hometusk.notifications.email.sender.EmailSender;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EmailNotificationDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationDeliveryService.class);
    private static final List<EmailNotificationStatus> DUE_STATUSES =
            List.of(EmailNotificationStatus.PENDING, EmailNotificationStatus.RETRY_SCHEDULED);

    private final EmailNotificationOutboxRepository repository;
    private final EmailNotificationProperties properties;
    private final EmailNotificationMetrics metrics;
    private final EmailSender sender;
    private final TransactionTemplate transactionTemplate;

    public EmailNotificationDeliveryService(
            EmailNotificationOutboxRepository repository,
            EmailNotificationProperties properties,
            EmailNotificationMetrics metrics,
            EmailSender sender,
            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.properties = properties;
        this.metrics = metrics;
        this.sender = sender;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public DeliveryResult deliverDueEmails() {
        List<UUID> dueIds = repository.findDueDeliveryIds(
                DUE_STATUSES, Instant.now(), PageRequest.of(0, properties.getBatchSize()));

        int sent = 0;
        int retryScheduled = 0;
        int failed = 0;
        int skipped = 0;
        int errors = 0;

        for (UUID notificationId : dueIds) {
            try {
                DeliveryOutcome outcome = transactionTemplate.execute(status -> deliverOne(notificationId));
                if (outcome == DeliveryOutcome.SENT) {
                    sent++;
                } else if (outcome == DeliveryOutcome.RETRY_SCHEDULED) {
                    retryScheduled++;
                } else if (outcome == DeliveryOutcome.FAILED) {
                    failed++;
                } else {
                    skipped++;
                }
            } catch (RuntimeException e) {
                errors++;
                metrics.recordFailure("unexpected");
                log.error("Email notification delivery failed unexpectedly: notificationId={}", notificationId, e);
            }
        }

        return new DeliveryResult(dueIds.size(), sent, retryScheduled, failed, skipped, errors);
    }

    private DeliveryOutcome deliverOne(UUID notificationId) {
        EmailNotificationOutbox notification =
                repository.findByIdForUpdate(notificationId).orElse(null);
        if (notification == null || !notification.isDue(Instant.now())) {
            return DeliveryOutcome.SKIPPED;
        }

        try {
            sender.send(new EmailMessage(
                    notification.getRecipientEmail(),
                    notification.getSubject(),
                    notification.getBodyText(),
                    notification.getBodyHtml()));
            Instant now = Instant.now();
            notification.markDeliverySucceeded(now);
            metrics.recordDelivery(EmailNotificationStatus.SENT);
            log.info(
                    "Email notification sent: notificationId={}, attemptCount={}, correlationId={}",
                    notification.getId(),
                    notification.getAttemptCount(),
                    notification.getCorrelationId());
            return DeliveryOutcome.SENT;
        } catch (RuntimeException e) {
            Instant now = Instant.now();
            notification.markDeliveryFailed(e.getMessage(), now, now.plus(properties.retryDelay()));
            String reason = EmailNotificationMetrics.reasonFromException(e);
            metrics.recordFailure(reason);
            metrics.recordDelivery(notification.getStatus());
            log.warn(
                    "Email notification delivery attempt failed: notificationId={}, status={}, attemptCount={}, maxAttempts={}, reason={}",
                    notification.getId(),
                    notification.getStatus(),
                    notification.getAttemptCount(),
                    notification.getMaxAttempts(),
                    reason);
            return notification.getStatus() == EmailNotificationStatus.FAILED
                    ? DeliveryOutcome.FAILED
                    : DeliveryOutcome.RETRY_SCHEDULED;
        }
    }

    private enum DeliveryOutcome {
        SENT,
        RETRY_SCHEDULED,
        FAILED,
        SKIPPED
    }

    public record DeliveryResult(int candidates, int sent, int retryScheduled, int failed, int skipped, int errors) {}
}
