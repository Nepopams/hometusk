package com.hometusk.shopping.api;

import com.hometusk.shared.exception.ValidationException;
import com.hometusk.shared.security.CurrentUser;
import com.hometusk.shopping.domain.ShoppingRun;
import com.hometusk.shopping.domain.ShoppingRunItem;
import com.hometusk.shopping.domain.ShoppingRunStatus;
import com.hometusk.shopping.dto.CloseShoppingRunRequest;
import com.hometusk.shopping.dto.CreateShoppingRunRequest;
import com.hometusk.shopping.dto.ShoppingRunDto;
import com.hometusk.shopping.dto.ShoppingRunItemDto;
import com.hometusk.shopping.dto.ShoppingRunSummaryDto;
import com.hometusk.shopping.dto.UpdateRunItemRequest;
import com.hometusk.shopping.service.ShoppingRunService;
import com.hometusk.users.service.MembershipService;
import com.hometusk.users.service.UserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/v1/households/{householdId}/shopping-runs")
@Tag(name = "ShoppingRuns", description = "Shopping run management")
public class ShoppingRunController {

    private static final Logger log = LoggerFactory.getLogger(ShoppingRunController.class);

    private final ShoppingRunService runService;
    private final MembershipService membershipService;
    private final UserResolver userResolver;

    public ShoppingRunController(
            ShoppingRunService runService, MembershipService membershipService, UserResolver userResolver) {
        this.runService = runService;
        this.membershipService = membershipService;
        this.userResolver = userResolver;
    }

    @PostMapping
    @Operation(summary = "Create a shopping run from a list")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Run created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household"),
        @ApiResponse(responseCode = "404", description = "Shopping list not found")
    })
    public ResponseEntity<ShoppingRunDto> createRun(
            @PathVariable UUID householdId, @Valid @RequestBody CreateShoppingRunRequest request) {
        log.info("Creating shopping run: householdId={}, listId={}", householdId, request.listId());

        CurrentUser user = userResolver.resolveCurrentUser();
        membershipService.requireMembership(user.id(), householdId);

        ShoppingRun run = runService.createRun(householdId, request.listId(), user.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(ShoppingRunDto.from(run));
    }

    @GetMapping
    @Operation(summary = "List shopping runs")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of runs"),
        @ApiResponse(responseCode = "400", description = "Invalid status"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household")
    })
    public ResponseEntity<List<ShoppingRunSummaryDto>> listRuns(
            @PathVariable UUID householdId,
            @RequestParam(required = false, defaultValue = "ACTIVE")
                    @Parameter(description = "Run status filter or 'all'")
                    String status,
            @RequestParam(required = false, defaultValue = "20") @Parameter(description = "Max results") int limit) {
        log.debug("Listing shopping runs: householdId={}, status={}, limit={}", householdId, status, limit);

        CurrentUser user = userResolver.resolveCurrentUser();
        membershipService.requireMembership(user.id(), householdId);

        ShoppingRunStatus statusEnum = parseStatus(status);
        int safeLimit = Math.min(Math.max(limit, 1), 100);

        List<ShoppingRun> runs = runService.listRuns(householdId, statusEnum, safeLimit);
        List<ShoppingRunSummaryDto> dtos =
                runs.stream().map(ShoppingRunSummaryDto::from).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{runId}")
    @Operation(summary = "Get shopping run details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Run details"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household"),
        @ApiResponse(responseCode = "404", description = "Run not found")
    })
    public ResponseEntity<ShoppingRunDto> getRun(@PathVariable UUID householdId, @PathVariable UUID runId) {
        log.debug("Getting shopping run: householdId={}, runId={}", householdId, runId);

        CurrentUser user = userResolver.resolveCurrentUser();
        membershipService.requireMembership(user.id(), householdId);

        ShoppingRun run = runService.getRun(householdId, runId);
        return ResponseEntity.ok(ShoppingRunDto.from(run));
    }

    @PostMapping("/{runId}/close")
    @Operation(summary = "Close a shopping run")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Run closed"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household"),
        @ApiResponse(responseCode = "404", description = "Run not found"),
        @ApiResponse(responseCode = "409", description = "Run already closed")
    })
    public ResponseEntity<ShoppingRunDto> closeRun(
            @PathVariable UUID householdId,
            @PathVariable UUID runId,
            @Valid @RequestBody CloseShoppingRunRequest request) {
        log.info("Closing shopping run: householdId={}, runId={}, status={}", householdId, runId, request.status());

        CurrentUser user = userResolver.resolveCurrentUser();
        membershipService.requireMembership(user.id(), householdId);

        ShoppingRun run = runService.closeRun(householdId, runId, request.status());
        return ResponseEntity.ok(ShoppingRunDto.from(run));
    }

    @PatchMapping("/{runId}/items/{itemId}")
    @Operation(summary = "Update item status in a run")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household"),
        @ApiResponse(responseCode = "404", description = "Run item not found"),
        @ApiResponse(responseCode = "409", description = "Run closed")
    })
    public ResponseEntity<ShoppingRunItemDto> updateItem(
            @PathVariable UUID householdId,
            @PathVariable UUID runId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateRunItemRequest request) {
        log.info(
                "Updating run item: householdId={}, runId={}, itemId={}, purchased={}, syncToList={}",
                householdId,
                runId,
                itemId,
                request.purchased(),
                request.shouldSyncToList());

        CurrentUser user = userResolver.resolveCurrentUser();
        membershipService.requireMembership(user.id(), householdId);

        ShoppingRunItem item =
                runService.updateItem(householdId, runId, itemId, request.purchased(), request.shouldSyncToList());
        return ResponseEntity.ok(ShoppingRunItemDto.from(item));
    }

    private ShoppingRunStatus parseStatus(String status) {
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
            return null;
        }
        try {
            return ShoppingRunStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("$.status", "INVALID_STATUS", "Invalid status: " + status);
        }
    }
}
