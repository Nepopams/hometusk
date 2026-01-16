package com.hometusk.households.dto;

import com.hometusk.households.domain.HouseholdInvite;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Household invite response")
public record CreateInviteResponse(
        @Schema(description = "Invite token") String inviteToken,
        @Schema(description = "Invite expiration time") Instant expiresAt,
        @Schema(description = "Invite status") String status,
        @Schema(description = "Optional invite link") String inviteLink) {

    public static CreateInviteResponse from(HouseholdInvite invite, String inviteLink) {
        return new CreateInviteResponse(
                invite.getInviteToken(),
                invite.getExpiresAt(),
                invite.getStatus().name().toLowerCase(),
                inviteLink);
    }
}
