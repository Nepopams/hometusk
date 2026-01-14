package com.hometusk.tasks.api;

import com.hometusk.shared.security.CurrentUser;
import com.hometusk.shopping.domain.ShoppingItem;
import com.hometusk.shopping.service.ShoppingService;
import com.hometusk.tasks.domain.Task;
import com.hometusk.tasks.domain.TaskStatus;
import com.hometusk.tasks.dto.TaskDetailDto;
import com.hometusk.tasks.dto.TaskDto;
import com.hometusk.tasks.service.TaskService;
import com.hometusk.users.service.MembershipService;
import com.hometusk.users.service.UserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/households/{householdId}/tasks")
@Tag(name = "Households", description = "Task management endpoints")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private final TaskService taskService;
    private final ShoppingService shoppingService;
    private final MembershipService membershipService;
    private final UserResolver userResolver;

    public TaskController(
            TaskService taskService,
            ShoppingService shoppingService,
            MembershipService membershipService,
            UserResolver userResolver) {
        this.taskService = taskService;
        this.shoppingService = shoppingService;
        this.membershipService = membershipService;
        this.userResolver = userResolver;
    }

    @GetMapping
    @Operation(
            summary = "List tasks in a household",
            description =
                    """
            Returns tasks in the household, optionally filtered by status, assignee, or zone.
            Results are sorted by creation date (newest first).
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of tasks"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household")
    })
    public ResponseEntity<List<TaskDto>> listTasks(
            @PathVariable UUID householdId,
            @RequestParam(required = false) @Parameter(description = "Filter by status (open, in_progress, done, cancelled)")
                    String status,
            @RequestParam(required = false) @Parameter(description = "Filter by assignee ID") UUID assigneeId,
            @RequestParam(required = false) @Parameter(description = "Filter by zone ID") UUID zoneId) {
        log.debug(
                "Listing tasks for household: {}, status: {}, assigneeId: {}, zoneId: {}",
                householdId,
                status,
                assigneeId,
                zoneId);

        // Verify membership (IDOR prevention)
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        // Parse status filter
        TaskStatus taskStatus = parseStatus(status);

        // Query tasks
        List<Task> tasks = taskService.findByHouseholdId(householdId, taskStatus, assigneeId, zoneId);

        // Convert to DTOs
        List<TaskDto> taskDtos = tasks.stream().map(TaskDto::from).toList();

        log.debug("Returning {} tasks for household: {}", taskDtos.size(), householdId);
        return ResponseEntity.ok(taskDtos);
    }

    @GetMapping("/{taskId}")
    @Operation(
            summary = "Get task details",
            description = "Returns task details including linked shopping items.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task details"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskDetailDto> getTask(@PathVariable UUID householdId, @PathVariable UUID taskId) {
        log.debug("Getting task details: householdId={}, taskId={}", householdId, taskId);

        // Verify membership (IDOR prevention)
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        // Get task (throws 404 if not found or not in household)
        Task task = taskService.getByIdAndHouseholdId(taskId, householdId);

        // Get linked shopping items
        List<ShoppingItem> linkedItems = shoppingService.getItemsForTask(taskId, householdId);

        // Build response
        TaskDetailDto detail = TaskDetailDto.from(task, linkedItems);

        return ResponseEntity.ok(detail);
    }

    /**
     * Parses status string to TaskStatus enum.
     * Supports case-insensitive matching.
     */
    private TaskStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return TaskStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid status value: {}", status);
            return null;
        }
    }
}
