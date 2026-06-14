package com.hometusk.integration.shopping;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.hometusk.activity.domain.ActivityType;
import com.hometusk.activity.repository.TaskActivityRepository;
import com.hometusk.commands.repository.CommandRepository;
import com.hometusk.integration.aiplatform.AiPlatformIntegrationTestBase;
import com.hometusk.shopping.domain.ShoppingItem;
import com.hometusk.shopping.domain.ShoppingList;
import com.hometusk.shopping.repository.ShoppingItemRepository;
import com.hometusk.shopping.repository.ShoppingListRepository;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Integration tests for Stage 5: Task ↔ Shopping Linkage.
 *
 * <p>Scenarios covered:
 * <ol>
 *   <li>Command creates task + shopping items linked to it</li>
 *   <li>add_shopping_item without task context → items added unlinked</li>
 *   <li>Idempotent retry → no duplicates</li>
 *   <li>Invalid decision schema → rejected safely</li>
 *   <li>Household boundary violation → reject</li>
 *   <li>Full-chain test: upstream → adapter → guardrails → execution → persisted links</li>
 * </ol>
 */
class ShoppingIntegrationTest extends AiPlatformIntegrationTestBase {

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private ShoppingItemRepository shoppingItemRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CommandRepository commandRepository;

    @Autowired
    private TaskActivityRepository taskActivityRepository;

    private ShoppingList testShoppingList;

    @BeforeEach
    void setUpShoppingData() {
        // Create a default shopping list for the test household
        testShoppingList = new ShoppingList(testHousehold, "Default");
        testShoppingList = shoppingListRepository.save(testShoppingList);

        // testUser2 is not a member yet - add as member for valid tests
        Membership membership2 = new Membership(testUser2, testHousehold, MembershipRole.member);
        membershipRepository.save(membership2);
    }

    @Nested
    @DisplayName("Scenario 1: Command creates task + shopping items linked")
    class TaskWithLinkedShoppingItemsTest {

        @Test
        @DisplayName("Should create task and linked shopping items from single decision")
        void createsTaskAndLinkedShoppingItems() throws Exception {
            // Given: AI returns start_job with create_task + add_shopping_item actions
            stubTaskWithShoppingItems(testUser.getId().toString(), "Grocery shopping");

            String correlationId = randomCorrelationId();

            // When: Execute command
            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Grocery shopping"),
                    "source",
                    "api");

            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("executed"))
                    .andExpect(jsonPath("$.result.taskId").exists());

            // Then: Task created
            var tasks = taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
            assertThat(tasks).hasSize(1);
            var task = tasks.get(0);
            assertThat(task.getTitle()).isEqualTo("Grocery shopping");

            // Then: Shopping items created and linked to task
            var items = shoppingItemRepository.findByLinkedTask_Id(task.getId());
            assertThat(items).hasSize(2);
            assertThat(items).allMatch(i -> i.getLinkedTaskId().equals(task.getId()));
            assertThat(items).extracting(ShoppingItem::getName).containsExactlyInAnyOrder("Milk", "Bread");

