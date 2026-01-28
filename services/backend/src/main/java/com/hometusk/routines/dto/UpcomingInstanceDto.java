package com.hometusk.routines.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Upcoming routine instance")
public record UpcomingInstanceDto(
        @Schema(description = "Scheduled date") LocalDate scheduledDate,
        @Schema(description = "Projected assignee") UserSummaryDto projectedAssignee) {}
