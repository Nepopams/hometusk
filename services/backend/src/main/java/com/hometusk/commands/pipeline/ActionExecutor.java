package com.hometusk.commands.pipeline;

import com.hometusk.activity.service.ActivityRecorder;
import com.hometusk.commands.domain.Command;
import com.hometusk.households.domain.Household;
import com.hometusk.households.domain.Zone;
import com.hometusk.households.service.HouseholdService;
import com.hometusk.households.service.ZoneService;
import com.hometusk.tasks.domain.Task;
import com.hometusk.tasks.service.TaskService;
import com.hometusk.users.domain.User;
import com.hometusk.users.service.UserService;
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

    public record CreateTaskResult(UUID taskId, UUID assigneeId) {}

    public record CompleteTaskResult(UUID taskId) {}
}
