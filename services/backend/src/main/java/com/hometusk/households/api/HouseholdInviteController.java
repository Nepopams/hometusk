package com.hometusk.households.api;

import com.hometusk.households.dto.AcceptInviteRequest;
import com.hometusk.households.dto.AcceptInviteResponse;
import com.hometusk.households.dto.CreateInviteResponse;
import com.hometusk.households.service.InviteService;
import com.hometusk.shared.logging.MdcKeys;
import com.hometusk.shared.security.CurrentUser;
import com.hometusk.users.service.UserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Invites", description = "Household invite endpoints")
public class HouseholdInviteController {

    private final InviteService inviteService;
    private final UserResolver userResolver;

    public HouseholdInviteController(InviteService inviteService, UserResolver userResolver) {
        this.inviteService = inviteService;
        this.userResolver = userResolver;
    }

    @PostMapping("/households/{householdId}/invites")
    @Operation(
            summary = "Create a household invite",
            description = "Creates a single-use invite token for the household.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Invite created"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household")
    })
    public ResponseEntity<CreateInviteResponse> createInvite(@PathVariable UUID householdId) {
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        CreateInviteResponse response = inviteService.createInvite(householdId, currentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/invites/accept")
    @Operation(
            summary = "Accept a household invite",
            description = "Accepts an invite token and joins the household.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invite accepted or no-op for existing member"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Invalid invite token"),
        @ApiResponse(responseCode = "410", description = "Invite expired, redeemed, or revoked")
    })
    public ResponseEntity<AcceptInviteResponse> acceptInvite(@RequestBody @Valid AcceptInviteRequest request) {
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        AcceptInviteResponse response =
                inviteService.acceptInvite(request.inviteToken(), currentUser.id(), getCorrelationId());
        return ResponseEntity.ok(response);
    }

    private UUID getCorrelationId() {
        String correlationIdStr = MDC.get(MdcKeys.CORRELATION_ID);
        if (correlationIdStr != null) {
            try {
                return UUID.fromString(correlationIdStr);
            } catch (IllegalArgumentException e) {
                // Generate new if invalid
            }
        }
        return UUID.randomUUID();
    }
}
