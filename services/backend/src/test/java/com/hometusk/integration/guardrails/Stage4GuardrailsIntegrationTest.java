package com.hometusk.integration.guardrails;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.hometusk.commands.repository.CommandRepository;
import com.hometusk.commands.repository.DecisionLogRepository;
import com.hometusk.households.domain.Zone;
import com.hometusk.integration.aiplatform.AiPlatformIntegrationTestBase;
import com.hometusk.tasks.domain.Task;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Integration tests for Stage 4: Context-driven Autodelegation.
 *
 * <p>Tests all guardrail policies and context enrichment:
 * <ol>
 *   <li>Decision with assignee and deadline → Task created</li>
 *   <li>Assignee not member → REJECT (MembershipPolicy)</li>
 *   <li>Deadline in past → CLARIFY (DeadlineSanityPolicy)</li>
 *   <li>Deadline too far future → CLARIFY (DeadlineSanityPolicy)</li>
 *   <li>Deadline in quiet hours → ACCEPT (AvailabilityPolicy disabled)</li>
 *   <li>Max open tasks exceeded → CLARIFY (MaxOpenTasksPerAssigneePolicy)</li>
 *   <li>Zone with owner, no assignee → Owner assigned (ZoneOwnerFirstPolicy)</li>
 *   <li>Workload score calculation → Included in AI request context</li>
 *   <li>Full chain: upstream → guardrails → execution</li>
 * </ol>
 */
class Stage4GuardrailsIntegrationTest extends AiPlatformIntegrationTestBase {

    @Autowired
    private CommandRepository commandRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private DecisionLogRepository decisionLogRepository;

    private UUID correlationId;

