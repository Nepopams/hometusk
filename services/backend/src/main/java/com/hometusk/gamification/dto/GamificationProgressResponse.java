package com.hometusk.gamification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Gamification progress summary")
public record GamificationProgressResponse(
        @Schema(description = "User ID") UUID userId,
        @Schema(description = "Total points earned by user") int totalPoints,
        @Schema(description = "Points earned in last 7 days") int pointsThisWeek,
        @Schema(description = "Badges earned by user") List<BadgeDto> earnedBadges,
        @Schema(description = "Recent points activity") List<PointsEntryDto> recentActivity,
        @Schema(description = "Household total completed tasks") int householdTotalTasks,
        @Schema(description = "Household total points") int householdTotalPoints,
        @Schema(description = "Current streak days") int currentStreak,
        @Schema(description = "Best streak days") int bestStreak,
        @Schema(description = "Grace day available") boolean graceAvailable) {}
