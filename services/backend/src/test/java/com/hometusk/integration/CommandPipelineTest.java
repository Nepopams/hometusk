package com.hometusk.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hometusk.activity.repository.TaskActivityRepository;
import com.hometusk.commands.repository.CommandRepository;
import com.hometusk.commands.repository.DecisionLogRepository;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

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
