package com.hometusk.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Task stats for a household member")
public record MemberStats(
        @Schema(description = "Member ID") UUID memberId,
        @Schema(description = "Member display name") String memberName,
        @Schema(description = "Tasks completed in period") int completedCount,
        @Schema(description = "Currently overdue tasks assigned to member") int overdueCount,
        @Schema(description = "Currently open tasks assigned to member") int openCount) {}
