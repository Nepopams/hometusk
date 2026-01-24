package com.hometusk.gamification.dto;

import com.hometusk.gamification.domain.PointsLedger;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Points ledger entry")
public record PointsEntryDto(
        @Schema(description = "Entry ID") UUID id,
        @Schema(description = "Related task ID") UUID taskId,
        @Schema(description = "Points amount") int points,
        @Schema(description = "Reason for points") String reason,
        @Schema(description = "Creation timestamp") Instant createdAt) {

    public static PointsEntryDto from(PointsLedger entry) {
        return new PointsEntryDto(
                entry.getId(),
                entry.getTaskId(),
                entry.getPoints(),
                entry.getReason().name().toLowerCase(),
                entry.getCreatedAt());
    }
}
