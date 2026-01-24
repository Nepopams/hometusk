package com.hometusk.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Overdue task summary")
public record OverdueTask(
        @Schema(description = "Task ID") UUID taskId,
        @Schema(description = "Task title") String title,
        @Schema(description = "Assignee display name") String assigneeName,
        @Schema(description = "Days overdue (minimum 1)") int daysOverdue) {}
