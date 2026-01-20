package com.hometusk.users.api;

import com.hometusk.shared.security.CurrentUser;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.User;
import com.hometusk.users.dto.UserProfileDto;
import com.hometusk.users.service.MembershipService;
import com.hometusk.users.service.UserResolver;
import com.hometusk.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User profile endpoints")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserResolver userResolver;
    private final UserService userService;
    private final MembershipService membershipService;

    public UserController(UserResolver userResolver, UserService userService, MembershipService membershipService) {
        this.userResolver = userResolver;
        this.userService = userService;
        this.membershipService = membershipService;
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get current user profile",
            description =
                    """
            Returns the current user's profile including all household memberships.
            If the user does not exist in the database, creates a new profile from JWT claims.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User profile returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<UserProfileDto> getCurrentUser() {
        log.debug("Getting current user profile");

        // Resolve current user from JWT (creates if not exists)
        CurrentUser currentUser = userResolver.resolveCurrentUser();

        // Get full user entity
        User user = userService.getById(currentUser.id());

        // Get all household memberships
        List<Membership> memberships = membershipService.findByUserId(user.getId());

        // Build response DTO
        UserProfileDto profile = UserProfileDto.from(user, memberships);

        log.info("Returning profile for user: {}", user.getId());
        return ResponseEntity.ok(profile);
    }
}
