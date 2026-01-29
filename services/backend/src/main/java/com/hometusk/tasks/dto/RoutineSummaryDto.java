package com.hometusk.tasks.dto;

import com.hometusk.routines.domain.Routine;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Summary of a routine")
public record RoutineSummaryDto(
        @Schema(description = "Routine ID") UUID id,
        @Schema(description = "Routine title") String title,
        @Schema(description = "Routine status") String status) {

    public static RoutineSummaryDto from(Routine routine) {
        if (routine == null) {
            return null;
        }
        return new RoutineSummaryDto(
                routine.getId(), routine.getTitle(), routine.getStatus().name());
    }
}
