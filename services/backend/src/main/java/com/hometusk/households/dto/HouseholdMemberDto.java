package com.hometusk.households.dto;

import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Household member information")
public record HouseholdMemberDto(
        @Schema(description = "User ID") UUID userId,
        @Schema(description = "Display name") String displayName,
        @Schema(description = "Email address") String email,
        @Schema(description = "Role in household") String role,
        @Schema(description = "Join timestamp") Instant joinedAt) {

    public static HouseholdMemberDto from(Membership membership) {
        User user = membership.getUser();
        return new HouseholdMemberDto(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                membership.getRole().name().toLowerCase(),
                membership.getJoinedAt());
    }
}
