package com.hometusk.gamification.dto;

import com.hometusk.gamification.domain.GamificationSettings;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Gamification privacy settings")
public record GamificationSettingsDto(
        @Schema(description = "Show progress to other household members") boolean showProgressToOthers,
        @Schema(description = "Enable gamification (points, badges, streaks)") boolean gamificationEnabled,
        @Schema(description = "Show streak to other household members") boolean streakVisible) {

    public static GamificationSettingsDto from(GamificationSettings settings) {
        return new GamificationSettingsDto(
                settings.isShowProgressToOthers(), settings.isGamificationEnabled(), settings.isStreakVisible());
    }

    public static GamificationSettingsDto defaults() {
        return new GamificationSettingsDto(true, true, true);
    }
}
