package com.hometusk.integration.aiplatform;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hometusk.commands.domain.CommandStatus;
import com.hometusk.commands.repository.CommandRepository;
import com.hometusk.commands.repository.DecisionLogRepository;
import com.hometusk.tasks.domain.Task;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Integration tests for AI Platform decision flow.
 *
 * <p>Tests scenarios from Stage 3 + Stage 2 Enhancement (upstream alignment):
 * <ol>
 *   <li>start_job → Task created</li>
 *   <li>clarify → NEEDS_INPUT</li>
 *   <li>Invalid payload → REJECTED</li>
 *   <li>Timeout/unavailable → Fallback</li>
 *   <li>Guardrails CLARIFY → NEEDS_INPUT</li>
 *   <li>propose_create_task → Task created (mapped to start_job)</li>
 *   <li>propose_add_shopping_item → NEEDS_INPUT (unsupported, safe degradation)</li>
 *   <li>Unknown type → REJECTED (safe degradation)</li>
 *   <li>Adapter → Guardrails flow (critical path test)</li>
 * </ol>
 */
class AiPlatformIntegrationTest extends AiPlatformIntegrationTestBase {

    @Autowired
    private CommandRepository commandRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private DecisionLogRepository decisionLogRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private UUID correlationId;

    @BeforeEach
    void setUp() {
        correlationId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Scenario 1: start_job → Task created")
    class StartJobScenario {

        @Test
        @DisplayName("should create task when AI returns start_job")
        void createsTaskOnStartJob() throws Exception {
            // Given: AI Platform returns start_job decision
            stubStartJobDecision(testUser.getId().toString(), "Clean the kitchen");

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "create_task",
                    "householdId", testHousehold.getId(),
                    "source", "text",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("rawText", "Clean the kitchen")));

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

            // Verify task was created
            var tasks = taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
            org.assertj.core.api.Assertions.assertThat(tasks).hasSize(1);
            org.assertj.core.api.Assertions.assertThat(tasks.get(0).getTitle()).isEqualTo("Clean the kitchen");

            // Verify decision log
            var log = decisionLogRepository.findByCorrelationId(correlationId);
            org.assertj.core.api.Assertions.assertThat(log).isPresent();
            org.assertj.core.api.Assertions.assertThat(log.get().getExternalDecisionId())
                    .isNotNull();
        }
    }

    @Nested
    @DisplayName("Scenario 2: clarify → NEEDS_INPUT")
    class ClarifyScenario {

        @Test
        @DisplayName("should return needs_input when AI returns clarify")
        void returnsNeedsInputOnClarify() throws Exception {
            // Given: AI Platform returns clarify decision
            stubClarifyDecision("Which room should I clean?", "zoneId");

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "create_task",
                    "householdId", testHousehold.getId(),
                    "source", "text",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("rawText", "Clean something")));

            // When
            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    // Then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("needs_input"))
                    .andExpect(jsonPath("$.question").value("Which room should I clean?"))
                    .andExpect(jsonPath("$.requiredFields", hasItem("zoneId")));

            // Verify no task was created
            var tasks = taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
            org.assertj.core.api.Assertions.assertThat(tasks).isEmpty();

            // Verify command status
            var commands = commandRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
            org.assertj.core.api.Assertions.assertThat(commands).isNotEmpty();
            org.assertj.core.api.Assertions.assertThat(commands.get(0).getStatus())
                    .isEqualTo(CommandStatus.NEEDS_INPUT);
        }
    }

    @Nested
    @DisplayName("Scenario 3: Invalid payload → REJECTED")
    class InvalidPayloadScenario {

        @Test
        @DisplayName("should reject when AI returns invalid response")
        void rejectsOnInvalidResponse() throws Exception {
            // Given: AI Platform returns invalid response (missing actions for start_job)
            stubInvalidResponse();

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
                    .andExpect(jsonPath("$.status").value("rejected"))
                    .andExpect(jsonPath("$.errorCode").value("AI_RESPONSE_INVALID"));

            // Verify no task was created
            var tasks = taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
            org.assertj.core.api.Assertions.assertThat(tasks).isEmpty();

            // Verify command was rejected
            var commands = commandRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
            org.assertj.core.api.Assertions.assertThat(commands).isNotEmpty();
            org.assertj.core.api.Assertions.assertThat(commands.get(0).getStatus())
                    .isEqualTo(CommandStatus.REJECTED);
        }
    }

    @Nested
    @DisplayName("Scenario 4: Timeout → Fallback")
    class TimeoutScenario {

        @Test
        @DisplayName("should use fallback when AI is unavailable")
        void usesFallbackOnTimeout() throws Exception {
            // Given: AI Platform health check fails (triggers fallback)
            stubHealthCheckFailed();

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
                    .andExpect(jsonPath("$.status").value("executed_degraded"))
                    .andExpect(jsonPath("$.degradedReason").value("ai_unavailable"))
                    .andExpect(jsonPath("$.result.taskId").exists());

            // Verify task was created via fallback
            var tasks = taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
            org.assertj.core.api.Assertions.assertThat(tasks).hasSize(1);

            // Verify command was executed
            var commands = commandRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
            org.assertj.core.api.Assertions.assertThat(commands).isNotEmpty();
            org.assertj.core.api.Assertions.assertThat(commands.get(0).getStatus())
                    .isEqualTo(CommandStatus.EXECUTED);
        }
    }

    @Nested
    @DisplayName("Scenario 4b: Timeout → Degraded")
    class TimeoutRequestScenario {

        @Test
        @DisplayName("should degrade when AI decision request times out")
        void usesFallbackOnDecisionTimeout() throws Exception {
            stubTimeout();

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type",
                    "create_task",
                    "householdId",
                    testHousehold.getId(),
                    "source",
                    "text",
                    "payload",
                    Map.of("title", "Clean kitchen")));

            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("executed_degraded"))
                    .andExpect(jsonPath("$.degradedReason").value("ai_unavailable"));
        }
    }

    @Nested
    @DisplayName("Scenario 4c: Circuit Open → Degraded")
    class CircuitOpenScenario {

        @Test
        @DisplayName("should degrade when circuit breaker is open")
        void usesFallbackWhenCircuitOpen() throws Exception {
            circuitBreakerRegistry.circuitBreaker("aiPlatform").transitionToOpenState();

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type",
                    "create_task",
                    "householdId",
                    testHousehold.getId(),
                    "source",
                    "text",
                    "payload",
                    Map.of("title", "Clean kitchen")));

            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("executed_degraded"))
                    .andExpect(jsonPath("$.degradedReason").value("ai_unavailable"));
        }
    }

    @Nested
    @DisplayName("Scenario 5: Guardrails CLARIFY")
    class GuardrailsClarifyScenario {

        @Test
        @DisplayName("should return needs_input when guardrails requests clarification")
        void returnsNeedsInputOnGuardrailsClarify() throws Exception {
            // Given: Create 10 tasks for testUser to hit MaxOpenTasks limit
            for (int i = 0; i < 10; i++) {
                Task task = new Task(testHousehold, "Task " + i, testUser);
                task.setAssignee(testUser);
                taskRepository.save(task);
            }

            // Ensure testUser2 has membership for assignment
            membershipRepository.save(new Membership(testUser2, testHousehold, MembershipRole.member));

            // AI Platform returns start_job with testUser as assignee (who has 10 tasks)
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
                    // Then: Guardrails should trigger NEEDS_INPUT due to MaxOpenTasks
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("needs_input"))
                    .andExpect(jsonPath("$.question", containsString("tasks")));

            // Verify no additional task was created
            var tasks = taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
            org.assertj.core.api.Assertions.assertThat(tasks).hasSize(10); // Only the pre-created tasks
        }
    }

    // --- Upstream Contract Alignment Tests (Stage 2 Enhancement) ---

    @Nested
    @DisplayName("Scenario 6: propose_create_task → Task created (mapped)")
    class ProposeCreateTaskScenario {

        @Test
        @DisplayName("should create task when AI returns propose_create_task")
        void createsTaskOnProposeCreateTask() throws Exception {
            // Given: AI Platform returns propose_create_task (upstream type)
            stubProposeCreateTaskDecision(testUser.getId().toString(), "Buy groceries");

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "create_task",
                    "householdId", testHousehold.getId(),
                    "source", "text",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("rawText", "Buy groceries")));

            // When
            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    // Then: Should be executed (propose_create_task mapped to start_job)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("executed"))
                    .andExpect(jsonPath("$.result.taskId").exists());

            // Verify task was created
            var tasks = taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
            org.assertj.core.api.Assertions.assertThat(tasks).hasSize(1);
            org.assertj.core.api.Assertions.assertThat(tasks.get(0).getTitle()).isEqualTo("Buy groceries");
        }
    }

    @Nested
    @DisplayName("Scenario 7: propose_add_shopping_item → NEEDS_INPUT (unsupported)")
    class ProposeAddShoppingItemScenario {

        @Test
        @DisplayName("should return needs_input for unsupported upstream type")
        void returnsNeedsInputForUnsupportedType() throws Exception {
            // Given: AI Platform returns propose_add_shopping_item (unsupported)
            stubProposeAddShoppingItemDecision();

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "create_task",
                    "householdId", testHousehold.getId(),
                    "source", "text",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("rawText", "Add milk to shopping list")));

            // When
            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    // Then: Safe degradation to NEEDS_INPUT
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("needs_input"))
                    .andExpect(jsonPath("$.question", containsString("не поддерживается")));

            // Verify no task was created
            var tasks = taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
            org.assertj.core.api.Assertions.assertThat(tasks).isEmpty();
        }
    }

    @Nested
    @DisplayName("Scenario 8: Unknown type → REJECTED (safe degradation)")
    class UnknownTypeScenario {

        @Test
        @DisplayName("should reject unknown decision type")
        void rejectsUnknownType() throws Exception {
            // Given: AI Platform returns unknown_future_type
            stubUnknownDecisionType();

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "create_task",
                    "householdId", testHousehold.getId(),
                    "source", "text",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("rawText", "Something")));

            // When
            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    // Then: Safe degradation to rejected
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("rejected"))
                    .andExpect(jsonPath("$.errorCode").value("UNKNOWN_DECISION_TYPE"));
        }
    }

    @Nested
    @DisplayName("Scenario 9: Adapter → Guardrails flow (critical path)")
    class AdapterGuardrailsFlowScenario {

        @Test
        @DisplayName("should pass mapped decision through guardrails correctly")
        void adapterToGuardrailsFlow() throws Exception {
            // Given: Create 10 tasks for testUser to trigger guardrails
            for (int i = 0; i < 10; i++) {
                Task task = new Task(testHousehold, "Existing Task " + i, testUser);
                task.setAssignee(testUser);
                taskRepository.save(task);
            }

            // AI Platform returns start_job with full params (assignee, zone)
            // This tests that adapter correctly preserves all fields for guardrails
            stubStartJobWithFullParams(
                    testUser.getId().toString(),
                    "Critical test task",
                    testZone.getId().toString());

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "create_task",
                    "householdId", testHousehold.getId(),
                    "source", "text",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("rawText", "Critical test task")));

            // When
            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    // Then: Guardrails should block (testUser has 10 tasks)
                    // This proves adapter correctly passed assigneeId to guardrails
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("needs_input"));

            // Verify no new task was created (guardrails blocked it)
            var tasks = taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
            org.assertj.core.api.Assertions.assertThat(tasks).hasSize(10);
        }
    }
}
