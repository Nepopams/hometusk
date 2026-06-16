package com.hometusk.integration.aiplatform;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.hometusk.commands.domain.CommandConfirmationStatus;
import com.hometusk.commands.domain.CommandStatus;
import com.hometusk.commands.domain.DecisionSource;
import com.hometusk.commands.repository.CommandConfirmationRepository;
import com.hometusk.commands.repository.CommandRepository;
import com.hometusk.commands.repository.DecisionLogRepository;
import com.hometusk.shopping.repository.ShoppingItemRepository;
import com.hometusk.tasks.domain.Task;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

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
 *   <li>propose_add_shopping_item → EXECUTED (mapped to add_shopping_item)</li>
 *   <li>Unknown type → REJECTED (safe degradation)</li>
 *   <li>Adapter → Guardrails flow (critical path test)</li>
 * </ol>
 */
class AiPlatformIntegrationTest extends AiPlatformIntegrationTestBase {

    @Autowired
    private CommandRepository commandRepository;

    @Autowired
    private CommandConfirmationRepository commandConfirmationRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ShoppingItemRepository shoppingItemRepository;

    @Autowired
    private DecisionLogRepository decisionLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

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
                    "source", "web",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("title", "Clean the kitchen")));

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
                    "source", "web",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("title", "Clean something")));

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
                    "source", "web",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("title", "Clean kitchen")));

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

        @Test
        @DisplayName("should reject malformed AI response without fallback mutation")
        void rejectsMalformedResponseWithoutFallbackMutation() throws Exception {
            stubMalformedJsonResponse();
            long taskCountBefore = taskRepository.count();
            long shoppingItemCountBefore = shoppingItemRepository.count();

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "create_task",
                    "householdId", testHousehold.getId(),
                    "source", "web",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("title", "Clean kitchen")));

            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("rejected"))
                    .andExpect(jsonPath("$.errorCode").value("AI_RESPONSE_INVALID"));

            org.assertj.core.api.Assertions.assertThat(taskRepository.count()).isEqualTo(taskCountBefore);
            org.assertj.core.api.Assertions.assertThat(shoppingItemRepository.count())
                    .isEqualTo(shoppingItemCountBefore);

            var command = commandRepository
                    .findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId())
                    .get(0);
            org.assertj.core.api.Assertions.assertThat(command.getStatus()).isEqualTo(CommandStatus.REJECTED);

            var log = decisionLogRepository.findByCorrelationId(correlationId);
            org.assertj.core.api.Assertions.assertThat(log).isPresent();
            org.assertj.core.api.Assertions.assertThat(log.get().getRawDecisionPayload())
                    .contains("\"raw_payload_format\":\"invalid_json\"")
                    .contains("malformed")
                    .contains("reject");
        }
    }

    @Nested
    @DisplayName("Scenario 4: Timeout → Fallback")
    class TimeoutScenario {

        @Test
        @DisplayName("should use fallback when AI is unavailable")
        void usesFallbackOnTimeout() throws Exception {
            // Given: AI Platform decision endpoint is unavailable
            stubDecisionEndpointUnavailable();

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "create_task",
                    "householdId", testHousehold.getId(),
                    "source", "web",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("title", "Clean kitchen")));

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
                    "web",
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
                    "web",
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
    @DisplayName("Scenario 4d: Health endpoint unavailable")
    class HealthEndpointUnavailableScenario {

        @Test
        @DisplayName("should still use AI decision endpoint when health check is unavailable")
        void usesDecisionEndpointWhenHealthEndpointUnavailable() throws Exception {
            stubHealthCheckFailed();
            stubProposeAddShoppingItemDecision();

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type",
                    "create_task",
                    "householdId",
                    testHousehold.getId(),
                    "source",
                    "voice",
                    "payload",
                    Map.of("title", "Add milk to shopping list")));

            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("executed"))
                    .andExpect(jsonPath("$.result.taskId").exists());

            var tasks = taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
            org.assertj.core.api.Assertions.assertThat(tasks).isEmpty();

            var log = decisionLogRepository.findByCorrelationId(correlationId);
            org.assertj.core.api.Assertions.assertThat(log).isPresent();
            org.assertj.core.api.Assertions.assertThat(log.get().getSource()).isEqualTo(DecisionSource.AI_PLATFORM);
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
                    "source", "web",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("title", "Another task")));

            // When
            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    // Then: Guardrails should trigger NEEDS_INPUT due to MaxOpenTasks
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("needs_input"))
                    .andExpect(jsonPath("$.question", containsString("10")));

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
                    "source", "web",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("title", "Buy groceries")));

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
    @DisplayName("Scenario 7: propose_add_shopping_item → EXECUTED (now supported)")
    class ProposeAddShoppingItemScenario {

        @Test
        @DisplayName("should execute shopping item action")
        void executesShoppingItemAction() throws Exception {
            // Given: AI Platform returns propose_add_shopping_item (now supported)
            stubProposeAddShoppingItemDecision();

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "create_task",
                    "householdId", testHousehold.getId(),
                    "source", "web",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("title", "Add milk to shopping list")));

            // When
            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    // Then: Shopping item should be added (id returned in taskId field)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("executed"))
                    .andExpect(jsonPath("$.result.taskId").exists());

            // Verify no task was created (shopping item was created instead)
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
                    "source", "web",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("title", "Something")));

            // When
            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    // Then: Schema validation rejects unknown type
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("rejected"))
                    .andExpect(jsonPath("$.errorCode").value("AI_RESPONSE_INVALID"));
        }
    }

    @Nested
    @DisplayName("Scenario 10: first-class reject → REJECTED without mutation")
    class RejectScenario {

        @Test
        @DisplayName("should reject safely and preserve raw provider payload")
        void rejectsWithoutMutationAndStoresRawPayload() throws Exception {
            stubRejectDecision();
            long taskCountBefore = taskRepository.count();
            long shoppingItemCountBefore = shoppingItemRepository.count();

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "create_task",
                    "householdId", testHousehold.getId(),
                    "source", "web",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("title", "Transfer money")));

            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("rejected"))
                    .andExpect(jsonPath("$.errorCode").value("unsupported_or_unsafe_command"))
                    .andExpect(jsonPath("$.reason").value("I cannot safely handle this request."));

            org.assertj.core.api.Assertions.assertThat(taskRepository.count()).isEqualTo(taskCountBefore);
            org.assertj.core.api.Assertions.assertThat(shoppingItemRepository.count())
                    .isEqualTo(shoppingItemCountBefore);

            var command = commandRepository
                    .findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId())
                    .get(0);
            org.assertj.core.api.Assertions.assertThat(command.getStatus()).isEqualTo(CommandStatus.REJECTED);

            var log = decisionLogRepository.findByCorrelationId(correlationId);
            org.assertj.core.api.Assertions.assertThat(log).isPresent();
            org.assertj.core.api.Assertions.assertThat(log.get().getRawDecisionPayload())
                    .contains("\"action\": \"reject\"")
                    .contains("\"trace_id\": \"trace-test-reject\"")
                    .contains("\"schema_version\": \"2.1.0\"")
                    .contains("\"decision_version\": \"mvp1-graph-0.1\"")
                    .contains("\"code\": \"unsupported_or_unsafe_command\"");
        }
    }

    @Nested
    @DisplayName("Scenario 11: schema confirm → controlled non-execution")
    class ConfirmScenario {

        @Test
        @DisplayName("should map confirm to needs_confirmation and preserve raw provider payload without mutation")
        void mapsConfirmToNeedsConfirmationWithoutMutation() throws Exception {
            stubConfirmDecision();
            long taskCountBefore = taskRepository.count();
            long shoppingItemCountBefore = shoppingItemRepository.count();

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "natural_command",
                    "householdId", testHousehold.getId(),
                    "source", "mobile",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload",
                            Map.of(
                                    "text",
                                    "Assign kitchen cleanup to someone else",
                                    "inputMode",
                                    "text",
                                    "locale",
                                    "en-US",
                                    "timezone",
                                    "UTC",
                                    "referenceInstant",
                                    "2026-06-16T09:00:00Z")));

            mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("needs_confirmation"))
                    .andExpect(jsonPath("$.confirmation.confirmationId").isNotEmpty())
                    .andExpect(jsonPath("$.confirmation.providerConfirmationId").value("conf-test"))
                    .andExpect(jsonPath("$.confirmation.summary").value("Create a task for another household member."))
                    .andExpect(
                            jsonPath("$.confirmation.proposedActions[0].type").value("create_task"))
                    .andExpect(jsonPath("$.trace.providerTraceId").value("trace-test-confirm"))
                    .andExpect(jsonPath("$.trace.schemaVersion").value("2.1.0"));

            org.assertj.core.api.Assertions.assertThat(taskRepository.count()).isEqualTo(taskCountBefore);
            org.assertj.core.api.Assertions.assertThat(shoppingItemRepository.count())
                    .isEqualTo(shoppingItemCountBefore);

            var command = commandRepository
                    .findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId())
                    .get(0);
            org.assertj.core.api.Assertions.assertThat(command.getStatus()).isEqualTo(CommandStatus.NEEDS_CONFIRMATION);
            org.assertj.core.api.Assertions.assertThat(
                            commandConfirmationRepository.findByCommand_IdOrderByCreatedAtDesc(command.getId()))
                    .hasSize(1);

            var log = decisionLogRepository.findByCorrelationId(correlationId);
            org.assertj.core.api.Assertions.assertThat(log).isPresent();
            org.assertj.core.api.Assertions.assertThat(log.get().getRawDecisionPayload())
                    .contains("\"action\": \"confirm\"")
                    .contains("\"decision_outcome\": \"confirm\"")
                    .contains("\"trace_id\": \"trace-test-confirm\"")
                    .contains("\"schema_version\": \"2.1.0\"")
                    .contains("\"confirmation_id\": \"conf-test\"");
            org.assertj.core.api.Assertions.assertThat(log.get().getDecision()).contains("needs_confirmation");
        }

        @Test
        @DisplayName("should approve pending confirmation exactly once")
        void approvesPendingConfirmationOnce() throws Exception {
            PendingConfirmation pending = createPendingConfirmation();
            long taskCountBefore = taskRepository.count();
            UUID approvalCorrelationId = UUID.randomUUID();

            mockMvc.perform(post(
                                    "/api/v1/commands/{commandId}/confirmations/{confirmationId}/approve",
                                    pending.commandId(),
                                    pending.confirmationId())
                            .header("X-Correlation-ID", approvalCorrelationId)
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("executed"))
                    .andExpect(jsonPath("$.result.taskId").isNotEmpty())
                    .andExpect(jsonPath("$.approvedBy").value(testUser.getId().toString()))
                    .andExpect(jsonPath("$.idempotentReplay").value(false));

            org.assertj.core.api.Assertions.assertThat(taskRepository.count()).isEqualTo(taskCountBefore + 1);

            var confirmation = commandConfirmationRepository
                    .findById(pending.confirmationId())
                    .orElseThrow();
            org.assertj.core.api.Assertions.assertThat(confirmation.getStatus())
                    .isEqualTo(CommandConfirmationStatus.EXECUTED);
            org.assertj.core.api.Assertions.assertThat(confirmation.getApprovedBy())
                    .isEqualTo(testUser.getId());
            org.assertj.core.api.Assertions.assertThat(confirmation.getExecutionResult())
                    .contains("taskId");

            var command = commandRepository.findById(pending.commandId()).orElseThrow();
            org.assertj.core.api.Assertions.assertThat(command.getStatus()).isEqualTo(CommandStatus.EXECUTED);

            var approvalLog = decisionLogRepository.findByCorrelationId(approvalCorrelationId);
            org.assertj.core.api.Assertions.assertThat(approvalLog).isPresent();
            org.assertj.core.api.Assertions.assertThat(approvalLog.get().getDecision())
                    .contains("confirmation_approved")
                    .contains("executed");

            mockMvc.perform(post(
                                    "/api/v1/commands/{commandId}/confirmations/{confirmationId}/approve",
                                    pending.commandId(),
                                    pending.confirmationId())
                            .header("X-Correlation-ID", UUID.randomUUID())
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("executed"))
                    .andExpect(jsonPath("$.idempotentReplay").value(true));

            org.assertj.core.api.Assertions.assertThat(taskRepository.count()).isEqualTo(taskCountBefore + 1);
        }

        @Test
        @DisplayName("should cancel pending confirmation without mutation and replay safely")
        void cancelsPendingConfirmationWithoutMutation() throws Exception {
            PendingConfirmation pending = createPendingConfirmation();
            long taskCountBefore = taskRepository.count();
            UUID cancelCorrelationId = UUID.randomUUID();

            mockMvc.perform(post(
                                    "/api/v1/commands/{commandId}/confirmations/{confirmationId}/cancel",
                                    pending.commandId(),
                                    pending.confirmationId())
                            .header("X-Correlation-ID", cancelCorrelationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"Not needed anymore\"}")
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("cancelled"))
                    .andExpect(jsonPath("$.cancelledBy").value(testUser.getId().toString()))
                    .andExpect(jsonPath("$.idempotentReplay").value(false));

            org.assertj.core.api.Assertions.assertThat(taskRepository.count()).isEqualTo(taskCountBefore);

            var confirmation = commandConfirmationRepository
                    .findById(pending.confirmationId())
                    .orElseThrow();
            org.assertj.core.api.Assertions.assertThat(confirmation.getStatus())
                    .isEqualTo(CommandConfirmationStatus.CANCELLED);
            org.assertj.core.api.Assertions.assertThat(confirmation.getCancelledBy())
                    .isEqualTo(testUser.getId());
            org.assertj.core.api.Assertions.assertThat(confirmation.getCancelReason())
                    .isEqualTo("Not needed anymore");

            var cancelLog = decisionLogRepository.findByCorrelationId(cancelCorrelationId);
            org.assertj.core.api.Assertions.assertThat(cancelLog).isPresent();
            org.assertj.core.api.Assertions.assertThat(cancelLog.get().getDecision())
                    .contains("confirmation_cancelled")
                    .contains("cancelled");

            mockMvc.perform(post(
                                    "/api/v1/commands/{commandId}/confirmations/{confirmationId}/cancel",
                                    pending.commandId(),
                                    pending.confirmationId())
                            .header("X-Correlation-ID", UUID.randomUUID())
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("cancelled"))
                    .andExpect(jsonPath("$.idempotentReplay").value(true));

            org.assertj.core.api.Assertions.assertThat(taskRepository.count()).isEqualTo(taskCountBefore);
        }

        @Test
        @DisplayName("should deny approval by non-initiator household member")
        void deniesNonInitiatorApproval() throws Exception {
            membershipRepository.save(new Membership(testUser2, testHousehold, MembershipRole.member));
            PendingConfirmation pending = createPendingConfirmation();
            long taskCountBefore = taskRepository.count();

            mockMvc.perform(post(
                                    "/api/v1/commands/{commandId}/confirmations/{confirmationId}/approve",
                                    pending.commandId(),
                                    pending.confirmationId())
                            .header("X-Correlation-ID", UUID.randomUUID())
                            .with(jwtForUser(testUser2)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));

            org.assertj.core.api.Assertions.assertThat(taskRepository.count()).isEqualTo(taskCountBefore);
            var confirmation = commandConfirmationRepository
                    .findById(pending.confirmationId())
                    .orElseThrow();
            org.assertj.core.api.Assertions.assertThat(confirmation.getStatus())
                    .isEqualTo(CommandConfirmationStatus.PENDING_CONFIRMATION);
        }

        @Test
        @DisplayName("should expire stale confirmation on approval without mutation")
        void expiresStaleConfirmationOnApprovalWithoutMutation() throws Exception {
            PendingConfirmation pending = createPendingConfirmation();
            long taskCountBefore = taskRepository.count();
            UUID approvalCorrelationId = UUID.randomUUID();

            entityManager.flush();
            entityManager.clear();
            jdbcTemplate.update(
                    "UPDATE command_confirmations SET expires_at = NOW() - INTERVAL '1 minute' WHERE id = ?",
                    pending.confirmationId());
            entityManager.clear();

            mockMvc.perform(post(
                                    "/api/v1/commands/{commandId}/confirmations/{confirmationId}/approve",
                                    pending.commandId(),
                                    pending.confirmationId())
                            .header("X-Correlation-ID", approvalCorrelationId)
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("rejected"))
                    .andExpect(jsonPath("$.errorCode").value("CONFIRMATION_EXPIRED"));

            org.assertj.core.api.Assertions.assertThat(taskRepository.count()).isEqualTo(taskCountBefore);
            var confirmation = commandConfirmationRepository
                    .findById(pending.confirmationId())
                    .orElseThrow();
            org.assertj.core.api.Assertions.assertThat(confirmation.getStatus())
                    .isEqualTo(CommandConfirmationStatus.EXPIRED);

            var expiryLog = decisionLogRepository.findByCorrelationId(approvalCorrelationId);
            org.assertj.core.api.Assertions.assertThat(expiryLog).isPresent();
            org.assertj.core.api.Assertions.assertThat(expiryLog.get().getDecision())
                    .contains("confirmation_expired")
                    .contains("expired");
        }

        private PendingConfirmation createPendingConfirmation() throws Exception {
            stubConfirmDecision();

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "type", "natural_command",
                    "householdId", testHousehold.getId(),
                    "source", "mobile",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload",
                            Map.of(
                                    "text",
                                    "Assign kitchen cleanup to someone else",
                                    "inputMode",
                                    "text",
                                    "locale",
                                    "en-US",
                                    "timezone",
                                    "UTC",
                                    "referenceInstant",
                                    "2026-06-16T09:00:00Z")));

            MvcResult result = mockMvc.perform(post("/api/v1/commands")
                            .header("X-Correlation-ID", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("needs_confirmation"))
                    .andReturn();

            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            return new PendingConfirmation(
                    UUID.fromString(body.get("commandId").asText()),
                    UUID.fromString(body.at("/confirmation/confirmationId").asText()));
        }

        private record PendingConfirmation(UUID commandId, UUID confirmationId) {}
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
                    "source", "web",
                    "clientTimestamp", "2024-01-15T10:00:00Z",
                    "payload", Map.of("title", "Critical test task")));

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
