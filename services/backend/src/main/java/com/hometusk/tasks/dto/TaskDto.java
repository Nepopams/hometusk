package com.hometusk.tasks.dto;

import com.hometusk.households.dto.ZoneDto;
import com.hometusk.tasks.domain.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Task entity")
public record TaskDto(
        @Schema(description = "Task ID") UUID id,
        @Schema(description = "Household ID") UUID householdId,
        @Schema(description = "Task title") String title,
        @Schema(description = "Task description") String description,
        @Schema(description = "Task status") String status,
        @Schema(description = "Assigned user") UserSummaryDto assignee,
        @Schema(description = "Task zone/location") ZoneDto zone,
        @Schema(description = "Deadline") Instant deadline,
        @Schema(description = "Created by user") UserSummaryDto createdBy,
        @Schema(description = "Command ID that created this task") UUID commandId,
        @Schema(description = "How the task was created") String createdVia,
        @Schema(description = "Creation timestamp") Instant createdAt,
        @Schema(description = "Last update timestamp") Instant updatedAt,
        @Schema(description = "Completion timestamp") Instant completedAt,
        @Schema(description = "Source routine if auto-generated") RoutineSummaryDto routine) {

    public static TaskDto from(Task task) {
        return new TaskDto(
                task.getId(),
                task.getHouseholdId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name().toLowerCase(),
                UserSummaryDto.from(task.getAssignee()),
                task.getZone() != null ? ZoneDto.from(task.getZone()) : null,
                task.getDeadline(),
                UserSummaryDto.from(task.getCreatedBy()),
                task.getCommandId(),
                task.getCreatedVia(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getCompletedAt(),
                RoutineSummaryDto.from(task.getRoutine()));
    }
}
