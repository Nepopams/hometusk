package com.hometusk.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hometusk.activity.domain.ActivityType;
import com.hometusk.activity.repository.TaskActivityRepository;
import com.hometusk.shopping.domain.ShoppingItem;
import com.hometusk.shopping.domain.ShoppingList;
import com.hometusk.shopping.repository.ShoppingItemRepository;
import com.hometusk.shopping.repository.ShoppingListRepository;
import com.hometusk.tasks.domain.Task;
import com.hometusk.tasks.repository.TaskRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Integration tests for ShoppingController.
 */
@DisplayName("ShoppingController Integration Tests")
class ShoppingControllerTest extends IntegrationTestBase {

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private ShoppingItemRepository shoppingItemRepository;

    @Autowired
    private TaskActivityRepository taskActivityRepository;

    @Autowired
    private TaskRepository taskRepository;

    private ShoppingList shoppingList;
    private ShoppingItem item1;
    private ShoppingItem item2Purchased;

    @BeforeEach
    void setUpShoppingData() {
        shoppingList = new ShoppingList(testHousehold, "Grocery List");
        shoppingList = shoppingListRepository.save(shoppingList);

        item1 = new ShoppingItem(shoppingList, "Milk", testUser);
        item1.setQuantity(2);
        item1.setUnit("liters");
        item1 = shoppingItemRepository.save(item1);

        item2Purchased = new ShoppingItem(shoppingList, "Bread", testUser);
        item2Purchased.setQuantity(1);
        item2Purchased.markPurchased();
        item2Purchased = shoppingItemRepository.save(item2Purchased);
    }

    @Nested
    @DisplayName("GET /api/v1/households/{id}/shopping-lists")
    class ListShoppingListsTests {

