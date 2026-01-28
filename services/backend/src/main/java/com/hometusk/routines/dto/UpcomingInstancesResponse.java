package com.hometusk.routines.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Upcoming instances preview response")
public record UpcomingInstancesResponse(
        @Schema(description = "Routine ID") UUID routineId,
        @Schema(description = "Routine title") String routineTitle,
        @Schema(description = "Upcoming instances") List<UpcomingInstanceDto> instances) {}
