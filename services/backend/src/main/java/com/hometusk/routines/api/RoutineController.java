package com.hometusk.routines.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.routines.domain.AssignmentPolicy;
import com.hometusk.routines.domain.Routine;
import com.hometusk.routines.domain.RoutineStatus;
import com.hometusk.routines.dto.CreateRoutineRequest;
import com.hometusk.routines.dto.RoutineDto;
import com.hometusk.routines.dto.UpdateRoutineRequest;
import com.hometusk.routines.service.RoutineService;
import com.hometusk.shared.security.CurrentUser;
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
@RequestMapping("/api/v1/households/{householdId}")
@Tag(name = "Routines", description = "Routine CRUD and lifecycle management")
public class RoutineController {

    private static final Logger log = LoggerFactory.getLogger(RoutineController.class);

    private final RoutineService routineService;
    private final MembershipService membershipService;
    private final UserResolver userResolver;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public RoutineController(
            RoutineService routineService,
            MembershipService membershipService,
            UserResolver userResolver,
            UserService userService,
            ObjectMapper objectMapper) {
        this.routineService = routineService;
        this.membershipService = membershipService;
        this.userResolver = userResolver;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/routines")
    @Operation(summary = "List routines in a household")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of routines"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household")
    })
    public ResponseEntity<List<RoutineDto>> listRoutines(
            @PathVariable UUID householdId,
            @RequestParam(required = false) RoutineStatus status,
            @RequestParam(required = false) AssignmentPolicy assignmentPolicy) {
        log.debug("Listing routines for household: {}, status: {}, policy: {}", householdId, status, assignmentPolicy);

        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        List<Routine> routines = routineService.listRoutines(householdId, status, assignmentPolicy);
        List<RoutineDto> dtos =
                routines.stream().map(r -> RoutineDto.from(r, objectMapper)).toList();

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/routines")
    @Operation(summary = "Create a new routine")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Routine created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household")
    })
    public ResponseEntity<RoutineDto> createRoutine(
            @PathVariable UUID householdId, @RequestBody @Valid CreateRoutineRequest request) {
        log.info("Creating routine for household: {}", householdId);

        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        User user = userService.getById(currentUser.id());
        Routine routine = routineService.createRoutine(householdId, request, user);

        return ResponseEntity.status(HttpStatus.CREATED).body(RoutineDto.from(routine, objectMapper));
    }

    @GetMapping("/routines/{routineId}")
    @Operation(summary = "Get a specific routine")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Routine details"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household"),
        @ApiResponse(responseCode = "404", description = "Routine not found")
    })
    public ResponseEntity<RoutineDto> getRoutine(@PathVariable UUID householdId, @PathVariable UUID routineId) {
        log.debug("Getting routine: {}, household: {}", routineId, householdId);

        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        Routine routine = routineService.getRoutine(routineId, householdId);
        return ResponseEntity.ok(RoutineDto.from(routine, objectMapper));
    }

    @PatchMapping("/routines/{routineId}")
    @Operation(summary = "Partially update a routine")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Routine updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household"),
        @ApiResponse(responseCode = "404", description = "Routine not found")
    })
    public ResponseEntity<RoutineDto> updateRoutine(
            @PathVariable UUID householdId,
            @PathVariable UUID routineId,
            @RequestBody @Valid UpdateRoutineRequest request) {
        log.info("Updating routine: {}, household: {}", routineId, householdId);

        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        Routine routine = routineService.updateRoutine(routineId, householdId, request);
        return ResponseEntity.ok(RoutineDto.from(routine, objectMapper));
    }

    @DeleteMapping("/routines/{routineId}")
    @Operation(summary = "Soft delete a routine")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Routine deleted"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household"),
        @ApiResponse(responseCode = "404", description = "Routine not found")
    })
    public ResponseEntity<Void> deleteRoutine(@PathVariable UUID householdId, @PathVariable UUID routineId) {
        log.info("Deleting routine: {}, household: {}", routineId, householdId);

        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        routineService.deleteRoutine(routineId, householdId);
        return ResponseEntity.noContent().build();
    }
}
