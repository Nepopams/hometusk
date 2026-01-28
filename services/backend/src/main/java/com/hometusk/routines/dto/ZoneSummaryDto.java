package com.hometusk.routines.dto;

import com.hometusk.households.domain.Zone;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Summary of a zone")
public record ZoneSummaryDto(@Schema(description = "Zone ID") UUID id, @Schema(description = "Zone name") String name) {

    public static ZoneSummaryDto from(Zone zone) {
        if (zone == null) {
            return null;
        }
        return new ZoneSummaryDto(zone.getId(), zone.getName());
    }
}
