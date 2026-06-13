package com.hometusk.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hometusk.activity.repository.TaskActivityRepository;
import com.hometusk.commands.domain.CommandStatus;
import com.hometusk.commands.repository.CommandRepository;
import com.hometusk.commands.repository.DecisionLogRepository;
import com.hometusk.commands.service.CommandSchedulerService;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

@DisplayName("Command Pipeline Integration Tests")
class CommandPipelineTest extends IntegrationTestBase {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CommandRepository commandRepository;

    @Autowired
    private DecisionLogRepository decisionLogRepository;

    @Autowired
    private TaskActivityRepository taskActivityRepository;

    @Autowired
    private CommandSchedulerService commandSchedulerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("POST /api/v1/commands - create_task")
    class CreateTaskTests {

        @Test
        @DisplayName("Should create task successfully with minimal payload")
        void createTask_minimal_success() throws Exception {
            String correlationId = randomCorrelationId();

            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Clean the kitchen"),
                    "source",
                    "api");

            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Correlation-ID", correlationId))
                    .andExpect(jsonPath("$.commandId").isNotEmpty())
                    .andExpect(jsonPath("$.correlationId").value(correlationId))
                    .andExpect(jsonPath("$.status").value("executed"))
                    .andExpect(jsonPath("$.result.taskId").isNotEmpty())
                    .andExpect(jsonPath("$.result.assigneeId")
                            .value(testUser.getId().toString()))
                    .andExpect(jsonPath("$.result.decisionConfidence").value(1.0))
                    .andExpect(jsonPath("$.executionMs").isNumber());

            // Verify task was created in DB
            var tasks = taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
            assertThat(tasks).hasSize(1);
            assertThat(tasks.get(0).getTitle()).isEqualTo("Clean the kitchen");
            assertThat(tasks.get(0).getCommandId()).isNotNull();

            // Verify decision log was created
            var decisionLogs = decisionLogRepository.findAll();
            assertThat(decisionLogs).hasSize(1);
            assertThat(decisionLogs.get(0).isSchemaValid()).isTrue();
            assertThat(decisionLogs.get(0).isBusinessValid()).isTrue();

            // Verify activity was recorded
            var activities =
                    taskActivityRepository.findByCorrelationIdOrderByCreatedAtDesc(UUID.fromString(correlationId));
            assertThat(activities).hasSize(1);
            assertThat(activities.get(0).getActivityType().name()).isEqualTo("TASK_CREATED");
        }

