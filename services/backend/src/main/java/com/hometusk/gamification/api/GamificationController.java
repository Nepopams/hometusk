package com.hometusk.gamification.api;

import com.hometusk.gamification.domain.GamificationSettings;
import com.hometusk.gamification.domain.StreakState;
import com.hometusk.gamification.dto.BadgeCatalogResponse;
import com.hometusk.gamification.dto.GamificationProgressResponse;
import com.hometusk.gamification.dto.GamificationSettingsDto;
import com.hometusk.gamification.service.BadgeService;
import com.hometusk.gamification.service.GamificationSettingsService;
import com.hometusk.gamification.service.PointsService;
import com.hometusk.gamification.service.StreakService;
import com.hometusk.households.domain.Household;
import com.hometusk.households.repository.HouseholdRepository;
import com.hometusk.shared.security.CurrentUser;
import com.hometusk.users.domain.User;
import com.hometusk.users.repository.UserRepository;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}/gamification")
@Tag(name = "Gamification", description = "Gamification progress and badges")
public class GamificationController {

    private final PointsService pointsService;
    private final BadgeService badgeService;
    private final GamificationSettingsService settingsService;
    private final StreakService streakService;
    private final MembershipService membershipService;
    private final UserResolver userResolver;
    private final UserRepository userRepository;
    private final HouseholdRepository householdRepository;

    public GamificationController(
            PointsService pointsService,
            BadgeService badgeService,
            GamificationSettingsService settingsService,
            StreakService streakService,
            MembershipService membershipService,
            UserResolver userResolver,
            UserRepository userRepository,
            HouseholdRepository householdRepository) {
        this.pointsService = pointsService;
        this.badgeService = badgeService;
        this.settingsService = settingsService;
        this.streakService = streakService;
        this.membershipService = membershipService;
        this.userResolver = userResolver;
        this.userRepository = userRepository;
        this.householdRepository = householdRepository;
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

        User user = userRepository
                .findById(currentUser.id())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        Household household = householdRepository
                .findById(householdId)
                .orElseThrow(() -> new IllegalStateException("Household not found"));
        StreakState streakState = streakService.getStreakState(user, household);

        GamificationProgressResponse response = new GamificationProgressResponse(
                currentUser.id(),
                pointsService.getTotalPoints(currentUser.id(), householdId),
                pointsService.getPointsThisWeek(currentUser.id(), householdId),
                badgeService.getEarnedBadges(currentUser.id(), householdId),
                pointsService.getRecentActivity(currentUser.id(), householdId, 10),
                pointsService.getHouseholdTotalTasks(householdId),
                pointsService.getHouseholdTotalPoints(householdId),
                streakState.getCurrentStreak(),
                streakState.getBestStreak(),
                !streakState.isGraceUsedToday());

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

    @GetMapping("/settings")
    @Operation(summary = "Get user's gamification settings")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Settings"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household")
    })
    public ResponseEntity<GamificationSettingsDto> getSettings(@PathVariable UUID householdId) {
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        User user = userRepository
                .findById(currentUser.id())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        Household household = householdRepository
                .findById(householdId)
                .orElseThrow(() -> new IllegalStateException("Household not found"));

        GamificationSettings settings = settingsService.getOrCreate(user, household);
        return ResponseEntity.ok(GamificationSettingsDto.from(settings));
    }

    @PutMapping("/settings")
    @Operation(summary = "Update user's gamification settings")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Settings updated"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household")
    })
    public ResponseEntity<GamificationSettingsDto> updateSettings(
            @PathVariable UUID householdId, @RequestBody GamificationSettingsDto request) {
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        User user = userRepository
                .findById(currentUser.id())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        Household household = householdRepository
                .findById(householdId)
                .orElseThrow(() -> new IllegalStateException("Household not found"));

        GamificationSettings settings = settingsService.update(user, household, request);
        return ResponseEntity.ok(GamificationSettingsDto.from(settings));
    }
}
