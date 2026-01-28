package com.hometusk.routines.dto;

import com.hometusk.users.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Summary of a user")
public record UserSummaryDto(
        @Schema(description = "User ID") UUID id, @Schema(description = "Display name") String displayName) {

    public static UserSummaryDto from(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummaryDto(user.getId(), user.getDisplayName());
    }
}
