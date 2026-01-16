package com.hometusk.notifications.api;

import com.hometusk.notifications.dto.NotificationDto;
import com.hometusk.notifications.service.NotificationService;
import com.hometusk.shared.exception.ValidationException;
import com.hometusk.shared.security.CurrentUser;
import com.hometusk.users.service.MembershipService;
import com.hometusk.users.service.UserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Notifications", description = "In-app notification endpoints")
public class NotificationController {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final NotificationService notificationService;
    private final UserResolver userResolver;
    private final MembershipService membershipService;

    public NotificationController(
            NotificationService notificationService,
            UserResolver userResolver,
            MembershipService membershipService) {
        this.notificationService = notificationService;
        this.userResolver = userResolver;
        this.membershipService = membershipService;
    }

    @GetMapping("/households/{householdId}/notifications")
    @Operation(
            summary = "List notifications for a household",
            description = "Returns notifications for the current user within the household.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of notifications"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household")
    })
    public ResponseEntity<List<NotificationDto>> listNotifications(
            @PathVariable UUID householdId,
            @RequestParam(required = false)
                    @Parameter(description = "RFC3339 timestamp to filter notifications since this time")
                    String since,
            @RequestParam(required = false)
                    @Parameter(description = "Max number of results (default 50, max 200)")
                    Integer limit) {
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        Instant sinceInstant = parseSince(since);
        int resolvedLimit = resolveLimit(limit);

        List<NotificationDto> notifications =
                notificationService.listNotifications(householdId, currentUser.id(), sinceInstant, resolvedLimit);
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/notifications/{notificationId}/read")
    @Operation(summary = "Mark notification as read")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notification updated"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<NotificationDto> markRead(@PathVariable UUID notificationId) {
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        NotificationDto notification = notificationService.markRead(notificationId, currentUser.id());
        return ResponseEntity.ok(notification);
    }

    private Instant parseSince(String since) {
        if (since == null || since.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(since);
        } catch (DateTimeParseException e) {
            throw new ValidationException("$.since", "INVALID_TIMESTAMP", "since must be an RFC3339 timestamp");
        }
    }

    private int resolveLimit(Integer limit) {
        int resolved = limit != null ? limit : DEFAULT_LIMIT;
        if (resolved < 1 || resolved > MAX_LIMIT) {
            throw new ValidationException("$.limit", "INVALID_RANGE", "limit must be between 1 and 200");
        }
        return resolved;
    }
}
