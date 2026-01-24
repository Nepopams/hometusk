package com.hometusk.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Household analytics summary")
public record AnalyticsSummaryResponse(
        @Schema(description = "Household ID") UUID householdId,
        @Schema(description = "Analytics period") String period,
        @Schema(description = "Period start timestamp") Instant periodStart,
        @Schema(description = "Period end timestamp") Instant periodEnd,
        @Schema(description = "Stats per household member") List<MemberStats> perMember,
        @Schema(description = "Stats per zone") List<ZoneStats> perZone,
        @Schema(description = "Balance score and explanation") FairnessInfo fairness,
        @Schema(description = "Top overdue tasks (optional)") List<OverdueTask> overdueTop) {}
