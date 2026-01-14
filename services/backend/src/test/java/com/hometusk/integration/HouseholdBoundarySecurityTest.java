package com.hometusk.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hometusk.households.domain.Household;
import com.hometusk.households.domain.Zone;
import com.hometusk.shopping.domain.ShoppingItem;
import com.hometusk.shopping.domain.ShoppingList;
import com.hometusk.shopping.repository.ShoppingItemRepository;
import com.hometusk.shopping.repository.ShoppingListRepository;
import com.hometusk.tasks.domain.Task;
import com.hometusk.tasks.domain.TaskStatus;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Integration tests for household boundary security (IDOR prevention).
 *
 * <p>Verifies that users cannot access resources from households they don't belong to.
 * Each test creates a separate "other household" that the test user is NOT a member of.
 */
@DisplayName("Household Boundary Security Tests")
class HouseholdBoundarySecurityTest extends IntegrationTestBase {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private ShoppingItemRepository shoppingItemRepository;

    private Household otherHousehold;
    private Zone otherZone;
    private Task otherTask;
    private ShoppingList otherShoppingList;
    private ShoppingItem otherShoppingItem;

    @BeforeEach
    void setUpOtherHousehold() {
        // Create another household that testUser is NOT a member of
        otherHousehold = new Household("Other Household");
        otherHousehold = householdRepository.save(otherHousehold);

        // testUser2 is a member of the other household
        Membership otherMembership = new Membership(testUser2, otherHousehold, MembershipRole.admin);
        membershipRepository.save(otherMembership);

        // Create resources in the other household
        otherZone = new Zone(otherHousehold, "Other Kitchen");
        otherZone = zoneRepository.save(otherZone);

        otherTask = new Task(otherHousehold, "Other Task", testUser2);
        otherTask.setStatus(TaskStatus.OPEN);
        otherTask = taskRepository.save(otherTask);

        otherShoppingList = new ShoppingList(otherHousehold, "Other Shopping List");
        otherShoppingList = shoppingListRepository.save(otherShoppingList);

        otherShoppingItem = new ShoppingItem(otherShoppingList, "Other Item", 1, testUser2);
        otherShoppingItem = shoppingItemRepository.save(otherShoppingItem);
    }

    @Nested
    @DisplayName("Household Endpoints")
    class HouseholdEndpointsTest {

        @Test
        @DisplayName("Should reject listing members of other household")
        void cannotListOtherHouseholdMembers() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/members", otherHousehold.getId())
                            .with(jwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject listing zones of other household")
        void cannotListOtherHouseholdZones() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/zones", otherHousehold.getId())
                            .with(jwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject creating zone in other household")
        void cannotCreateZoneInOtherHousehold() throws Exception {
            var request = Map.of("name", "Hacked Zone");

            mockMvc.perform(post("/api/v1/households/{id}/zones", otherHousehold.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Task Endpoints")
    class TaskEndpointsTest {

        @Test
        @DisplayName("Should reject listing tasks of other household")
        void cannotListOtherHouseholdTasks() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/tasks", otherHousehold.getId())
                            .with(jwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject getting task detail from other household")
        void cannotGetOtherHouseholdTaskDetail() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/tasks/{taskId}", otherHousehold.getId(), otherTask.getId())
                            .with(jwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject getting task from other household via ID in own household URL")
        void cannotGetOtherHouseholdTaskViaOwnHouseholdUrl() throws Exception {
            // Try to access otherTask but through testHousehold's URL - should return 404
            mockMvc.perform(get("/api/v1/households/{id}/tasks/{taskId}", testHousehold.getId(), otherTask.getId())
                            .with(jwt()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Shopping Endpoints")
    class ShoppingEndpointsTest {

        @Test
        @DisplayName("Should reject listing shopping lists of other household")
        void cannotListOtherHouseholdShoppingLists() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/shopping-lists", otherHousehold.getId())
                            .with(jwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject listing items from other household's shopping list")
        void cannotListOtherHouseholdShoppingItems() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/shopping-lists/{listId}/items",
                            otherHousehold.getId(), otherShoppingList.getId())
                            .with(jwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject adding item to other household's shopping list")
        void cannotAddItemToOtherHouseholdShoppingList() throws Exception {
            var request = Map.of("name", "Hacked Item", "quantity", 1);

            mockMvc.perform(post("/api/v1/households/{id}/shopping-lists/{listId}/items",
                            otherHousehold.getId(), otherShoppingList.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject updating item in other household")
        void cannotUpdateOtherHouseholdShoppingItem() throws Exception {
            var request = Map.of("purchased", true);

            mockMvc.perform(patch("/api/v1/households/{id}/shopping-items/{itemId}",
                            otherHousehold.getId(), otherShoppingItem.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject deleting item from other household")
        void cannotDeleteOtherHouseholdShoppingItem() throws Exception {
            mockMvc.perform(delete("/api/v1/households/{id}/shopping-items/{itemId}",
                            otherHousehold.getId(), otherShoppingItem.getId())
                            .with(jwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject updating other household's item via own household URL")
        void cannotUpdateOtherHouseholdItemViaOwnHouseholdUrl() throws Exception {
            var request = Map.of("purchased", true);

            // Try to update otherShoppingItem but through testHousehold's URL - should return 404
            mockMvc.perform(patch("/api/v1/households/{id}/shopping-items/{itemId}",
                            testHousehold.getId(), otherShoppingItem.getId())
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should reject deleting other household's item via own household URL")
        void cannotDeleteOtherHouseholdItemViaOwnHouseholdUrl() throws Exception {
            // Try to delete otherShoppingItem but through testHousehold's URL - should return 404
            mockMvc.perform(delete("/api/v1/households/{id}/shopping-items/{itemId}",
                            testHousehold.getId(), otherShoppingItem.getId())
                            .with(jwt()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("Should reject unauthenticated access to tasks")
        void unauthenticatedAccessToTasksDenied() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/tasks", testHousehold.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject unauthenticated access to shopping lists")
        void unauthenticatedAccessToShoppingListsDenied() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/shopping-lists", testHousehold.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject unauthenticated user profile access")
        void unauthenticatedAccessToUserProfileDenied() throws Exception {
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject unauthenticated household creation")
        void unauthenticatedHouseholdCreationDenied() throws Exception {
            var request = Map.of("name", "Hacked Household");

            mockMvc.perform(post("/api/v1/households")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
