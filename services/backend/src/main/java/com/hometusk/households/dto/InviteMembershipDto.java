package com.hometusk.households.dto;

import com.hometusk.users.domain.Membership;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Membership created or returned after invite acceptance")
public record InviteMembershipDto(
        @Schema(description = "Membership ID") UUID id,
        @Schema(description = "Role in household") String role,
        @Schema(description = "Join timestamp") Instant joinedAt) {

    public static InviteMembershipDto from(Membership membership) {
        return new InviteMembershipDto(
                membership.getId(), membership.getRole().name().toLowerCase(), membership.getJoinedAt());
    }
}
