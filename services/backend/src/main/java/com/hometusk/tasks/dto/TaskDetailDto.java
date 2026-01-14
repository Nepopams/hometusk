package com.hometusk.tasks.dto;

import com.hometusk.households.dto.ZoneDto;
import com.hometusk.shopping.domain.ShoppingItem;
import com.hometusk.shopping.dto.ShoppingItemDto;
import com.hometusk.tasks.domain.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Task detail with linked shopping items")
public record TaskDetailDto(
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
        @Schema(description = "Shopping items linked to this task") List<ShoppingItemDto> linkedShoppingItems) {

    public static TaskDetailDto from(Task task, List<ShoppingItem> linkedItems) {
        List<ShoppingItemDto> itemDtos =
                linkedItems != null ? linkedItems.stream().map(ShoppingItemDto::from).toList() : List.of();

        return new TaskDetailDto(
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
                itemDtos);
    }
}