    @BeforeEach
    void setUp() {
        correlationId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Scenario 1: Decision with assignee and deadline")
    class AssigneeAndDeadlineScenario {

        @Test
        @DisplayName("should create task with both assignee and deadline fields")
        void createsTaskWithBothFields() throws Exception {
            // Given: AI Platform returns start_job with assignee and deadline
            String deadline = "2026-01-20T18:00:00Z";
            stubStartJobWithAssigneeAndDeadline(testUser.getId().toString(), "Clean kitchen", deadline);

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "create_task",
                    "householdId", testHousehold.getId(),
                    "source", "text",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("rawText", "Clean kitchen")));

            // When
            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    // Then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("executed"))
                    .andExpect(jsonPath("$.result.taskId").exists())
                    .andExpect(jsonPath("$.result.assigneeId")
                            .value(testUser.getId().toString()));

            // Verify task has both fields
            var tasks = taskRepository.findByHouseholdIdOrderByCreatedAtDesc(testHousehold.getId());
            org.assertj.core.api.Assertions.assertThat(tasks).hasSize(1);
            org.assertj.core.api.Assertions.assertThat(tasks.get(0).getAssigneeId())
                    .isEqualTo(testUser.getId());
            org.assertj.core.api.Assertions.assertThat(tasks.get(0).getDeadline())
                    .isNotNull();
            org.assertj.core.api.Assertions.assertThat(
                            tasks.get(0).getDeadline().toString())
                    .isEqualTo(deadline);
        }
    }

    @Nested
    @DisplayName("Scenario 2: Assignee not member → REJECT")
    class AssigneeNotMemberScenario {

        @Test
        @DisplayName("should reject when assignee is not a household member")
        void rejectsNonMemberAssignee() throws Exception {
            // Given: AI Platform returns start_job with non-existent assignee
            UUID invalidAssigneeId = UUID.randomUUID();
            stubStartJobDecision(invalidAssigneeId.toString(), "Clean kitchen");

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "create_task",
                    "householdId", testHousehold.getId(),
                    "source", "text",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("rawText", "Clean kitchen")));

            // When
            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    // Then: MembershipPolicy should reject
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("rejected"))
                    .andExpect(jsonPath("$.errorCode").value("ASSIGNEE_NOT_MEMBER"));

            // Verify no task was created
            var tasks = taskRepository.findByHouseholdIdOrderByCreatedAtDesc(testHousehold.getId());
            org.assertj.core.api.Assertions.assertThat(tasks).isEmpty();
        }
    }

    @Nested
    @DisplayName("Scenario 3: Deadline in past → CLARIFY")
    class DeadlineInPastScenario {

        @Test
        @DisplayName("should clarify when deadline is in the past")
        void clarifiesDeadlineInPast() throws Exception {
            // Given: AI Platform returns start_job with past deadline (yesterday)
            String pastDeadline = Instant.now().minus(1, ChronoUnit.DAYS).toString();
            stubStartJobWithAssigneeAndDeadline(testUser.getId().toString(), "Clean kitchen", pastDeadline);

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "create_task",
                    "householdId", testHousehold.getId(),
                    "source", "text",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("rawText", "Clean kitchen")));

            // When
            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    // Then: DeadlineSanityPolicy should clarify
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("needs_input"))
                    .andExpect(jsonPath("$.question", containsString("прошёл")))
                    .andExpect(jsonPath("$.requiredFields", hasItem("deadline")));

            // Verify no task was created
            var tasks = taskRepository.findByHouseholdIdOrderByCreatedAtDesc(testHousehold.getId());
            org.assertj.core.api.Assertions.assertThat(tasks).isEmpty();
        }
    }

    @Nested
    @DisplayName("Scenario 4: Deadline too far future → CLARIFY")
    class DeadlineTooFarFutureScenario {

        @Test
        @DisplayName("should clarify when deadline is more than 365 days in future")
        void clarifiesDeadlineTooFar() throws Exception {
            // Given: AI Platform returns start_job with deadline > 365 days
            String farDeadline = Instant.now().plus(400, ChronoUnit.DAYS).toString();
            stubStartJobWithAssigneeAndDeadline(testUser.getId().toString(), "Future task", farDeadline);

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "create_task",
                    "householdId", testHousehold.getId(),
                    "source", "text",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("rawText", "Future task")));

            // When
            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    // Then: DeadlineSanityPolicy should clarify
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("needs_input"))
                    .andExpect(jsonPath("$.requiredFields", hasItem("deadline")));

            // Verify no task was created
            var tasks = taskRepository.findByHouseholdIdOrderByCreatedAtDesc(testHousehold.getId());
            org.assertj.core.api.Assertions.assertThat(tasks).isEmpty();
        }
    }

    @Nested
    @DisplayName("Scenario 5: Deadline in quiet hours (AvailabilityPolicy disabled)")
    class QuietHoursScenario {

        @Test
        @DisplayName("should accept deadline in quiet hours when AvailabilityPolicy is disabled")
        void acceptsQuietHoursWhenDisabled() throws Exception {
            // Given: AI Platform returns start_job with deadline at 23:00 (quiet hours)
            // AvailabilityPolicy is disabled by default in Stage 4
            Instant quietHoursDeadline = Instant.parse("2026-01-20T23:00:00Z");
            stubStartJobWithAssigneeAndDeadline(
                    testUser.getId().toString(), "Late evening task", quietHoursDeadline.toString());

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "create_task",
                    "householdId", testHousehold.getId(),
                    "source", "text",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("rawText", "Late evening task")));

            // When
            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    // Then: Should be accepted (AvailabilityPolicy disabled)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("executed"))
                    .andExpect(jsonPath("$.result.taskId").exists());

            // Verify task was created
            var tasks = taskRepository.findByHouseholdIdOrderByCreatedAtDesc(testHousehold.getId());
            org.assertj.core.api.Assertions.assertThat(tasks).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Scenario 6: Max open tasks exceeded → CLARIFY")
    class MaxOpenTasksScenario {

        @Test
        @DisplayName("should clarify when assignee has max open tasks")
        void clarifiesMaxOpenTasks() throws Exception {
            // Given: testUser has 10 open tasks (at limit)
            for (int i = 0; i < 10; i++) {
                Task task = new Task(testHousehold, "Existing task " + i, testUser);
                task.setAssignee(testUser);
                taskRepository.save(task);
            }

            // AI Platform returns start_job assigning to testUser (who has 10 tasks)
            stubStartJobDecision(testUser.getId().toString(), "Another task");

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "create_task",
                    "householdId", testHousehold.getId(),
                    "source", "text",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("rawText", "Another task")));

            // When
            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    // Then: MaxOpenTasksPerAssigneePolicy should clarify
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("needs_input"))
                    .andExpect(jsonPath("$.question")
                            .value(anyOf(
                                    containsString("открытых задач"),
                                    containsString("максимум"),
                                    containsString("tasks"))))
                    .andExpect(jsonPath("$.requiredFields", hasItem("assigneeId")));

            // Verify no additional task was created
            var tasks = taskRepository.findByHouseholdIdOrderByCreatedAtDesc(testHousehold.getId());
            org.assertj.core.api.Assertions.assertThat(tasks).hasSize(10); // Only pre-created tasks
        }
    }

    @Nested
    @DisplayName("Scenario 7: Zone with owner, no assignee → Owner assigned")
    class ZoneOwnerFirstScenario {

        private Zone zoneWithOwner;

        @BeforeEach
        void setUpZoneWithOwner() {
            // Create zone with testUser as owner
            zoneWithOwner = new Zone(testHousehold, "Master Bedroom");
            zoneWithOwner.setOwner(testUser);
            zoneWithOwner = zoneRepository.save(zoneWithOwner);
        }

        @Test
        @DisplayName("should assign zone owner when no assignee specified")
        void assignsZoneOwner() throws Exception {
            // Given: AI Platform returns start_job with zone but NO assignee
            stubStartJobWithZoneNoAssignee(
                    "Clean bedroom", zoneWithOwner.getId().toString());

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "create_task",
                    "householdId", testHousehold.getId(),
                    "source", "text",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("rawText", "Clean bedroom")));

            // When
            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    // Then: ZoneOwnerFirstPolicy should assign owner
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("executed"))
                    .andExpect(jsonPath("$.result.taskId").exists())
                    .andExpect(jsonPath("$.result.assigneeId")
                            .value(testUser.getId().toString()));

            // Verify task assigned to zone owner
            var tasks = taskRepository.findByHouseholdIdOrderByCreatedAtDesc(testHousehold.getId());
            org.assertj.core.api.Assertions.assertThat(tasks).hasSize(1);
            org.assertj.core.api.Assertions.assertThat(tasks.get(0).getAssigneeId())
                    .isEqualTo(testUser.getId());
            org.assertj.core.api.Assertions.assertThat(tasks.get(0).getZoneId()).isEqualTo(zoneWithOwner.getId());
        }
    }

    @Nested
    @DisplayName("Scenario 8: Workload score calculation")
    class WorkloadScoreScenario {

        @Test
        @DisplayName("should include workload_score in AI Platform request context")
        void includesWorkloadScoreInContext() throws Exception {
            // Given: testUser has 3 open tasks (maxTasks=10, so score=0.3)
            for (int i = 0; i < 3; i++) {
                Task task = new Task(testHousehold, "Task " + i, testUser);
                task.setAssignee(testUser);
                taskRepository.save(task);
            }

            // Stub AI Platform to return start_job
            stubStartJobDecision(testUser.getId().toString(), "New task");

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "create_task",
                    "householdId", testHousehold.getId(),
                    "source", "text",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("rawText", "New task")));

            // When
            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("executed"));

            // Then: Verify request to AI Platform included workload_score
            // WireMock should have received request with context.members[].workload_score
            wireMockServer.verify(postRequestedFor(urlEqualTo("/decision"))
                    .withRequestBody(matchingJsonPath("$.context.members[?(@.workload_score)]")));
        }
    }

    @Nested
    @DisplayName("Scenario 9: Full chain (CRITICAL)")
    class FullChainScenario {

        private Zone bedroomZone;

        @BeforeEach
        void setUpCompleteHousehold() {
            // Create second member
            membershipRepository.save(new Membership(testUser2, testHousehold, MembershipRole.member));

            // Create zones (Kitchen already exists from base, add Bedroom)
            bedroomZone = new Zone(testHousehold, "Bedroom");
            bedroomZone = zoneRepository.save(bedroomZone);

            // Create existing tasks (2 for testUser, 1 for testUser2)
            Task task1 = new Task(testHousehold, "Existing task 1", testUser);
            task1.setAssignee(testUser);
            taskRepository.save(task1);

            Task task2 = new Task(testHousehold, "Existing task 2", testUser);
            task2.setAssignee(testUser);
            taskRepository.save(task2);

            Task task3 = new Task(testHousehold, "Existing task 3", testUser2);
            task3.setAssignee(testUser2);
            taskRepository.save(task3);
        }

        @Test
        @DisplayName("should execute full chain from upstream to action execution")
        void executesFullChain() throws Exception {
            // Given: AI Platform returns start_job with all fields
            String deadline = "2026-01-25T14:00:00Z";
            stubStartJobWithFullParams(
                    testUser2.getId().toString(),
                    "Deep clean kitchen",
                    testZone.getId().toString(),
                    deadline);

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "create_task",
                    "householdId", testHousehold.getId(),
                    "source", "text",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("rawText", "Deep clean kitchen")));

            // When
            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    // Then: Full chain should succeed
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("executed"))
                    .andExpect(jsonPath("$.result.taskId").exists())
                    .andExpect(jsonPath("$.result.assigneeId")
                            .value(testUser2.getId().toString()));

            // Verify task persisted with all fields
            var tasks = taskRepository.findByHouseholdIdOrderByCreatedAtDesc(testHousehold.getId());
            org.assertj.core.api.Assertions.assertThat(tasks).hasSize(4); // 3 existing + 1 new

            Task newTask = tasks.get(0); // Most recent
            org.assertj.core.api.Assertions.assertThat(newTask.getTitle()).isEqualTo("Deep clean kitchen");
            org.assertj.core.api.Assertions.assertThat(newTask.getAssigneeId()).isEqualTo(testUser2.getId());
            org.assertj.core.api.Assertions.assertThat(newTask.getZoneId()).isEqualTo(testZone.getId());
            org.assertj.core.api.Assertions.assertThat(newTask.getDeadline()).isNotNull();
            org.assertj.core.api.Assertions.assertThat(newTask.getDeadline().toString())
                    .isEqualTo(deadline);

            // Verify DecisionLog exists with external_decision_id
            var log = decisionLogRepository.findByCorrelationId(correlationId);
            org.assertj.core.api.Assertions.assertThat(log).isPresent();
            org.assertj.core.api.Assertions.assertThat(log.get().getExternalDecisionId())
                    .isNotNull();
        }
    }

    // ==================== Helper Stub Methods ====================

    /**
     * Stubs AI Platform to return start_job with assignee and deadline.
     */
    protected void stubStartJobWithAssigneeAndDeadline(String assigneeId, String title, String deadline) {
        String responseBody =
                """
                {
                    "decisionId": "%s",
                    "type": "start_job",
                    "confidence": 0.95,
                    "actions": [
                        {
                            "actionType": "create_task",
                            "parameters": {
                                "title": "%s",
                                "assigneeId": "%s",
                                "deadline": "%s"
                            }
                        }
                    ]
                }
                """
                        .formatted(UUID.randomUUID(), title, assigneeId, deadline);

        stubFor(WireMock.post(urlEqualTo("/decision"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    /**
     * Stubs AI Platform to return start_job with zone but NO assignee.
     */
    protected void stubStartJobWithZoneNoAssignee(String title, String zoneId) {
        String responseBody =
                """
                {
                    "decisionId": "%s",
                    "type": "start_job",
                    "confidence": 0.90,
                    "actions": [
                        {
                            "actionType": "create_task",
                            "parameters": {
                                "title": "%s",
                                "zoneId": "%s"
                            }
                        }
                    ]
                }
                """
                        .formatted(UUID.randomUUID(), title, zoneId);

        stubFor(WireMock.post(urlEqualTo("/decision"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    /**
     * Stubs AI Platform to return start_job with all parameters (override base method).
     */
    protected void stubStartJobWithFullParams(String assigneeId, String title, String zoneId, String deadline) {
        String responseBody =
                """
                {
                    "decisionId": "%s",
                    "type": "start_job",
                    "confidence": 0.95,
                    "actions": [
                        {
                            "actionType": "create_task",
                            "parameters": {
                                "title": "%s",
                                "assigneeId": "%s",
                                "zoneId": "%s",
                                "deadline": "%s"
                            }
                        }
                    ]
                }
                """
                        .formatted(UUID.randomUUID(), title, assigneeId, zoneId, deadline);

        stubFor(WireMock.post(urlEqualTo("/decision"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }
}
