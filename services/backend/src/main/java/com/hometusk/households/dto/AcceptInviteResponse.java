package com.hometusk.households.dto;

import com.hometusk.users.domain.Membership;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Invite acceptance response")
public record AcceptInviteResponse(
        @Schema(description = "Membership info") InviteMembershipDto membership,
        @Schema(description = "Household summary") HouseholdDto household) {

    public static AcceptInviteResponse from(Membership membership) {
        return new AcceptInviteResponse(
                InviteMembershipDto.from(membership), HouseholdDto.from(membership.getHousehold()));
    }
}
