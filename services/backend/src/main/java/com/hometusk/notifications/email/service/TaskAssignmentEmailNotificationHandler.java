package com.hometusk.notifications.email.service;

import com.hometusk.notifications.email.config.TaskAssignmentEmailProperties;
import com.hometusk.notifications.email.template.EmailTemplateRenderer;
import com.hometusk.tasks.event.TaskAssignedEvent;
import com.hometusk.users.domain.User;
import com.hometusk.users.repository.MembershipRepository;
import com.hometusk.users.repository.UserRepository;
import com.hometusk.users.service.EmailNotificationEligibilityPolicy;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class TaskAssignmentEmailNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(TaskAssignmentEmailNotificationHandler.class);
    private static final String CONTEXT_TYPE_TASK = "task";

    private final EmailNotificationService emailNotificationService;
    private final EmailTemplateRenderer templateRenderer;
    private final EmailNotificationEligibilityPolicy eligibilityPolicy;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final TaskAssignmentEmailProperties properties;
    private final TransactionTemplate transactionTemplate;

    public TaskAssignmentEmailNotificationHandler(
            EmailNotificationService emailNotificationService,
            EmailTemplateRenderer templateRenderer,
            EmailNotificationEligibilityPolicy eligibilityPolicy,
            UserRepository userRepository,
            MembershipRepository membershipRepository,
            TaskAssignmentEmailProperties properties,
            PlatformTransactionManager transactionManager) {
        this.emailNotificationService = emailNotificationService;
        this.templateRenderer = templateRenderer;
        this.eligibilityPolicy = eligibilityPolicy;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskAssigned(TaskAssignedEvent event) {
        try {
            enqueueIfEligible(event);
        } catch (RuntimeException e) {
            log.warn(
                    "Task assignment email enqueue failed after task assignment commit: taskId={}, assigneeId={}, correlationId={}",
                    event.taskId(),
                    event.assigneeId(),
                    event.correlationId(),
                    e);
        }
    }

    public void enqueueIfEligible(TaskAssignedEvent event) {
        if (!properties.isEnabled()) {
            log.debug("Task assignment email disabled: taskId={}", event.taskId());
            return;
        }
        if (event.assigneeId() == null) {
            return;
        }
        if (properties.isSkipSelfNotifications()
                && event.actorId() != null
                && event.actorId().equals(event.assigneeId())) {
            log.debug(
                    "Skipping self task assignment email: taskId={}, assigneeId={}",
                    event.taskId(),
                    event.assigneeId());
            return;
        }
        if (!membershipRepository.existsByUser_IdAndHousehold_Id(event.assigneeId(), event.householdId())) {
            log.warn(
                    "Skipping task assignment email for non-member assignee: taskId={}, assigneeId={}, householdId={}",
                    event.taskId(),
                    event.assigneeId(),
                    event.householdId());
            return;
        }

        User assignee = userRepository.findById(event.assigneeId()).orElse(null);
        if (!eligibilityPolicy.isEligible(assignee)) {
            log.debug(
                    "Skipping task assignment email for ineligible assignee: taskId={}, assigneeId={}",
                    event.taskId(),
                    event.assigneeId());
            return;
        }

        Map<String, String> textVariables = textVariables(event, assignee);
        Map<String, String> htmlVariables = htmlVariables(textVariables);
        var textContent = templateRenderer.render(
                properties.getSubjectTemplate(), properties.getBodyTextTemplate(), null, textVariables);
        var htmlContent = templateRenderer.render(null, null, properties.getBodyHtmlTemplate(), htmlVariables);

        transactionTemplate.executeWithoutResult(
                status -> emailNotificationService.enqueue(new EmailNotificationRequest(
                        assignee.getEmail(),
                        textContent.subject(),
                        textContent.bodyText(),
                        htmlContent.bodyHtml(),
                        idempotencyKey(event),
                        event.correlationId(),
                        CONTEXT_TYPE_TASK,
                        event.taskId())));
    }

    private Map<String, String> textVariables(TaskAssignedEvent event, User assignee) {
        String taskId = event.taskId().toString();
        String householdId = event.householdId().toString();
        return Map.of(
                "assigneeName", safeText(assignee.getDisplayName(), "there"),
                "actorName", safeText(event.actorName(), "Someone"),
                "householdName", safeText(event.householdName(), "your household"),
                "taskTitle", safeText(event.taskTitle(), "Untitled task"),
                "zoneName", safeText(event.zoneName(), "Not set"),
                "deadline",
                        event.deadline() != null ? DateTimeFormatter.ISO_INSTANT.format(event.deadline()) : "Not set",
                "taskId", taskId,
                "householdId", householdId,
                "taskUrl", properties.buildTaskUrl(householdId, taskId));
    }

    private Map<String, String> htmlVariables(Map<String, String> variables) {
        return variables.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> escapeHtml(entry.getValue())));
    }

    private String idempotencyKey(TaskAssignedEvent event) {
        return "TASK_ASSIGNED:%s:%s:%d"
                .formatted(
                        event.taskId(),
                        event.assigneeId(),
                        event.assignmentTimestamp().toEpochMilli());
    }

    private static String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.strip();
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
