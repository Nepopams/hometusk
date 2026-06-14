package com.hometusk.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hometusk.notifications.email.domain.EmailNotificationStatus;
import com.hometusk.notifications.email.repository.EmailNotificationOutboxRepository;
import com.hometusk.notifications.email.service.TaskAssignmentEmailNotificationHandler;
import com.hometusk.tasks.event.TaskAssignedEvent;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import com.hometusk.users.domain.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DisplayName("Task assignment email notification integration tests")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TaskAssignmentEmailNotificationIntegrationTest extends IntegrationTestBase {

    @Autowired
    private EmailNotificationOutboxRepository outboxRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskAssignmentEmailNotificationHandler taskAssignmentEmailNotificationHandler;

    @Test
    @DisplayName("manual command assignment queues pending email for verified assignee")
    void manualCommandAssignment_queuesPendingEmail() throws Exception {
        addVerifiedMember(testUser2);
        Instant deadline = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);

        UUID taskId = createTask("Clean assigned room", testUser2.getId(), testZone.getId(), deadline, null);

        var emails = outboxRepository.findByContextTypeAndContextId("task", taskId);
        assertThat(emails).hasSize(1);
        var email = emails.get(0);
        assertThat(email.getStatus()).isEqualTo(EmailNotificationStatus.PENDING);
        assertThat(email.getRecipientEmail()).isEqualTo(testUser2.getEmail());
        assertThat(email.getSubject()).isEqualTo("HomeTusk: task assigned");
        assertThat(email.getBodyText()).contains("Clean assigned room", testHousehold.getName(), "Kitchen");
        assertThat(email.getBodyHtml()).contains("Clean assigned room", "Open task");
        assertThat(email.getIdempotencyKey()).startsWith("TASK_ASSIGNED:%s:%s:".formatted(taskId, testUser2.getId()));
    }

    @Test
    @DisplayName("duplicate command does not create duplicate assignment email")
    void duplicateCommand_doesNotCreateDuplicateAssignmentEmail() throws Exception {
        addVerifiedMember(testUser2);
        long before = outboxRepository.count();
        String idempotencyKey = "task-email-" + UUID.randomUUID();

        UUID taskId = createTask("Deduplicate assignment email", testUser2.getId(), null, null, idempotencyKey);
        createTask("Deduplicate assignment email", testUser2.getId(), null, null, idempotencyKey);

        assertThat(outboxRepository.findByContextTypeAndContextId("task", taskId))
                .hasSize(1);
        assertThat(outboxRepository.count()).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("duplicate task assigned event is suppressed by email idempotency key")
    void duplicateTaskAssignedEvent_isSuppressedByEmailIdempotencyKey() throws Exception {
        addVerifiedMember(testUser2);
        UUID taskId = createTask("Duplicate event assignment", testUser2.getId(), null, null, null);
        var task = taskRepository.findById(taskId).orElseThrow();
        long afterInitialEvent = outboxRepository.count();

        taskAssignmentEmailNotificationHandler.enqueueIfEligible(new TaskAssignedEvent(
                taskId,
                task.getTitle(),
                testHousehold.getId(),
                testHousehold.getName(),
                testUser2.getId(),
                testUser.getId(),
                testUser.getDisplayName(),
                null,
                null,
                task.getUpdatedAt(),
                UUID.randomUUID()));

        assertThat(outboxRepository.count()).isEqualTo(afterInitialEvent);
        assertThat(outboxRepository.findByContextTypeAndContextId("task", taskId))
                .hasSize(1);
    }

    @Test
    @DisplayName("self assignment skips email")
    void selfAssignment_skipsEmail() throws Exception {
        long before = outboxRepository.count();

        UUID taskId = createTask("Self assignment", testUser.getId(), null, null, null);

        assertThat(outboxRepository.findByContextTypeAndContextId("task", taskId))
                .isEmpty();
        assertThat(outboxRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("unverified assignee email skips email without breaking task creation")
    void unverifiedAssigneeEmail_skipsEmailWithoutBreakingTaskCreation() throws Exception {
        addMember(testUser2);
        long before = outboxRepository.count();

        UUID taskId = createTask("Unverified assignee", testUser2.getId(), null, null, null);

        var task = taskRepository.findById(taskId).orElseThrow();
        assertThat(task.getAssigneeId()).isEqualTo(testUser2.getId());
        assertThat(outboxRepository.findByContextTypeAndContextId("task", taskId))
                .isEmpty();
        assertThat(outboxRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("missing assignee email skips email without breaking task creation")
    void missingAssigneeEmail_skipsEmailWithoutBreakingTaskCreation() throws Exception {
        testUser2.setEmail(null);
        testUser2.setEmailVerified(true);
        testUser2 = userRepository.saveAndFlush(testUser2);
        addMember(testUser2);
        long before = outboxRepository.count();

        UUID taskId = createTask("Missing assignee email", testUser2.getId(), null, null, null);

        assertThat(taskRepository.findById(taskId)).isPresent();
        assertThat(outboxRepository.findByContextTypeAndContextId("task", taskId))
                .isEmpty();
        assertThat(outboxRepository.count()).isEqualTo(before);
    }

    private UUID createTask(String title, UUID assigneeId, UUID zoneId, Instant deadline, String idempotencyKey)
            throws Exception {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("title", title);
        if (assigneeId != null) {
            payload.put("assigneeId", assigneeId.toString());
        }
        if (zoneId != null) {
            payload.put("zoneId", zoneId.toString());
        }
        if (deadline != null) {
            payload.put("deadline", deadline.toString());
        }

        var request = Map.of(
                "householdId",
                testHousehold.getId().toString(),
                "type",
                "create_task",
                "payload",
                payload,
                "source",
                "api");

        var builder = post("/api/v1/commands")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
        if (idempotencyKey != null) {
            builder.header("Idempotency-Key", idempotencyKey);
        }

        var response = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("executed"))
                .andReturn();

        return UUID.fromString(objectMapper
                .readTree(response.getResponse().getContentAsString())
                .get("result")
                .get("taskId")
                .asText());
    }

    private void addVerifiedMember(User user) {
        user.setEmailVerified(true);
        userRepository.saveAndFlush(user);
        addMember(user);
    }

    private void addMember(User user) {
        if (!membershipRepository.existsByUser_IdAndHousehold_Id(user.getId(), testHousehold.getId())) {
            membershipRepository.saveAndFlush(new Membership(user, testHousehold, MembershipRole.member));
        }
    }
}