        @Test
        @DisplayName("Should list shopping lists with unpurchased counts")
        void listShoppingListsReturnsWithCounts() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/shopping-lists", testHousehold.getId())
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(shoppingList.getId().toString()))
                    .andExpect(jsonPath("$[0].name").value("Grocery List"))
                    .andExpect(jsonPath("$[0].unpurchasedCount").value(1));
        }

        @Test
        @DisplayName("Should reject listing for non-member")
        void listShoppingListsRejectsNonMember() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/shopping-lists", testHousehold.getId())
                            .with(jwtForUser(testUser2)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/households/{id}/shopping-lists")
    class CreateShoppingListTests {

        @Test
        @DisplayName("Should create a shopping list")
        void createShoppingListCreatesList() throws Exception {
            var request = Map.of("name", "  Hardware  ");

            mockMvc.perform(post("/api/v1/households/{id}/shopping-lists", testHousehold.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("Hardware"))
                    .andExpect(jsonPath("$.householdId").value(testHousehold.getId().toString()))
                    .andExpect(jsonPath("$.unpurchasedCount").value(0));

            assertThat(shoppingListRepository.findByHousehold_IdAndName(testHousehold.getId(), "Hardware"))
                    .isPresent();
        }

        @Test
        @DisplayName("Should reject blank shopping list name")
        void createShoppingListRejectsBlankName() throws Exception {
            var request = Map.of("name", "   ");

            mockMvc.perform(post("/api/v1/households/{id}/shopping-lists", testHousehold.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject create list for non-member")
        void createShoppingListRejectsNonMember() throws Exception {
            var request = Map.of("name", "Hardware");

            mockMvc.perform(post("/api/v1/households/{id}/shopping-lists", testHousehold.getId())
                            .with(jwtForUser(testUser2))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/households/{id}/shopping-lists/{listId}/items")
    class ListItemsTests {

        @Test
        @DisplayName("Should list all items in shopping list")
        void listItemsReturnsAllItems() throws Exception {
            mockMvc.perform(get(
                                    "/api/v1/households/{id}/shopping-lists/{listId}/items",
                                    testHousehold.getId(),
                                    shoppingList.getId())
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").exists())
                    .andExpect(jsonPath("$[0].quantity").exists());
        }

        @Test
        @DisplayName("Should filter items by purchased=false")
        void listItemsFiltersByPurchasedFalse() throws Exception {
            mockMvc.perform(get(
                                    "/api/v1/households/{id}/shopping-lists/{listId}/items",
                                    testHousehold.getId(),
                                    shoppingList.getId())
                            .with(jwt())
                            .param("purchased", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("Milk"));
        }

        @Test
        @DisplayName("Should filter items by category and source")
        void listItemsFiltersByCategoryAndSource() throws Exception {
            item1.setCategory("groceries");
            item1.setSource("Perekrestok");
            shoppingItemRepository.saveAndFlush(item1);

            item2Purchased.setCategory("cleaning");
            item2Purchased.setSource("Ozon");
            shoppingItemRepository.saveAndFlush(item2Purchased);

            mockMvc.perform(get(
                                    "/api/v1/households/{id}/shopping-lists/{listId}/items",
                                    testHousehold.getId(),
                                    shoppingList.getId())
                            .with(jwt())
                            .param("category", "groceries")
                            .param("source", " Perekrestok "))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("Milk"))
                    .andExpect(jsonPath("$[0].category").value("groceries"))
                    .andExpect(jsonPath("$[0].source").value("Perekrestok"));
        }

        @Test
        @DisplayName("Should reject invalid category filter")
        void listItemsRejectsInvalidCategory() throws Exception {
            mockMvc.perform(get(
                                    "/api/v1/households/{id}/shopping-lists/{listId}/items",
                                    testHousehold.getId(),
                                    shoppingList.getId())
                            .with(jwt())
                            .param("category", "unknown_bucket"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 for non-existent list")
        void listItemsReturnsNotFoundForMissingList() throws Exception {
            mockMvc.perform(get(
                                    "/api/v1/households/{id}/shopping-lists/{listId}/items",
                                    testHousehold.getId(),
                                    UUID.randomUUID())
                            .with(jwt()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/households/{id}/shopping-lists/{listId}/items")
    class AddItemTests {

        @Test
        @DisplayName("Should add item to shopping list")
        void addItemCreatesNewItem() throws Exception {
            var request = Map.of(
                    "name", "Eggs",
                    "quantity", 12,
                    "unit", "pcs");

            String correlationId = randomCorrelationId();

            mockMvc.perform(post(
                                    "/api/v1/households/{id}/shopping-lists/{listId}/items",
                                    testHousehold.getId(),
                                    shoppingList.getId())
                            .with(jwt())
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("Eggs"))
                    .andExpect(jsonPath("$.quantity").value(12))
                    .andExpect(jsonPath("$.unit").value("pcs"))
                    .andExpect(jsonPath("$.category").value(nullValue()))
                    .andExpect(jsonPath("$.source").value(nullValue()))
                    .andExpect(jsonPath("$.purchased").value(false));

            // Verify item was created
            var items = shoppingItemRepository.findByShoppingList_IdOrderByCreatedAtDesc(shoppingList.getId());
            assertThat(items).hasSize(3);
            assertThat(items).anyMatch(i -> i.getName().equals("Eggs"));

            // Verify activity recorded
            var activities =
                    taskActivityRepository.findByCorrelationIdOrderByCreatedAtDesc(UUID.fromString(correlationId));
            assertThat(activities).anyMatch(a -> a.getActivityType() == ActivityType.SHOPPING_ITEM_ADDED);
        }

        @Test
        @DisplayName("Should add item with category and source")
        void addItemCreatesNewItemWithCategoryAndSource() throws Exception {
            var request = Map.of(
                    "name", "Soap",
                    "quantity", 2,
                    "unit", "pcs",
                    "category", "cleaning",
                    "source", " Ozon ");

            mockMvc.perform(post(
                                    "/api/v1/households/{id}/shopping-lists/{listId}/items",
                                    testHousehold.getId(),
                                    shoppingList.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Soap"))
                    .andExpect(jsonPath("$.category").value("cleaning"))
                    .andExpect(jsonPath("$.source").value("Ozon"));

            var saved = shoppingItemRepository.findByShoppingList_IdOrderByCreatedAtDesc(shoppingList.getId()).stream()
                    .filter(i -> i.getName().equals("Soap"))
                    .findFirst()
                    .orElseThrow();
            assertThat(saved.getCategory()).isEqualTo("cleaning");
            assertThat(saved.getSource()).isEqualTo("Ozon");
        }

        @Test
        @DisplayName("Should add item linked to same-household task")
        void addItemCreatesNewItemLinkedToTask() throws Exception {
            Task task = taskRepository.save(new Task(testHousehold, "Buy dinner supplies", testUser));
            var request = Map.of(
                    "name", "Chicken",
                    "quantity", 1,
                    "unit", "kg",
                    "category", "groceries",
                    "source", "Market",
                    "linkedTaskId", task.getId().toString());

            mockMvc.perform(post(
                                    "/api/v1/households/{id}/shopping-lists/{listId}/items",
                                    testHousehold.getId(),
                                    shoppingList.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Chicken"))
                    .andExpect(jsonPath("$.linkedTaskId").value(task.getId().toString()));

            var saved = shoppingItemRepository.findByShoppingList_IdOrderByCreatedAtDesc(shoppingList.getId()).stream()
                    .filter(i -> i.getName().equals("Chicken"))
                    .findFirst()
                    .orElseThrow();
            assertThat(saved.getLinkedTaskId()).isEqualTo(task.getId());
        }

        @Test
        @DisplayName("Should reject cross-household linked task without creating item")
        void addItemRejectsCrossHouseholdLinkedTask() throws Exception {
            var otherHousehold = householdRepository.save(new com.hometusk.households.domain.Household("Other"));
            Task otherTask = taskRepository.save(new Task(otherHousehold, "Other task", testUser));
            var request = Map.of(
                    "name", "Private item",
                    "linkedTaskId", otherTask.getId().toString());

            mockMvc.perform(post(
                                    "/api/v1/households/{id}/shopping-lists/{listId}/items",
                                    testHousehold.getId(),
                                    shoppingList.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("TASK_NOT_FOUND"));

            assertThat(shoppingItemRepository.findByShoppingList_IdOrderByCreatedAtDesc(shoppingList.getId()))
                    .extracting(ShoppingItem::getName)
                    .doesNotContain("Private item");
        }

        @Test
        @DisplayName("Should reject invalid category without creating item")
        void addItemRejectsInvalidCategory() throws Exception {
            var request = Map.of(
                    "name", "Mystery",
                    "category", "unknown_bucket");

            mockMvc.perform(post(
                                    "/api/v1/households/{id}/shopping-lists/{listId}/items",
                                    testHousehold.getId(),
                                    shoppingList.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertThat(shoppingItemRepository.findByShoppingList_IdOrderByCreatedAtDesc(shoppingList.getId()))
                    .extracting(ShoppingItem::getName)
                    .doesNotContain("Mystery");
        }

        @Test
        @DisplayName("Should add item with default quantity")
        void addItemWithDefaultQuantity() throws Exception {
            var request = Map.of("name", "Butter");

            mockMvc.perform(post(
                                    "/api/v1/households/{id}/shopping-lists/{listId}/items",
                                    testHousehold.getId(),
                                    shoppingList.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.quantity").value(1));
        }

        @Test
        @DisplayName("Should reject blank item name")
        void addItemRejectsBlankName() throws Exception {
            var request = Map.of("name", "   ");

            mockMvc.perform(post(
                                    "/api/v1/households/{id}/shopping-lists/{listId}/items",
                                    testHousehold.getId(),
                                    shoppingList.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject adding to non-existent list")
        void addItemRejectsNonExistentList() throws Exception {
            var request = Map.of("name", "Cheese");

            mockMvc.perform(post(
                                    "/api/v1/households/{id}/shopping-lists/{listId}/items",
                                    testHousehold.getId(),
                                    UUID.randomUUID())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should include newly added item in list response")
        void addItemAppearsInList() throws Exception {
            var request = Map.of("name", "Apples", "quantity", 4);

            mockMvc.perform(post(
                                    "/api/v1/households/{id}/shopping-lists/{listId}/items",
                                    testHousehold.getId(),
                                    shoppingList.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get(
                                    "/api/v1/households/{id}/shopping-lists/{listId}/items",
                                    testHousehold.getId(),
                                    shoppingList.getId())
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].name").value("Apples"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/households/{id}/shopping-items/{itemId}")
    class UpdateItemTests {

        @Test
        @DisplayName("Should mark item as purchased")
        void markPurchasedUpdatesItem() throws Exception {
            var request = Map.of("purchased", true);
            String correlationId = randomCorrelationId();

            mockMvc.perform(patch(
                                    "/api/v1/households/{id}/shopping-items/{itemId}",
                                    testHousehold.getId(),
                                    item1.getId())
                            .with(jwt())
                            .header("X-Correlation-ID", correlationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(item1.getId().toString()))
                    .andExpect(jsonPath("$.purchased").value(true));

            // Verify DB updated
            var updated = shoppingItemRepository.findById(item1.getId()).orElseThrow();
            assertThat(updated.isPurchased()).isTrue();

            // Verify activity recorded
            var activities =
                    taskActivityRepository.findByCorrelationIdOrderByCreatedAtDesc(UUID.fromString(correlationId));
            assertThat(activities).anyMatch(a -> a.getActivityType() == ActivityType.SHOPPING_ITEM_PURCHASED);
        }

        @Test
        @DisplayName("Should update category and source without changing purchase state")
        void updateCategoryAndSourcePreservesPurchaseState() throws Exception {
            var request = Map.of(
                    "category", "groceries",
                    "source", " Perekrestok ");

            mockMvc.perform(patch(
                                    "/api/v1/households/{id}/shopping-items/{itemId}",
                                    testHousehold.getId(),
                                    item1.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.purchased").value(false))
                    .andExpect(jsonPath("$.category").value("groceries"))
                    .andExpect(jsonPath("$.source").value("Perekrestok"));

            var updated = shoppingItemRepository.findById(item1.getId()).orElseThrow();
            assertThat(updated.isPurchased()).isFalse();
            assertThat(updated.getCategory()).isEqualTo("groceries");
            assertThat(updated.getSource()).isEqualTo("Perekrestok");
        }

        @Test
        @DisplayName("Should clear category and source with explicit null or blank")
        void updateClearsCategoryAndSource() throws Exception {
            item1.setCategory("groceries");
            item1.setSource("Perekrestok");
            shoppingItemRepository.saveAndFlush(item1);

            String request =
                    """
                    {
                      "category": null,
                      "source": "   "
                    }
                    """;

            mockMvc.perform(patch(
                                    "/api/v1/households/{id}/shopping-items/{itemId}",
                                    testHousehold.getId(),
                                    item1.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.category").value(nullValue()))
                    .andExpect(jsonPath("$.source").value(nullValue()));

            var updated = shoppingItemRepository.findById(item1.getId()).orElseThrow();
            assertThat(updated.getCategory()).isNull();
            assertThat(updated.getSource()).isNull();
        }

        @Test
        @DisplayName("Should link and unlink item to task")
        void updateLinksAndUnlinksItemToTask() throws Exception {
            Task task = taskRepository.save(new Task(testHousehold, "Plan dinner", testUser));
            String linkRequest = """
                    {
                      "linkedTaskId": "%s"
                    }
                    """
                    .formatted(task.getId());

            mockMvc.perform(patch(
                                    "/api/v1/households/{id}/shopping-items/{itemId}",
                                    testHousehold.getId(),
                                    item1.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(linkRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.linkedTaskId").value(task.getId().toString()));

            var linked = shoppingItemRepository.findById(item1.getId()).orElseThrow();
            assertThat(linked.getLinkedTaskId()).isEqualTo(task.getId());

            String unlinkRequest = """
                    {
                      "linkedTaskId": null
                    }
                    """;

            mockMvc.perform(patch(
                                    "/api/v1/households/{id}/shopping-items/{itemId}",
                                    testHousehold.getId(),
                                    item1.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(unlinkRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.linkedTaskId").value(nullValue()));

            var unlinked = shoppingItemRepository.findById(item1.getId()).orElseThrow();
            assertThat(unlinked.getLinkedTaskId()).isNull();
        }

        @Test
        @DisplayName("Should reject cross-household linked task update")
        void updateRejectsCrossHouseholdLinkedTask() throws Exception {
            var otherHousehold = householdRepository.save(new com.hometusk.households.domain.Household("Other"));
            Task otherTask = taskRepository.save(new Task(otherHousehold, "Other task", testUser));
            String request = """
                    {
                      "linkedTaskId": "%s"
                    }
                    """
                    .formatted(otherTask.getId());

            mockMvc.perform(patch(
                                    "/api/v1/households/{id}/shopping-items/{itemId}",
                                    testHousehold.getId(),
                                    item1.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("TASK_NOT_FOUND"));

            var unchanged = shoppingItemRepository.findById(item1.getId()).orElseThrow();
            assertThat(unchanged.getLinkedTaskId()).isNull();
        }

        @Test
        @DisplayName("Should reject empty patch")
        void updateItemRejectsEmptyPatch() throws Exception {
            mockMvc.perform(patch(
                                    "/api/v1/households/{id}/shopping-items/{itemId}",
                                    testHousehold.getId(),
                                    item1.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject invalid category without changing purchase state")
        void updateItemRejectsInvalidCategoryWithoutMutation() throws Exception {
            var request = Map.of("purchased", true, "category", "unknown_bucket");

            mockMvc.perform(patch(
                                    "/api/v1/households/{id}/shopping-items/{itemId}",
                                    testHousehold.getId(),
                                    item1.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            var updated = shoppingItemRepository.findById(item1.getId()).orElseThrow();
            assertThat(updated.isPurchased()).isFalse();
            assertThat(updated.getCategory()).isNull();
        }

        @Test
        @DisplayName("Should unmark item as purchased")
        void unmarkPurchasedUpdatesItem() throws Exception {
            var request = Map.of("purchased", false);

            mockMvc.perform(patch(
                                    "/api/v1/households/{id}/shopping-items/{itemId}",
                                    testHousehold.getId(),
                                    item2Purchased.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.purchased").value(false));

            // Verify DB updated
            var updated =
                    shoppingItemRepository.findById(item2Purchased.getId()).orElseThrow();
            assertThat(updated.isPurchased()).isFalse();
        }

        @Test
        @DisplayName("Should return 404 for non-existent item")
        void updateItemReturnsNotFoundForMissingItem() throws Exception {
            var request = Map.of("purchased", true);

            mockMvc.perform(patch(
                                    "/api/v1/households/{id}/shopping-items/{itemId}",
                                    testHousehold.getId(),
                                    UUID.randomUUID())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/households/{id}/shopping-items/{itemId}")
    class DeleteItemTests {

        @Test
        @DisplayName("Should delete item from shopping list")
        void deleteItemRemovesItem() throws Exception {
            String correlationId = randomCorrelationId();

            mockMvc.perform(delete(
                                    "/api/v1/households/{id}/shopping-items/{itemId}",
                                    testHousehold.getId(),
                                    item1.getId())
                            .with(jwt())
                            .header("X-Correlation-ID", correlationId))
                    .andExpect(status().isNoContent());

            // Verify item was deleted
            assertThat(shoppingItemRepository.findById(item1.getId())).isEmpty();

            // Verify activity recorded
            var activities =
                    taskActivityRepository.findByCorrelationIdOrderByCreatedAtDesc(UUID.fromString(correlationId));
            assertThat(activities).anyMatch(a -> a.getActivityType() == ActivityType.SHOPPING_ITEM_DELETED);
        }

        @Test
        @DisplayName("Should return 404 for non-existent item")
        void deleteItemReturnsNotFoundForMissingItem() throws Exception {
            mockMvc.perform(delete(
                                    "/api/v1/households/{id}/shopping-items/{itemId}",
                                    testHousehold.getId(),
                                    UUID.randomUUID())
                            .with(jwt()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should reject delete for non-member")
        void deleteItemRejectsNonMember() throws Exception {
            mockMvc.perform(delete(
                                    "/api/v1/households/{id}/shopping-items/{itemId}",
                                    testHousehold.getId(),
                                    item1.getId())
                            .with(jwtForUser(testUser2)))
                    .andExpect(status().isForbidden());
        }
    }
}
