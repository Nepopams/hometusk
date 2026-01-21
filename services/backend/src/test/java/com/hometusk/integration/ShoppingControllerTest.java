package com.hometusk.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hometusk.activity.domain.ActivityType;
import com.hometusk.activity.repository.TaskActivityRepository;
import com.hometusk.shopping.domain.ShoppingItem;
import com.hometusk.shopping.domain.ShoppingList;
import com.hometusk.shopping.repository.ShoppingItemRepository;
import com.hometusk.shopping.repository.ShoppingListRepository;
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
