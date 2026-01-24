package com.hometusk.gamification.api;

import com.hometusk.gamification.dto.BadgeCatalogResponse;
import com.hometusk.gamification.dto.GamificationProgressResponse;
import com.hometusk.gamification.service.BadgeService;
import com.hometusk.gamification.service.PointsService;
import com.hometusk.shared.security.CurrentUser;
import com.hometusk.users.service.MembershipService;
import com.hometusk.users.service.UserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}/gamification")
@Tag(name = "Gamification", description = "Gamification progress and badges")
public class GamificationController {

    private final PointsService pointsService;
    private final BadgeService badgeService;
    private final MembershipService membershipService;
    private final UserResolver userResolver;

    public GamificationController(
            PointsService pointsService,
            BadgeService badgeService,
            MembershipService membershipService,
            UserResolver userResolver) {
        this.pointsService = pointsService;
        this.badgeService = badgeService;
        this.membershipService = membershipService;
        this.userResolver = userResolver;
    }

    @GetMapping("/progress")
    @Operation(summary = "Get user's gamification progress")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Gamification progress"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household")
    })
    public ResponseEntity<GamificationProgressResponse> getProgress(@PathVariable UUID householdId) {
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        GamificationProgressResponse response = new GamificationProgressResponse(
                currentUser.id(),
                pointsService.getTotalPoints(currentUser.id(), householdId),
                pointsService.getPointsThisWeek(currentUser.id(), householdId),
                badgeService.getEarnedBadges(currentUser.id(), householdId),
                pointsService.getRecentActivity(currentUser.id(), householdId, 10),
                pointsService.getHouseholdTotalTasks(householdId),
                pointsService.getHouseholdTotalPoints(householdId));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/badges")
    @Operation(summary = "Get badge catalog with earned status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Badge catalog"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household")
    })
    public ResponseEntity<BadgeCatalogResponse> getBadges(@PathVariable UUID householdId) {
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        BadgeCatalogResponse response = badgeService.getBadgeCatalog(currentUser.id(), householdId);
        return ResponseEntity.ok(response);
    }
}
