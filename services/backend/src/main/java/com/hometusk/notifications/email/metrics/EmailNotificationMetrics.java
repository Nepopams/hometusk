package com.hometusk.notifications.email.metrics;

import com.hometusk.notifications.email.domain.EmailNotificationStatus;
import com.hometusk.notifications.email.repository.EmailNotificationOutboxRepository;
import com.hometusk.notifications.email.sender.EmailSendException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationMetrics {

    private static final String OUTBOX_COUNT = "email_notifications_outbox_count";
    private static final String ENQUEUED_TOTAL = "email_notifications_enqueued_total";
    private static final String DELIVERY_TOTAL = "email_notifications_delivery_total";
    private static final String FAILURES_TOTAL = "email_notifications_failures_total";

    private final MeterRegistry registry;

    public EmailNotificationMetrics(MeterRegistry registry, EmailNotificationOutboxRepository repository) {
        this.registry = registry;
        for (EmailNotificationStatus status : EmailNotificationStatus.values()) {
            Gauge.builder(OUTBOX_COUNT, repository, repo -> repo.countByStatus(status))
                    .tag("status", tag(status))
                    .description("Email notification outbox rows by status")
                    .register(registry);
        }
    }

    public void recordEnqueued(boolean duplicate) {
        Counter.builder(ENQUEUED_TOTAL)
                .tag("result", duplicate ? "duplicate" : "created")
                .description("Email notification enqueue requests")
                .register(registry)
                .increment();
    }

    public void recordDelivery(EmailNotificationStatus status) {
        Counter.builder(DELIVERY_TOTAL)
                .tag("status", tag(status))
                .description("Email notification delivery outcomes")
                .register(registry)
                .increment();
    }

    public void recordFailure(String reason) {
        Counter.builder(FAILURES_TOTAL)
                .tag("reason", reason)
                .description("Email notification delivery failures")
                .register(registry)
                .increment();
    }

    public static String reasonFromException(RuntimeException exception) {
        if (exception instanceof EmailSendException) {
            return "message_build_failed";
        }
        if (exception instanceof MailException) {
            return "provider_error";
        }
        return "internal";
    }

    private static String tag(EmailNotificationStatus status) {
        return status.name().toLowerCase(Locale.ROOT);
    }
}
