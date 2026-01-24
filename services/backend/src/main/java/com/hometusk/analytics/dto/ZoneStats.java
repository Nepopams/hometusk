package com.hometusk.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Task stats for a zone")
public record ZoneStats(
        @Schema(description = "Zone ID") UUID zoneId,
        @Schema(description = "Zone name") String zoneName,
        @Schema(description = "Tasks completed in period") int completedCount,
        @Schema(description = "Currently overdue tasks in zone") int overdueCount) {}
