package com.hometusk.routines.dto;

import com.hometusk.routines.domain.AssignmentPolicy;
import com.hometusk.routines.domain.RecurrenceRule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(description = "Request to update a routine")
public record UpdateRoutineRequest(
        @Schema(description = "Routine title", maxLength = 255)
                @Size(max = 255, message = "Title must be at most 255 characters")
                String title,
        @Schema(description = "Routine description", maxLength = 2000)
                @Size(max = 2000, message = "Description must be at most 2000 characters")
                String description,
        @Schema(description = "Zone ID", format = "uuid") UUID zoneId,
        @Schema(description = "Recurrence rule") RecurrenceRule recurrenceRule,
        @Schema(description = "Assignment policy") AssignmentPolicy assignmentPolicy,
        @Schema(description = "Fixed assignee ID", format = "uuid") UUID fixedAssigneeId,
        @Schema(description = "Generation window in days", minimum = "1", maximum = "30")
                @Min(value = 1, message = "Generation window must be between 1 and 30")
                @Max(value = 30, message = "Generation window must be between 1 and 30")
                Integer generationWindowDays) {}
