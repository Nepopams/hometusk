package com.hometusk.users.dto;

import com.hometusk.users.domain.Membership;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Summary of a household membership")
public record HouseholdSummaryDto(
        @Schema(description = "Household ID") UUID id,
        @Schema(description = "Household name") String name,
        @Schema(description = "User's role in this household") String role) {

    public static HouseholdSummaryDto from(Membership membership) {
        return new HouseholdSummaryDto(
                membership.getHousehold().getId(),
                membership.getHousehold().getName(),
                membership.getRole().name().toLowerCase());
    }
}
