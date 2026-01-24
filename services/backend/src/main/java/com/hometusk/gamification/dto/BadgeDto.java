package com.hometusk.gamification.dto;

import com.hometusk.gamification.domain.Badge;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Badge")
public record BadgeDto(
        @Schema(description = "Badge code") String code,
        @Schema(description = "Badge name") String name,
        @Schema(description = "Badge description") String description,
        @Schema(description = "Badge criteria") String criteria,
        @Schema(description = "Badge icon name") String iconName,
        @Schema(description = "Whether current user has earned this badge") boolean earned,
        @Schema(description = "When the badge was earned") Instant earnedAt) {

    public static BadgeDto from(Badge badge, Instant earnedAt) {
        boolean earned = earnedAt != null;
        return new BadgeDto(
                badge.getCode(),
                badge.getName(),
                badge.getDescription(),
                badge.getCriteria(),
                badge.getIconName(),
                earned,
                earnedAt);
    }
}
