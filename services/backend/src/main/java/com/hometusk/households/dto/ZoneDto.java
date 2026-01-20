package com.hometusk.households.dto;

import com.hometusk.households.domain.Zone;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Zone/location within a household")
public record ZoneDto(
        @Schema(description = "Zone ID") UUID id,
        @Schema(description = "Zone name") String name,
        @Schema(description = "Household ID") UUID householdId,
        @Schema(description = "Creation timestamp") Instant createdAt) {

    public static ZoneDto from(Zone zone) {
        return new ZoneDto(zone.getId(), zone.getName(), zone.getHouseholdId(), zone.getCreatedAt());
    }
}
