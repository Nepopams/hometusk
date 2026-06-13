package com.hometusk.notifications.email.scheduler;

import com.hometusk.notifications.email.service.EmailNotificationDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "hometusk.email.enabled", havingValue = "true")
public class EmailNotificationDeliveryJob {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationDeliveryJob.class);

    private final EmailNotificationDeliveryService deliveryService;

    public EmailNotificationDeliveryJob(EmailNotificationDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @Scheduled(fixedRateString = "${hometusk.email.fixed-rate-ms:60000}")
    public void deliverDueEmails() {
        var result = deliveryService.deliverDueEmails();
        if (result.candidates() > 0) {
            log.info(
                    "Email notification delivery run finished: candidates={}, sent={}, retryScheduled={}, failed={}, skipped={}, errors={}",
                    result.candidates(),
                    result.sent(),
                    result.retryScheduled(),
                    result.failed(),
                    result.skipped(),
                    result.errors());
        }
    }
}