        @Test
        @DisplayName("Should create task with all optional fields")
        void createTask_fullPayload_success() throws Exception {
            // Add testUser2 to household for assignee test
            membershipRepository.save(new Membership(testUser2, testHousehold, MembershipRole.member));

            var deadline = Instant.now().plus(1, ChronoUnit.DAYS).toString();

            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of(
                            "title",
                            "Deep clean the kitchen",
                            "description",
                            "Scrub all surfaces",
                            "assigneeId",
                            testUser2.getId().toString(),
                            "zoneId",
                            testZone.getId().toString(),
                            "deadline",
                            deadline),
                    "source",
                    "web");

            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.assigneeId")
                            .value(testUser2.getId().toString()));

            var task = taskRepository
                    .findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId())
                    .get(0);
            assertThat(task.getDescription()).isEqualTo("Scrub all surfaces");
            assertThat(task.getAssigneeId()).isEqualTo(testUser2.getId());
            assertThat(task.getZoneId()).isEqualTo(testZone.getId());
            assertThat(task.getDeadline()).isNotNull();
        }

        @Test
        @DisplayName("Should create task with command-level structured attributes")
        void createTask_commandLevelAttributes_success() throws Exception {
            membershipRepository.save(new Membership(testUser2, testHousehold, MembershipRole.member));
            Instant dueDate = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);

            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Clean command attributes"),
                    "dueDate",
                    dueDate.toString(),
                    "assigneeId",
                    testUser2.getId().toString(),
                    "zoneId",
                    testZone.getId().toString(),
                    "source",
                    "web");

            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("executed"))
                    .andExpect(jsonPath("$.result.assigneeId")
                            .value(testUser2.getId().toString()));

            var task = taskRepository
                    .findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId())
                    .get(0);
            assertThat(task.getTitle()).isEqualTo("Clean command attributes");
            assertThat(task.getAssigneeId()).isEqualTo(testUser2.getId());
            assertThat(task.getZoneId()).isEqualTo(testZone.getId());
            assertThat(task.getDeadline()).isEqualTo(dueDate);

            var command = commandRepository.findById(task.getCommandId()).orElseThrow();
            assertThat(command.getDueDate()).isEqualTo(dueDate);
            assertThat(command.getAssigneeId()).isEqualTo(testUser2.getId());
            assertThat(command.getZoneId()).isEqualTo(testZone.getId());
        }

        @Test
        @DisplayName("Should reject conflicting command-level and payload attributes")
        void createTask_attributeConflict_rejected() throws Exception {
            membershipRepository.save(new Membership(testUser2, testHousehold, MembershipRole.member));

            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of(
                            "title",
                            "Conflicting task",
                            "assigneeId",
                            testUser.getId().toString()),
                    "assigneeId",
                    testUser2.getId().toString(),
                    "source",
                    "api");

            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"))
                    .andExpect(jsonPath("$.violations[0].rule").value("COMMAND_ATTRIBUTE_CONFLICT"));

            assertThat(taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId()))
                    .isEmpty();
            assertThat(decisionLogRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("Should reject when command-level assignee is not household member")
        void createTask_commandLevelAssigneeNotMember_rejected() throws Exception {
            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Some task"),
                    "assigneeId",
                    testUser2.getId().toString(),
                    "source",
                    "api");

            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"))
                    .andExpect(jsonPath("$.violations[0].rule").value("ASSIGNEE_MUST_BE_MEMBER"));
        }

        @Test
        @DisplayName("Should reject when command-level zone does not exist in household")
        void createTask_commandLevelZoneNotInHousehold_rejected() throws Exception {
            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Some task"),
                    "zoneId",
                    UUID.randomUUID().toString(),
                    "source",
                    "api");

            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations[0].rule").value("ZONE_MUST_EXIST"));
        }

        @Test
        @DisplayName("Should reject when command-level dueDate is in the past")
        void createTask_commandLevelPastDueDate_rejected() throws Exception {
            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Some task"),
                    "dueDate",
                    Instant.now().minus(1, ChronoUnit.DAYS).toString(),
                    "source",
                    "api");

            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations[0].rule").value("DEADLINE_MUST_BE_FUTURE"));
        }

        @Test
        @DisplayName("Should accept scheduled command without creating task immediately")
        void createTask_scheduled_success() throws Exception {
            Instant scheduleAt = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);

            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Clean later"),
                    "scheduleAt",
                    scheduleAt.toString(),
                    "source",
                    "web");

            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("scheduled"))
                    .andExpect(jsonPath("$.scheduleAt").value(scheduleAt.toString()))
                    .andExpect(jsonPath("$.commandId").isNotEmpty());

            assertThat(taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId()))
                    .isEmpty();

            var command = commandRepository.findAll().get(0);
            assertThat(command.getStatus()).isEqualTo(CommandStatus.SCHEDULED);
            assertThat(command.getScheduleAt()).isEqualTo(scheduleAt);
            assertThat(decisionLogRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("Should reject scheduled command when scheduleAt is in the past")
        void createTask_pastScheduleAt_rejected() throws Exception {
            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Clean in the past"),
                    "scheduleAt",
                    Instant.now().minus(1, ChronoUnit.HOURS).toString(),
                    "source",
                    "web");

            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"))
                    .andExpect(jsonPath("$.violations[0].rule").value("SCHEDULE_AT_MUST_BE_FUTURE"));

            assertThat(taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId()))
                    .isEmpty();
            assertThat(commandRepository.findAll().get(0).getStatus()).isEqualTo(CommandStatus.REJECTED);
        }

        @Test
        @DisplayName("Should execute due scheduled command through scheduler")
        void createTask_dueScheduledCommand_executes() throws Exception {
            Instant scheduleAt = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);

            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Clean when due"),
                    "scheduleAt",
                    scheduleAt.toString(),
                    "source",
                    "web");

            var response = mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("scheduled"))
                    .andReturn();

            UUID commandId = UUID.fromString(objectMapper
                    .readTree(response.getResponse().getContentAsString())
                    .get("commandId")
                    .asText());
            commandRepository.flush();
            int updated = jdbcTemplate.update(
                    "UPDATE commands SET schedule_at = now() - interval '1 minute' WHERE id = ?", commandId);
            assertThat(updated).isEqualTo(1);
            entityManager.clear();

            var result = commandSchedulerService.runDueCommands();

            assertThat(result.candidates()).isEqualTo(1);
            assertThat(result.processed()).isEqualTo(1);
            assertThat(result.skipped()).isZero();
            assertThat(result.errors()).isZero();

            var task = taskRepository
                    .findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId())
                    .get(0);
            assertThat(task.getTitle()).isEqualTo("Clean when due");
            assertThat(task.getCommandId()).isEqualTo(commandId);
            assertThat(commandRepository.findById(commandId).orElseThrow().getStatus())
                    .isEqualTo(CommandStatus.EXECUTED);
            assertThat(decisionLogRepository.findAll()).hasSize(2);
        }

        @Test
        @DisplayName("Should revalidate due scheduled command before execution")
        void createTask_dueScheduledCommandRevalidation_rejectsInvalidState() throws Exception {
            membershipRepository.save(new Membership(testUser2, testHousehold, MembershipRole.member));
            Instant scheduleAt = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);

            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Assign later"),
                    "assigneeId",
                    testUser2.getId().toString(),
                    "scheduleAt",
                    scheduleAt.toString(),
                    "source",
                    "web");

            var response = mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("scheduled"))
                    .andReturn();

            UUID commandId = UUID.fromString(objectMapper
                    .readTree(response.getResponse().getContentAsString())
                    .get("commandId")
                    .asText());
            commandRepository.flush();
            int deleted = jdbcTemplate.update(
                    "DELETE FROM memberships WHERE user_id = ? AND household_id = ?",
                    testUser2.getId(),
                    testHousehold.getId());
            assertThat(deleted).isEqualTo(1);
            int updated = jdbcTemplate.update(
                    "UPDATE commands SET schedule_at = now() - interval '1 minute' WHERE id = ?", commandId);
            assertThat(updated).isEqualTo(1);
            entityManager.clear();

            var result = commandSchedulerService.runDueCommands();

            assertThat(result.processed()).isEqualTo(1);
            assertThat(result.errors()).isZero();
            assertThat(taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId()))
                    .isEmpty();

            var command = commandRepository.findById(commandId).orElseThrow();
            assertThat(command.getStatus()).isEqualTo(CommandStatus.REJECTED);
            assertThat(command.getErrorCode()).isEqualTo("BUSINESS_RULE_VIOLATION");
            assertThat(decisionLogRepository.findAll()).hasSize(2);
        }

        @Test
        @DisplayName("Should reject when title is missing")
        void createTask_missingTitle_rejected() throws Exception {
            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of(), // Missing title
                    "source",
                    "api");

            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("SCHEMA_INVALID"));
        }

        @Test
        @DisplayName("Should reject when assignee is not household member")
        void createTask_assigneeNotMember_rejected() throws Exception {
            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of(
                            "title",
                            "Some task",
                            "assigneeId",
                            testUser2.getId().toString() // testUser2 is not a member
                            ),
                    "source",
                    "api");

            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"))
                    .andExpect(jsonPath("$.violations[0].rule").value("ASSIGNEE_MUST_BE_MEMBER"));
        }

        @Test
        @DisplayName("Should reject when zone does not exist in household")
        void createTask_zoneNotInHousehold_rejected() throws Exception {
            var nonExistentZoneId = UUID.randomUUID().toString();

            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Some task", "zoneId", nonExistentZoneId),
                    "source",
                    "api");

            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations[0].rule").value("ZONE_MUST_EXIST"));
        }

        @Test
        @DisplayName("Should reject when deadline is in the past")
        void createTask_pastDeadline_rejected() throws Exception {
            var pastDeadline = Instant.now().minus(1, ChronoUnit.DAYS).toString();

            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Some task", "deadline", pastDeadline),
                    "source",
                    "api");

            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations[0].rule").value("DEADLINE_MUST_BE_FUTURE"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/commands - complete_task")
    class CompleteTaskTests {

        @Test
        @DisplayName("Should complete task successfully")
        void completeTask_success() throws Exception {
            // First create a task
            String createCorrelationId = randomCorrelationId();
            var createRequest = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Task to complete"),
                    "source",
                    "api");

            var createResult = mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .header("X-Correlation-ID", createCorrelationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            var createResponse =
                    objectMapper.readTree(createResult.getResponse().getContentAsString());
            String taskId = createResponse.get("result").get("taskId").asText();

            // Now complete the task
            String completeCorrelationId = randomCorrelationId();
            var completeRequest = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "complete_task",
                    "payload",
                    Map.of("taskId", taskId),
                    "source",
                    "mobile");

            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .header("X-Correlation-ID", completeCorrelationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(completeRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("executed"))
                    .andExpect(jsonPath("$.result.taskId").value(taskId));

            // Verify task status is updated
            var task = taskRepository.findById(UUID.fromString(taskId)).orElseThrow();
            assertThat(task.getStatus().name()).isEqualTo("DONE");
            assertThat(task.getCompletedAt()).isNotNull();

            // Verify activity was recorded
            var activities = taskActivityRepository.findByCorrelationIdOrderByCreatedAtDesc(
                    UUID.fromString(completeCorrelationId));
            assertThat(activities).hasSize(1);
            assertThat(activities.get(0).getActivityType().name()).isEqualTo("TASK_COMPLETED");
        }

        @Test
        @DisplayName("Should reject when task does not exist")
        void completeTask_notFound_rejected() throws Exception {
            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "complete_task",
                    "payload",
                    Map.of("taskId", UUID.randomUUID().toString()),
                    "source",
                    "api");

            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("TASK_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should reject when user is not member of household")
        void notHouseholdMember_forbidden() throws Exception {
            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Some task"),
                    "source",
                    "api");

            // testUser2 is not a member of testHousehold
            mockMvc.perform(post("/api/v1/commands")
                            .with(jwtForUser(testUser2))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject when no authentication")
        void noAuth_unauthorized() throws Exception {
            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Some task"),
                    "source",
                    "api");

            mockMvc.perform(post("/api/v1/commands")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Correlation ID Tests")
    class CorrelationIdTests {

        @Test
        @DisplayName("Should use provided correlation ID")
        void providedCorrelationId_used() throws Exception {
            String correlationId = UUID.randomUUID().toString();

            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Test task"),
                    "source",
                    "api");

            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Correlation-ID", correlationId))
                    .andExpect(jsonPath("$.correlationId").value(correlationId));

            // Verify correlation ID in command
            var commands = commandRepository.findAll();
            assertThat(commands).hasSize(1);
            assertThat(commands.get(0).getCorrelationId().toString()).isEqualTo(correlationId);
        }

        @Test
        @DisplayName("Should write correlation ID to decision log")
        void correlationId_writtenToDecisionLog() throws Exception {
            String correlationId = UUID.randomUUID().toString();

            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Decision log test"),
                    "source",
                    "api");

            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            var log = decisionLogRepository.findByCorrelationId(UUID.fromString(correlationId));
            assertThat(log).isPresent();
        }

        @Test
        @DisplayName("Should generate correlation ID if not provided")
        void noCorrelationId_generated() throws Exception {
            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Test task"),
                    "source",
                    "api");

            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-Correlation-ID"))
                    .andExpect(jsonPath("$.correlationId").isNotEmpty());
        }
    }
}
