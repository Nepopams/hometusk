package com.hometusk.tasks.event;

import com.hometusk.tasks.domain.Task;
import com.hometusk.users.domain.User;
import java.time.Instant;
import java.util.UUID;

public record TaskAssignedEvent(
        UUID taskId,
        String taskTitle,
        UUID householdId,
        String householdName,
        UUID assigneeId,
        UUID actorId,
        String actorName,
        String zoneName,
        Instant deadline,
        Instant assignmentTimestamp,
        UUID correlationId) {

    public static TaskAssignedEvent from(Task task, User actor, UUID correlationId) {
        User assignee = task.getAssignee();
        return new TaskAssignedEvent(
                task.getId(),
                task.getTitle(),
                task.getHouseholdId(),
                task.getHousehold().getName(),
                assignee != null ? assignee.getId() : null,
                actor != null ? actor.getId() : null,
                actor != null ? actor.getDisplayName() : null,
                task.getZone() != null ? task.getZone().getName() : null,
                task.getDeadline(),
                task.getUpdatedAt() != null ? task.getUpdatedAt() : Instant.now(),
                correlationId);
    }
}
