package com.hometusk.activity.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.activity.domain.ActivityType;
import com.hometusk.activity.domain.TaskActivity;
import com.hometusk.activity.repository.TaskActivityRepository;
import com.hometusk.households.domain.Household;
import com.hometusk.tasks.domain.Task;
import com.hometusk.users.domain.User;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records activity events for audit and history.
 * All activities are linked to correlationId for traceability.
 */
@Service
public class ActivityRecorder {

    private static final Logger log = LoggerFactory.getLogger(ActivityRecorder.class);
    private static final String ENTITY_TYPE_TASK = "task";
    private static final String ENTITY_TYPE_SHOPPING_ITEM = "shopping_item";

    private final TaskActivityRepository taskActivityRepository;
    private final ObjectMapper objectMapper;

    public ActivityRecorder(TaskActivityRepository taskActivityRepository, ObjectMapper objectMapper) {
        this.taskActivityRepository = taskActivityRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TaskActivity recordTaskCreated(Task task, User actor, UUID commandId, UUID correlationId) {
        TaskActivity activity = new TaskActivity(
                task.getHousehold(), commandId, correlationId, ActivityType.TASK_CREATED, ENTITY_TYPE_TASK, task.getId(), actor);

        activity.setMetadata(toJson(Map.of(
                "title", task.getTitle(),
                "assigneeId", task.getAssigneeId() != null ? task.getAssigneeId().toString() : "null",
                "zoneId", task.getZoneId() != null ? task.getZoneId().toString() : "null",
                "deadline", task.getDeadline() != null ? task.getDeadline().toString() : "null")));

        TaskActivity saved = taskActivityRepository.save(activity);
        log.debug("Recorded TASK_CREATED: taskId={}, correlationId={}", task.getId(), correlationId);

        return saved;
    }

    @Transactional
    public TaskActivity recordTaskAssigned(Task task, User actor, User previousAssignee, UUID commandId, UUID correlationId) {
        TaskActivity activity = new TaskActivity(
                task.getHousehold(), commandId, correlationId, ActivityType.TASK_ASSIGNED, ENTITY_TYPE_TASK, task.getId(), actor);

        activity.setChanges(toJson(Map.of(
                "assigneeId",
                Map.of(
                        "old", previousAssignee != null ? previousAssignee.getId().toString() : "null",
                        "new", task.getAssigneeId() != null ? task.getAssigneeId().toString() : "null"))));

        TaskActivity saved = taskActivityRepository.save(activity);
        log.debug("Recorded TASK_ASSIGNED: taskId={}, correlationId={}", task.getId(), correlationId);

        return saved;
    }

    @Transactional
    public TaskActivity recordTaskCompleted(Task task, User actor, UUID commandId, UUID correlationId) {
        TaskActivity activity = new TaskActivity(
                task.getHousehold(), commandId, correlationId, ActivityType.TASK_COMPLETED, ENTITY_TYPE_TASK, task.getId(), actor);

        activity.setChanges(toJson(Map.of("status", Map.of("old", "open", "new", "done"))));

        activity.setMetadata(toJson(Map.of("completedAt", task.getCompletedAt().toString())));

        TaskActivity saved = taskActivityRepository.save(activity);
        log.debug("Recorded TASK_COMPLETED: taskId={}, correlationId={}", task.getId(), correlationId);

        return saved;
    }

    @Transactional
    public TaskActivity recordTaskCancelled(Task task, User actor, UUID commandId, UUID correlationId) {
        TaskActivity activity = new TaskActivity(
                task.getHousehold(), commandId, correlationId, ActivityType.TASK_CANCELLED, ENTITY_TYPE_TASK, task.getId(), actor);

        activity.setChanges(toJson(Map.of("status", Map.of("old", task.getStatus().name(), "new", "cancelled"))));

        return taskActivityRepository.save(activity);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize to JSON", e);
            return "{}";
        }
    }
}