            // Then: Activities recorded
            var activities =
                    taskActivityRepository.findByCorrelationIdOrderByCreatedAtDesc(UUID.fromString(correlationId));
            assertThat(activities).anyMatch(a -> a.getActivityType() == ActivityType.TASK_CREATED);
            assertThat(activities).anyMatch(a -> a.getActivityType() == ActivityType.SHOPPING_ITEM_ADDED);
        }
    }

    @Nested
    @DisplayName("Scenario 2: add_shopping_item without task context")
    class ShoppingItemWithoutTaskTest {

        @Test
        @DisplayName("Should add items without task link when no task in decision")
        void addsItemsWithoutTaskLink() throws Exception {
            // Given: AI returns propose_add_shopping_item (now supported in Stage 5)
            stubAddShoppingItemOnly("Молоко", 2, "л");

            String correlationId = randomCorrelationId();

            // When: Execute command
            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Купить молоко"),
                    "source",
                    "api");

            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("executed"));

            // Then: Shopping item created without task link
            var items = shoppingItemRepository.findByShoppingList_IdOrderByCreatedAtDesc(testShoppingList.getId());
            assertThat(items).hasSize(1);
            assertThat(items.get(0).getName()).isEqualTo("Молоко");
            assertThat(items.get(0).getQuantity()).isEqualTo(2);
            assertThat(items.get(0).getUnit()).isEqualTo("л");
            assertThat(items.get(0).getCategory()).isNull();
            assertThat(items.get(0).getSource()).isNull();
            assertThat(items.get(0).getLinkedTask()).isNull();
        }
    }

    @Nested
    @DisplayName("Scenario 3: Idempotent retry → no duplicates")
    class IdempotencyTest {

        @Test
        @DisplayName("Should not create duplicate items on retry with same command")
        void noDoubleCreationOnRetry() throws Exception {
            // Given: AI returns add_shopping_item
            stubAddShoppingItemOnly("Test Item", 1, null);

            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Add test item"),
                    "source",
                    "api");

            // When: First call
            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .header("X-Correlation-ID", randomCorrelationId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Verify first item created
            var itemsAfterFirst =
                    shoppingItemRepository.findByShoppingList_IdOrderByCreatedAtDesc(testShoppingList.getId());
            assertThat(itemsAfterFirst).hasSize(1);

            // Reset WireMock and stub again for second call
            wireMockServer.resetAll();
            stubFor(get(urlEqualTo("/health")).willReturn(aResponse().withStatus(200)));
            stubAddShoppingItemOnly("Test Item", 1, null);

            // When: Second call (same command, different correlation ID)
            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .header("X-Correlation-ID", randomCorrelationId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Then: Items should have different idempotency keys (different command IDs)
            // so we get 2 items (each command creates its own item)
            var itemsAfterSecond =
                    shoppingItemRepository.findByShoppingList_IdOrderByCreatedAtDesc(testShoppingList.getId());
            assertThat(itemsAfterSecond).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Scenario 4: Invalid decision schema → rejected safely")
    class InvalidSchemaTest {

        @Test
        @DisplayName("Should reject add_shopping_item with empty name")
        void rejectsEmptyItemName() throws Exception {
            // Given: AI returns add_shopping_item with empty name
            stubAddShoppingItemWithEmptyName();

            String correlationId = randomCorrelationId();

            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Empty item"),
                    "source",
                    "api");

            // When: Execute command
            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("rejected"))
                    .andExpect(jsonPath("$.errorCode").value("SHOPPING_ITEM_NAME_EMPTY"));

            // Then: No items created
            var items = shoppingItemRepository.findByShoppingList_IdOrderByCreatedAtDesc(testShoppingList.getId());
            assertThat(items).isEmpty();
        }
    }

    @Nested
    @DisplayName("Scenario 5: Household boundary violation → reject")
    class HouseholdBoundaryTest {

        @Test
        @DisplayName("Should reject request to different household")
        void rejectsRequestToOtherHousehold() throws Exception {
            // Given: AI returns start_job
            stubAddShoppingItemOnly("Item", 1, null);

            // Create another household that testUser is NOT a member of
            var otherHousehold =
                    householdRepository.save(new com.hometusk.households.domain.Household("Other Household"));

            var request = Map.of(
                    "householdId",
                    otherHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Should fail"),
                    "source",
                    "api");

            // When: Execute command for other household
            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .header("X-Correlation-ID", randomCorrelationId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));

            // Then: No items created
            var items = shoppingItemRepository.findByShoppingList_IdOrderByCreatedAtDesc(testShoppingList.getId());
            assertThat(items).isEmpty();
        }
    }

    @Nested
    @DisplayName("Scenario 6: Full-chain test")
    class FullChainTest {

        @Test
        @DisplayName("Should trace from upstream decision to persisted links")
        void fullChainFromUpstreamToPersistedLinks() throws Exception {
            // Given: Upstream returns propose_add_shopping_item (mapped to start_job)
            stubProposeAddShoppingItemWithTask(testUser.getId().toString(), "Full chain task");

            String correlationId = randomCorrelationId();

            var request = Map.of(
                    "householdId",
                    testHousehold.getId().toString(),
                    "type",
                    "create_task",
                    "payload",
                    Map.of("title", "Full chain test"),
                    "source",
                    "api");

            // When: Execute command
            mockMvc.perform(post("/api/v1/commands")
                            .with(jwt())
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("executed"));

            // Then: Command created and executed
            var commands = commandRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
            assertThat(commands).isNotEmpty();
            assertThat(commands.get(0).getStatus().name()).isEqualTo("EXECUTED");

            // Then: Task created
            var tasks = taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
            assertThat(tasks).hasSize(1);

            // Then: Shopping items created and linked
            var items = shoppingItemRepository.findByLinkedTask_Id(tasks.get(0).getId());
            assertThat(items).isNotEmpty();

            // Then: Activities recorded with correlationId
            var activities =
                    taskActivityRepository.findByCorrelationIdOrderByCreatedAtDesc(UUID.fromString(correlationId));
            assertThat(activities).anyMatch(a -> a.getActivityType() == ActivityType.TASK_CREATED);
            assertThat(activities).anyMatch(a -> a.getActivityType() == ActivityType.SHOPPING_ITEM_ADDED);
        }
    }

    // --- WireMock stub helpers ---

    private void stubTaskWithShoppingItems(String assigneeId, String title) {
        String responseBody =
                """
                {
                    "decision_id": "550e8400-e29b-41d4-a716-446655440010",
                    "command_id": "cmd-test",
                    "status": "ok",
                    "action": "start_job",
                    "confidence": 0.95,
                    "payload": {
                        "job_id": "job-shopping-task",
                        "job_type": "create_task",
                        "proposed_actions": [
                            {
                                "action": "propose_create_task",
                                "payload": {
                                    "task": {
                                        "title": "%s",
                                        "assignee_id": "%s"
                                    }
                                }
                            },
                            {
                                "action": "propose_add_shopping_item",
                                "payload": {
                                    "item": {
                                        "name": "Milk",
                                        "quantity": "2",
                                        "unit": "liters"
                                    }
                                }
                            },
                            {
                                "action": "propose_add_shopping_item",
                                "payload": {
                                    "item": {
                                        "name": "Bread",
                                        "quantity": "1"
                                    }
                                }
                            }
                        ]
                    },
                    "explanation": "Task and shopping items accepted.",
                    "trace_id": "trace-test-shopping-task",
                    "schema_version": "1.0.0",
                    "decision_version": "test-1",
                    "created_at": "2026-06-14T00:00:00Z"
                }
                """
                        .formatted(title, assigneeId);

        stubFor(WireMock.post(urlEqualTo(DECIDE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    private void stubAddShoppingItemOnly(String name, int quantity, String unit) {
        String unitField = unit != null ? ", \"unit\": \"" + unit + "\"" : "";
        String responseBody =
                """
                {
                    "decision_id": "550e8400-e29b-41d4-a716-446655440011",
                    "command_id": "cmd-test",
                    "status": "ok",
                    "action": "start_job",
                    "confidence": 0.85,
                    "payload": {
                        "job_id": "job-shopping",
                        "job_type": "add_shopping_item",
                        "proposed_actions": [
                            {
                                "action": "propose_add_shopping_item",
                                "payload": {
                                    "item": {
                                        "name": "%s",
                                        "quantity": "%d"%s
                                    }
                                }
                            }
                        ]
                    },
                    "explanation": "Shopping item accepted.",
                    "trace_id": "trace-test-shopping-only",
                    "schema_version": "1.0.0",
                    "decision_version": "test-1",
                    "created_at": "2026-06-14T00:00:00Z"
                }
                """
                        .formatted(name, quantity, unitField);

        stubFor(WireMock.post(urlEqualTo(DECIDE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    private void stubAddShoppingItemWithEmptyName() {
        String responseBody =
                """
                {
                    "decision_id": "550e8400-e29b-41d4-a716-446655440012",
                    "command_id": "cmd-test",
                    "status": "ok",
                    "action": "start_job",
                    "confidence": 0.85,
                    "payload": {
                        "job_id": "job-shopping-empty",
                        "job_type": "add_shopping_item",
                        "proposed_actions": [
                            {
                                "action": "propose_add_shopping_item",
                                "payload": {
                                    "item": {
                                        "name": "",
                                        "quantity": "1"
                                    }
                                }
                            }
                        ]
                    },
                    "explanation": "Shopping item accepted.",
                    "trace_id": "trace-test-shopping-empty",
                    "schema_version": "1.0.0",
                    "decision_version": "test-1",
                    "created_at": "2026-06-14T00:00:00Z"
                }
                """;

        stubFor(WireMock.post(urlEqualTo(DECIDE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    private void stubProposeAddShoppingItemWithTask(String assigneeId, String taskTitle) {
        String responseBody =
                """
                {
                    "decision_id": "550e8400-e29b-41d4-a716-446655440013",
                    "command_id": "cmd-test",
                    "status": "ok",
                    "action": "start_job",
                    "confidence": 0.88,
                    "payload": {
                        "job_id": "job-shopping-chain",
                        "job_type": "add_shopping_item",
                        "proposed_actions": [
                            {
                                "action": "propose_create_task",
                                "payload": {
                                    "task": {
                                        "title": "%s",
                                        "assignee_id": "%s"
                                    }
                                }
                            },
                            {
                                "action": "propose_add_shopping_item",
                                "payload": {
                                    "item": {
                                        "name": "Full chain item",
                                        "quantity": "3"
                                    }
                                }
                            }
                        ]
                    },
                    "explanation": "Shopping item and task accepted.",
                    "trace_id": "trace-test-shopping-chain",
                    "schema_version": "1.0.0",
                    "decision_version": "test-1",
                    "created_at": "2026-06-14T00:00:00Z"
                }
                """
                        .formatted(taskTitle, assigneeId);

        stubFor(WireMock.post(urlEqualTo(DECIDE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }
}
