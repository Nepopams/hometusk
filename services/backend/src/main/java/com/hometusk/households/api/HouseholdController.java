package com.hometusk.households.api;

import com.hometusk.households.domain.Household;
import com.hometusk.households.domain.Zone;
import com.hometusk.households.dto.*;
import com.hometusk.households.service.HouseholdService;
import com.hometusk.households.service.ZoneService;
import com.hometusk.shared.security.CurrentUser;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import com.hometusk.users.domain.User;
import com.hometusk.users.service.MembershipService;
import com.hometusk.users.service.UserResolver;
import com.hometusk.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/households")
@Tag(name = "Households", description = "Household management endpoints")
public class HouseholdController {

    private static final Logger log = LoggerFactory.getLogger(HouseholdController.class);

    private final HouseholdService householdService;
    private final ZoneService zoneService;
    private final MembershipService membershipService;
    private final UserResolver userResolver;
    private final UserService userService;

    public HouseholdController(
            HouseholdService householdService,
            ZoneService zoneService,
            MembershipService membershipService,
            UserResolver userResolver,
            UserService userService) {
        this.householdService = householdService;
        this.zoneService = zoneService;
        this.membershipService = membershipService;
        this.userResolver = userResolver;
        this.userService = userService;
    }

    @PostMapping
    @Operation(
            summary = "Create a new household",
            description =
                    """
            Creates a new household and automatically adds the current user as admin.
            The household name is trimmed and validated (1-80 characters, non-blank).
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Household created"),
        @ApiResponse(responseCode = "400", description = "Invalid request (name validation failed)"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<HouseholdDto> createHousehold(@RequestBody @Valid CreateHouseholdRequest request) {
        log.info("Creating household with name: {}", request.name());

        // Resolve current user
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        User user = userService.getById(currentUser.id());

        // Validate trimmed name is not blank
        String trimmedName = request.trimmedName();
        if (trimmedName == null || trimmedName.isBlank()) {
            throw new IllegalArgumentException("Household name cannot be blank");
        }

        // Create household
        Household household = householdService.create(trimmedName);

        // Add creator as admin
        membershipService.addMember(user, household, MembershipRole.admin);

        log.info("Created household: {} with admin user: {}", household.getId(), user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(HouseholdDto.from(household));
    }

    @GetMapping("/{householdId}/members")
    @Operation(summary = "List household members", description = "Returns all members of the specified household.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of members"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household")
    })
    public ResponseEntity<List<HouseholdMemberDto>> listMembers(@PathVariable UUID householdId) {
        log.debug("Listing members for household: {}", householdId);

        // Verify membership (IDOR prevention)
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        // Get all members
        List<Membership> memberships = membershipService.findByHouseholdId(householdId);
        List<HouseholdMemberDto> members =
                memberships.stream().map(HouseholdMemberDto::from).toList();

        return ResponseEntity.ok(members);
    }

    @GetMapping("/{householdId}/zones")
    @Operation(summary = "List household zones", description = "Returns all zones in the specified household.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of zones"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household")
    })
    public ResponseEntity<List<ZoneDto>> listZones(@PathVariable UUID householdId) {
        log.debug("Listing zones for household: {}", householdId);

        // Verify membership (IDOR prevention)
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        // Get all zones
        List<Zone> zones = zoneService.findByHouseholdId(householdId);
        List<ZoneDto> zoneDtos = zones.stream().map(ZoneDto::from).toList();

        return ResponseEntity.ok(zoneDtos);
    }

    @PostMapping("/{householdId}/zones")
    @Operation(
            summary = "Create a zone",
            description =
                    """
            Creates a new zone in the household.
            If a zone with the same name already exists, returns the existing zone (idempotent).
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Zone created"),
        @ApiResponse(responseCode = "200", description = "Zone already exists (idempotent)"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household")
    })
    public ResponseEntity<ZoneDto> createZone(
            @PathVariable UUID householdId, @RequestBody @Valid CreateZoneRequest request) {
        log.info("Creating zone in household: {}, name: {}", householdId, request.name());

        // Verify membership (IDOR prevention)
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        // Validate trimmed name is not blank
        String trimmedName = request.trimmedName();
        if (trimmedName == null || trimmedName.isBlank()) {
            throw new IllegalArgumentException("Zone name cannot be blank");
        }

        // Get household
        Household household = householdService.getById(householdId);

        // Create zone (idempotent - returns existing if name matches)
        Zone zone = zoneService.create(household, trimmedName);

        log.info("Created/returned zone: {} in household: {}", zone.getId(), householdId);

        // ZoneService.create() is idempotent, but we don't know if it was newly created
        // For simplicity, always return 201 as the zone is available
        return ResponseEntity.status(HttpStatus.CREATED).body(ZoneDto.from(zone));
    }
}
