package com.hometusk.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.hometusk.notifications.email.domain.EmailNotificationStatus;
import com.hometusk.notifications.email.repository.EmailNotificationOutboxRepository;
import com.hometusk.notifications.email.sender.EmailMessage;
import com.hometusk.notifications.email.sender.EmailSender;
import com.hometusk.notifications.email.service.EmailNotificationDeliveryService;
import com.hometusk.notifications.email.service.EmailNotificationRequest;
import com.hometusk.notifications.email.service.EmailNotificationService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.TestPropertySource;

@DisplayName("Email notification outbox integration tests")
@TestPropertySource(
        properties = {"hometusk.email.batch-size=10", "hometusk.email.max-attempts=2", "hometusk.email.retry-delay-ms=0"
        })
class EmailNotificationOutboxIntegrationTest extends IntegrationTestBase {

    @Autowired
    private EmailNotificationService emailNotificationService;

    @Autowired
    private EmailNotificationDeliveryService deliveryService;

    @Autowired
    private EmailNotificationOutboxRepository outboxRepository;

    @MockBean
    private EmailSender emailSender;

    @Test
    @DisplayName("enqueue stores pending email without sending immediately")
    void enqueue_storesPendingEmailWithoutImmediateSend() {
        var result = emailNotificationService.enqueue(request("system:welcome:" + UUID.randomUUID()));

        assertThat(result.duplicate()).isFalse();
        assertThat(result.status()).isEqualTo(EmailNotificationStatus.PENDING);

        var notification = outboxRepository.findById(result.id()).orElseThrow();
        assertThat(notification.getRecipientEmail()).isEqualTo("user@example.test");
        assertThat(notification.getSubject()).isEqualTo("Welcome");
        assertThat(notification.getStatus()).isEqualTo(EmailNotificationStatus.PENDING);
        assertThat(notification.getAttemptCount()).isZero();
        verifyNoInteractions(emailSender);
    }

    @Test
    @DisplayName("duplicate enqueue by idempotency key returns existing row")
    void enqueue_duplicateKey_returnsExistingRow() {
        String idempotencyKey = "system:duplicate:" + UUID.randomUUID();

        var first = emailNotificationService.enqueue(request(idempotencyKey));
        var second = emailNotificationService.enqueue(request(idempotencyKey));

        assertThat(second.duplicate()).isTrue();
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(outboxRepository.count()).isEqualTo(1);
        verifyNoInteractions(emailSender);
    }

    @Test
    @DisplayName("delivery sends due pending email and marks it sent")
    void delivery_sendsPendingEmailAndMarksSent() {
        var enqueueResult = emailNotificationService.enqueue(request("system:send:" + UUID.randomUUID()));

        var deliveryResult = deliveryService.deliverDueEmails();

        assertThat(deliveryResult.candidates()).isEqualTo(1);
        assertThat(deliveryResult.sent()).isEqualTo(1);
        assertThat(deliveryResult.errors()).isZero();

        var messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().recipientEmail()).isEqualTo("user@example.test");
        assertThat(messageCaptor.getValue().subject()).isEqualTo("Welcome");

        var notification = outboxRepository.findById(enqueueResult.id()).orElseThrow();
        assertThat(notification.getStatus()).isEqualTo(EmailNotificationStatus.SENT);
        assertThat(notification.getAttemptCount()).isEqualTo(1);
        assertThat(notification.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("provider outage schedules retry and then fails at retry limit without throwing")
    void delivery_providerOutage_retriesAndFailsAtLimitWithoutThrowing() {
        var enqueueResult = emailNotificationService.enqueue(request("system:outage:" + UUID.randomUUID()));
        doThrow(new MailSendException("smtp down")).when(emailSender).send(any(EmailMessage.class));

        assertThatCode(() -> {
                    var firstRun = deliveryService.deliverDueEmails();
                    assertThat(firstRun.retryScheduled()).isEqualTo(1);
                    assertThat(firstRun.errors()).isZero();

                    var secondRun = deliveryService.deliverDueEmails();
                    assertThat(secondRun.failed()).isEqualTo(1);
                    assertThat(secondRun.errors()).isZero();
                })
                .doesNotThrowAnyException();

        var notification = outboxRepository.findById(enqueueResult.id()).orElseThrow();
        assertThat(notification.getStatus()).isEqualTo(EmailNotificationStatus.FAILED);
        assertThat(notification.getAttemptCount()).isEqualTo(2);
        assertThat(notification.getLastError()).contains("smtp down");
    }

    private static EmailNotificationRequest request(String idempotencyKey) {
        return new EmailNotificationRequest(
                "USER@EXAMPLE.TEST",
                "Welcome",
                "Hello from HomeTusk",
                "<p>Hello from HomeTusk</p>",
                idempotencyKey,
                UUID.randomUUID(),
                "system",
                UUID.randomUUID());
    }
}
