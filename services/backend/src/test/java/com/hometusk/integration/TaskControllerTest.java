package com.hometusk.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hometusk.shopping.domain.ShoppingItem;
import com.hometusk.shopping.domain.ShoppingList;
import com.hometusk.shopping.repository.ShoppingItemRepository;
import com.hometusk.shopping.repository.ShoppingListRepository;
import com.hometusk.tasks.domain.Task;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for TaskController.
 */
@DisplayName("TaskController Integration Tests")
class TaskControllerTest extends IntegrationTestBase {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private ShoppingItemRepository shoppingItemRepository;

    private Task task1;
    private Task task2;
    private Task task3;
    private ShoppingList shoppingList;
    private ShoppingItem linkedItem;

    @BeforeEach
    void setUpTasks() {
        // Add testUser2 as member
        Membership membership2 = new Membership(testUser2, testHousehold, MembershipRole.member);
        membershipRepository.save(membership2);

        // Create tasks
        task1 = new Task(testHousehold, "Task 1 - Open", testUser);
        task1.setAssignee(testUser);
        task1.setZone(testZone);
        task1 = taskRepository.save(task1);

        task2 = new Task(testHousehold, "Task 2 - In Progress", testUser);
        task2.start();
        task2.setAssignee(testUser2);
        task2 = taskRepository.save(task2);

        task3 = new Task(testHousehold, "Task 3 - Done", testUser);
        task3.complete();
        task3.setAssignee(testUser);
        task3.setZone(testZone);
        task3 = taskRepository.save(task3);

        // Create shopping list and linked item
        shoppingList = new ShoppingList(testHousehold, "Default");
        shoppingList = shoppingListRepository.save(shoppingList);

        linkedItem = new ShoppingItem(shoppingList, "Linked Item", testUser);
        linkedItem.setQuantity(2);
        linkedItem.setLinkedTask(task1);
        linkedItem = shoppingItemRepository.save(linkedItem);
    }

    @Nested
    @DisplayName("GET /api/v1/households/{id}/tasks")
    class ListTasksTests {

        @Test
        @DisplayName("Should list all tasks in household")
        void listTasksReturnsAllTasks() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/tasks", testHousehold.getId())
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].id").exists())
                    .andExpect(jsonPath("$[0].title").exists())
                    .andExpect(jsonPath("$[0].status").exists());
        }

        @Test
        @DisplayName("Should filter tasks by status")
        void listTasksFiltersByStatus() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/tasks", testHousehold.getId())
                            .with(jwt())
                            .param("status", "open"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].title").value("Task 1 - Open"));
        }

        @Test
        @DisplayName("Should filter tasks by assigneeId")
        void listTasksFiltersByAssignee() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/tasks", testHousehold.getId())
                            .with(jwt())
                            .param("assigneeId", testUser2.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].title").value("Task 2 - In Progress"));
        }

        @Test
        @DisplayName("Should filter tasks by zoneId")
        void listTasksFiltersByZone() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/tasks", testHousehold.getId())
                            .with(jwt())
                            .param("zoneId", testZone.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("Should combine multiple filters")
        void listTasksCombinesFilters() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/tasks", testHousehold.getId())
                            .with(jwt())
                            .param("status", "open")
                            .param("assigneeId", testUser.getId().toString())
                            .param("zoneId", testZone.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].title").value("Task 1 - Open"));
        }

        @Test
        @DisplayName("Should reject listing tasks for non-member")
        void listTasksRejectsNonMember() throws Exception {
            // Remove testUser2 from members for this test
            membershipRepository.deleteAll(
                    membershipRepository.findByUserIdAndHouseholdId(testUser2.getId(), testHousehold.getId()).stream()
                            .toList());

            mockMvc.perform(get("/api/v1/households/{id}/tasks", testHousehold.getId())
                            .with(jwtForUser(testUser2)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/households/{id}/tasks/{taskId}")
    class GetTaskDetailTests {

        @Test
        @DisplayName("Should return task detail with linked shopping items")
        void getTaskDetailIncludesLinkedItems() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/tasks/{taskId}", testHousehold.getId(), task1.getId())
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(task1.getId().toString()))
                    .andExpect(jsonPath("$.title").value("Task 1 - Open"))
                    .andExpect(jsonPath("$.status").value("open"))
                    .andExpect(jsonPath("$.assignee.id").value(testUser.getId().toString()))
                    .andExpect(jsonPath("$.zone.id").value(testZone.getId().toString()))
                    .andExpect(jsonPath("$.linkedShoppingItems").isArray())
                    .andExpect(jsonPath("$.linkedShoppingItems.length()").value(1))
                    .andExpect(jsonPath("$.linkedShoppingItems[0].name").value("Linked Item"));
        }

        @Test
        @DisplayName("Should return task detail without linked items")
        void getTaskDetailWithoutLinkedItems() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/tasks/{taskId}", testHousehold.getId(), task2.getId())
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(task2.getId().toString()))
                    .andExpect(jsonPath("$.linkedShoppingItems").isArray())
                    .andExpect(jsonPath("$.linkedShoppingItems.length()").value(0));
        }

        @Test
        @DisplayName("Should return 404 for non-existent task")
        void getTaskDetailReturnsNotFoundForMissingTask() throws Exception {
            mockMvc.perform(get(
                                    "/api/v1/households/{id}/tasks/{taskId}",
                                    testHousehold.getId(),
                                    java.util.UUID.randomUUID())
                            .with(jwt()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should reject task detail for non-member")
        void getTaskDetailRejectsNonMember() throws Exception {
            // Remove testUser2 from members
            membershipRepository.deleteAll(
                    membershipRepository.findByUserIdAndHouseholdId(testUser2.getId(), testHousehold.getId()).stream()
                            .toList());

            mockMvc.perform(get("/api/v1/households/{id}/tasks/{taskId}", testHousehold.getId(), task1.getId())
                            .with(jwtForUser(testUser2)))
                    .andExpect(status().isForbidden());
        }
    }
}
