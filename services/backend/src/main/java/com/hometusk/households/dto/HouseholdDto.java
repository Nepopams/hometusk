package com.hometusk.households.dto;

import com.hometusk.households.domain.Household;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Household entity")
public record HouseholdDto(
        @Schema(description = "Household ID") UUID id,
        @Schema(description = "Household name") String name,
        @Schema(description = "Creation timestamp") Instant createdAt) {

    public static HouseholdDto from(Household household) {
        return new HouseholdDto(household.getId(), household.getName(), household.getCreatedAt());
    }
}
