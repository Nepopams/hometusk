package com.hometusk.routines.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.routines.domain.AssignmentPolicy;
import com.hometusk.routines.domain.RecurrenceRule;
import com.hometusk.routines.domain.Routine;
import com.hometusk.routines.domain.RoutineStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Routine")
public record RoutineDto(
        @Schema(description = "Routine ID") UUID id,
        @Schema(description = "Household ID") UUID householdId,
        @Schema(description = "Title") String title,
        @Schema(description = "Description") String description,
        @Schema(description = "Zone") ZoneSummaryDto zone,
        @Schema(description = "Recurrence rule") RecurrenceRule recurrenceRule,
        @Schema(description = "Assignment policy") AssignmentPolicy assignmentPolicy,
        @Schema(description = "Fixed assignee") UserSummaryDto fixedAssignee,
        @Schema(description = "Status") RoutineStatus status,
        @Schema(description = "Generation window (days)") int generationWindowDays,
        @Schema(description = "Created by") UserSummaryDto createdBy,
        @Schema(description = "Created at") Instant createdAt,
        @Schema(description = "Updated at") Instant updatedAt,
        @Schema(description = "Paused at") Instant pausedAt) {

    public static RoutineDto from(Routine routine, ObjectMapper objectMapper) {
        RecurrenceRule rule = null;
        if (routine.getRecurrenceRuleJson() != null) {
            try {
                rule = objectMapper.readValue(routine.getRecurrenceRuleJson(), RecurrenceRule.class);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Invalid recurrence rule JSON", e);
            }
        }

        return new RoutineDto(
                routine.getId(),
                routine.getHouseholdId(),
                routine.getTitle(),
                routine.getDescription(),
                ZoneSummaryDto.from(routine.getZone()),
                rule,
                routine.getAssignmentPolicy(),
                UserSummaryDto.from(routine.getFixedAssignee()),
                routine.getStatus(),
                routine.getGenerationWindowDays(),
                UserSummaryDto.from(routine.getCreatedBy()),
                routine.getCreatedAt(),
                routine.getUpdatedAt(),
                routine.getPausedAt());
    }
}
