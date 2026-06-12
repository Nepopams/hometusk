package com.hometusk.integration;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hometusk.routines.domain.AssignmentPolicy;
import com.hometusk.routines.domain.RecurrenceRule;
import com.hometusk.routines.domain.Routine;
import com.hometusk.routines.domain.RoutineStatus;
import com.hometusk.routines.repository.RoutineRepository;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for TaskController.
 */
@DisplayName("TaskController Integration Tests")
class TaskControllerTest extends IntegrationTestBase {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private RoutineRepository routineRepository;

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private ShoppingItemRepository shoppingItemRepository;

    private Task task1;
    private Task task2;
    private Task task3;
    private Task taskWithRoutine;
    private Routine routine;
    private ShoppingList shoppingList;
    private ShoppingItem linkedItem;

    @BeforeEach
    void setUpTasks() throws Exception {
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

        routine = saveRoutine("Routine Task", RoutineStatus.ACTIVE);
        taskWithRoutine = new Task(testHousehold, "Task 4 - Routine", testUser);
        taskWithRoutine.setRoutine(routine);
        taskWithRoutine.setScheduledDate(java.time.LocalDate.now());
        taskWithRoutine.setCreatedVia("routine");
        taskWithRoutine.start();
        taskWithRoutine = taskRepository.save(taskWithRoutine);

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
                    .andExpect(jsonPath("$.length()").value(4))
                    .andExpect(jsonPath("$[0].id").exists())
                    .andExpect(jsonPath("$[0].title").exists())
                    .andExpect(jsonPath("$[0].status").exists());
        }

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("Should list command-created unassigned task without open session")
        void listTasksReturnsCommandCreatedUnassignedTaskWithoutOpenSession() throws Exception {
            Task commandTask = new Task(testHousehold, "Command-created task", testUser);
            commandTask.setCommandId(java.util.UUID.randomUUID());
            commandTask.setCreatedVia("command");
            commandTask = taskRepository.saveAndFlush(commandTask);

            mockMvc.perform(get("/api/v1/households/{id}/tasks", testHousehold.getId())
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id=='" + commandTask.getId() + "')].title")
                            .value(contains("Command-created task")))
                    .andExpect(jsonPath("$[?(@.id=='" + commandTask.getId() + "')].createdBy.id")
                            .value(contains(testUser.getId().toString())));
        }

        @Test
        @DisplayName("Should include routine summary when task has routine")
        void listTasksIncludesRoutineSummary() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/tasks", testHousehold.getId())
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id=='" + taskWithRoutine.getId() + "')].routine.id")
                            .value(contains(routine.getId().toString())))
                    .andExpect(jsonPath("$[?(@.id=='" + taskWithRoutine.getId() + "')].routine.status")
                            .value(contains(RoutineStatus.ACTIVE.name())));
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
                    membershipRepository.findByUser_IdAndHousehold_Id(testUser2.getId(), testHousehold.getId()).stream()
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
        @DisplayName("Should include routine summary for routine task")
        void getTask_withRoutine_includesRoutineSummary() throws Exception {
            mockMvc.perform(get(
                                    "/api/v1/households/{id}/tasks/{taskId}",
                                    testHousehold.getId(),
                                    taskWithRoutine.getId())
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.routine.id").value(routine.getId().toString()))
                    .andExpect(jsonPath("$.routine.title").value(routine.getTitle()))
                    .andExpect(jsonPath("$.routine.status").value(RoutineStatus.ACTIVE.name()));
        }

        @Test
        @DisplayName("Should include deleted routine status when routine is deleted")
        void getTask_withDeletedRoutine_includesDeletedStatus() throws Exception {
            Routine deletedRoutine = saveRoutine("Deleted Routine", RoutineStatus.DELETED);
            Task deletedRoutineTask = new Task(testHousehold, "Task 5 - Deleted Routine", testUser);
            deletedRoutineTask.setRoutine(deletedRoutine);
            deletedRoutineTask.setScheduledDate(java.time.LocalDate.now());
            deletedRoutineTask.setCreatedVia("routine");
            deletedRoutineTask = taskRepository.save(deletedRoutineTask);

            mockMvc.perform(get(
                                    "/api/v1/households/{id}/tasks/{taskId}",
                                    testHousehold.getId(),
                                    deletedRoutineTask.getId())
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.routine.status").value(RoutineStatus.DELETED.name()));
        }

        @Test
        @DisplayName("Should return null routine for manual task")
        void getTask_manualTask_routineIsNull() throws Exception {
            mockMvc.perform(get("/api/v1/households/{id}/tasks/{taskId}", testHousehold.getId(), task1.getId())
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.routine").doesNotExist());
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
                    membershipRepository.findByUser_IdAndHousehold_Id(testUser2.getId(), testHousehold.getId()).stream()
                            .toList());

            mockMvc.perform(get("/api/v1/households/{id}/tasks/{taskId}", testHousehold.getId(), task1.getId())
                            .with(jwtForUser(testUser2)))
                    .andExpect(status().isForbidden());
        }
    }

    private Routine saveRoutine(String title, RoutineStatus status) throws Exception {
        String ruleJson = objectMapper.writeValueAsString(new RecurrenceRule.Daily());
        Routine routine = new Routine(testHousehold, title, ruleJson, AssignmentPolicy.MANUAL, testUser);
        if (status == RoutineStatus.DELETED) {
            routine.softDelete();
        }
        return routineRepository.save(routine);
    }
}
