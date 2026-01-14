package com.hometusk.users.dto;

import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "User profile with household memberships")
public record UserProfileDto(
        @Schema(description = "User ID") UUID id,
        @Schema(description = "External ID (Keycloak subject)") String externalId,
        @Schema(description = "Email address") String email,
        @Schema(description = "Display name") String displayName,
        @Schema(description = "Avatar URL") String avatarUrl,
        @Schema(description = "Households the user belongs to") List<HouseholdSummaryDto> households,
        @Schema(description = "Account creation timestamp") Instant createdAt) {

    public static UserProfileDto from(User user, List<Membership> memberships) {
        List<HouseholdSummaryDto> householdSummaries =
                memberships.stream().map(HouseholdSummaryDto::from).toList();

        return new UserProfileDto(
                user.getId(),
                user.getExternalId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                householdSummaries,
                user.getCreatedAt());
    }
}
