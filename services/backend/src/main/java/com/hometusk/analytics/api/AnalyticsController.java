package com.hometusk.analytics.api;

import com.hometusk.analytics.dto.AnalyticsSummaryResponse;
import com.hometusk.analytics.service.AnalyticsService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}/analytics")
@Tag(name = "Analytics", description = "Household analytics endpoints")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final MembershipService membershipService;
    private final UserResolver userResolver;

    public AnalyticsController(
            AnalyticsService analyticsService, MembershipService membershipService, UserResolver userResolver) {
        this.analyticsService = analyticsService;
        this.membershipService = membershipService;
        this.userResolver = userResolver;
    }

    @GetMapping
    @Operation(summary = "Get household analytics summary")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analytics summary"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household")
    })
    public ResponseEntity<AnalyticsSummaryResponse> getAnalytics(
            @PathVariable UUID householdId, @RequestParam(defaultValue = "7d") String period) {
        CurrentUser user = userResolver.resolveCurrentUser();
        membershipService.requireMembership(user.id(), householdId);

        AnalyticsSummaryResponse response = analyticsService.getAnalytics(householdId, period);
        return ResponseEntity.ok(response);
    }
}
