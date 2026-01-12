package com.hometusk.commands.pipeline;

import com.hometusk.activity.service.ActivityRecorder;
import com.hometusk.commands.domain.Command;
import com.hometusk.commands.pipeline.decision.DecisionResult;
import com.hometusk.households.domain.Household;
import com.hometusk.households.domain.Zone;
import com.hometusk.households.service.HouseholdService;
import com.hometusk.households.service.ZoneService;
import com.hometusk.tasks.domain.Task;
import com.hometusk.tasks.service.TaskService;
import com.hometusk.users.domain.User;
import com.hometusk.users.service.UserService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Executes the decided action by calling domain services.
 * Routes to appropriate service based on command type.
 */
@Component
public class ActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(ActionExecutor.class);

    private final TaskService taskService;
    private final HouseholdService householdService;
    private final ZoneService zoneService;
    private final UserService userService;
    private final ActivityRecorder activityRecorder;

    public ActionExecutor(
            TaskService taskService,
            HouseholdService householdService,
            ZoneService zoneService,
            UserService userService,
            ActivityRecorder activityRecorder) {
        this.taskService = taskService;
        this.householdService = householdService;
        this.zoneService = zoneService;
        this.userService = userService;
        this.activityRecorder = activityRecorder;
    }

    /**
     * Executes a create_task decision.
     */
    public CreateTaskResult executeCreateTask(
            DecisionEngine.CreateTaskDecision decision, Command command, UUID correlationId) {
        log.debug("Executing create_task: commandId={}", command.getId());

        Household household = householdService.getById(command.getHouseholdId());
        User createdBy = command.getRequester();

        // Resolve assignee
        User assignee = null;
        if (decision.assigneeId() != null) {
            assignee = userService.getById(decision.assigneeId());
        }

        // Resolve zone
        Zone zone = null;
        if (decision.zoneId() != null) {
            zone = zoneService.getByIdAndHouseholdId(decision.zoneId(), household.getId());
        }

        // Create the task
        Task task = taskService.create(TaskService.CreateTaskRequest.builder()
                .household(household)
                .title(decision.title())
                .description(decision.description())
                .createdBy(createdBy)
                .assignee(assignee)
                .zone(zone)
                .deadline(decision.deadline())
                .commandId(command.getId())
                .createdVia("command")
                .build());

        // Record activity
        activityRecorder.recordTaskCreated(task, createdBy, command.getId(), correlationId);

        log.info("Task created via command: taskId={}, commandId={}", task.getId(), command.getId());

        return new CreateTaskResult(task.getId(), task.getAssigneeId());
    }

    /**
     * Executes a complete_task decision.
     */
    public CompleteTaskResult executeCompleteTask(
            DecisionEngine.CompleteTaskDecision decision, Command command, UUID correlationId) {
        log.debug("Executing complete_task: commandId={}, taskId={}", command.getId(), decision.taskId());

        Task task = taskService.complete(decision.taskId(), command.getHouseholdId());

        // Record activity
        activityRecorder.recordTaskCompleted(task, command.getRequester(), command.getId(), correlationId);

        log.info("Task completed via command: taskId={}, commandId={}", task.getId(), command.getId());

        return new CompleteTaskResult(task.getId());
    }

    /**
     * Executes a proposed action from DecisionResult (Stage 2).
     */
    public ActionResult executeAction(
            DecisionResult.StartJob.ProposedAction action, Command command, UUID correlationId) {
        return switch (action.actionType()) {
            case "create_task" -> executeCreateTaskFromAction(action.parameters(), command, correlationId);
            case "complete_task" -> executeCompleteTaskFromAction(action.parameters(), command, correlationId);
            default -> throw new IllegalArgumentException("Unknown action type: " + action.actionType());
        };
    }

    private ActionResult executeCreateTaskFromAction(
            Map<String, Object> params, Command command, UUID correlationId) {
        log.debug("Executing create_task from action: commandId={}", command.getId());

        Household household = householdService.getById(command.getHouseholdId());
        User createdBy = command.getRequester();

        // Resolve assignee
        User assignee = null;
        UUID assigneeId = parseUuid(params.get("assigneeId"));
        if (assigneeId != null) {
            assignee = userService.getById(assigneeId);
        }

        // Resolve zone
        Zone zone = null;
        UUID zoneId = parseUuid(params.get("zoneId"));
        if (zoneId != null) {
            zone = zoneService.getByIdAndHouseholdId(zoneId, household.getId());
        }

        // Parse deadline
        Instant deadline = parseInstant(params.get("deadline"));

        // Create the task
        Task task = taskService.create(TaskService.CreateTaskRequest.builder()
                .household(household)
                .title((String) params.get("title"))
                .description((String) params.get("description"))
                .createdBy(createdBy)
                .assignee(assignee)
                .zone(zone)
                .deadline(deadline)
                .commandId(command.getId())
                .createdVia("command")
                .build());

        // Record activity
        activityRecorder.recordTaskCreated(task, createdBy, command.getId(), correlationId);

        log.info("Task created via command: taskId={}, commandId={}", task.getId(), command.getId());

        return new ActionResult("create_task", task.getId(), task.getAssigneeId());
    }

    private ActionResult executeCompleteTaskFromAction(
            Map<String, Object> params, Command command, UUID correlationId) {
        UUID taskId = parseUuid(params.get("taskId"));
        log.debug("Executing complete_task from action: commandId={}, taskId={}", command.getId(), taskId);

        Task task = taskService.complete(taskId, command.getHouseholdId());

        // Record activity
        activityRecorder.recordTaskCompleted(task, command.getRequester(), command.getId(), correlationId);

        log.info("Task completed via command: taskId={}, commandId={}", task.getId(), command.getId());

        return new ActionResult("complete_task", task.getId(), null);
    }

    private UUID parseUuid(Object value) {
        if (value == null) return null;
        if (value instanceof UUID uuid) return uuid;
        if (value instanceof String s) return UUID.fromString(s);
        return null;
    }

    private Instant parseInstant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant instant) return instant;
        if (value instanceof String s) return Instant.parse(s);
        return null;
    }

    public record CreateTaskResult(UUID taskId, UUID assigneeId) {}

    public record CompleteTaskResult(UUID taskId) {}

    /** Generic result for any action type (Stage 2) */
    public record ActionResult(String actionType, UUID taskId, UUID assigneeId) {}
}
