package com.hometusk.commands.api;

import com.hometusk.commands.dto.CommandDegradedResponse;
import com.hometusk.commands.dto.CommandNeedsInputResponse;
import com.hometusk.commands.dto.CommandRequest;
import com.hometusk.commands.dto.CommandResponseBase;
import com.hometusk.commands.service.CommandService;
import com.hometusk.shared.logging.MdcKeys;
import com.hometusk.users.domain.User;
import com.hometusk.users.service.MembershipService;
import com.hometusk.users.service.UserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/commands")
@Tag(name = "Commands", description = "Command execution endpoints")
public class CommandController {

    private static final Logger log = LoggerFactory.getLogger(CommandController.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    private final CommandService commandService;
    private final UserResolver userResolver;
    private final MembershipService membershipService;

    public CommandController(
            CommandService commandService, UserResolver userResolver, MembershipService membershipService) {
        this.commandService = commandService;
        this.userResolver = userResolver;
        this.membershipService = membershipService;
    }

    @PostMapping
    @Operation(
            summary = "Execute a command",
            description =
                    """
            Processes a command and returns the execution result.
            Commands are first-class entities with their own lifecycle.

            **Supported command types:**
            - `create_task` — Create a new task
            - `complete_task` — Mark a task as done
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Command executed successfully"),
        @ApiResponse(responseCode = "207", description = "Command executed with degraded mode"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household")
    })
    public ResponseEntity<CommandResponseBase> executeCommand(
            @RequestBody @Valid CommandRequest request,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false)
                    @Parameter(description = "Client-provided correlation ID")
                    String correlationIdHeader) {

        // Generate or use provided correlation ID
        UUID correlationId =
                correlationIdHeader != null ? parseUuidOrGenerate(correlationIdHeader) : UUID.randomUUID();

        // Set correlation ID in MDC for logging
        MDC.put(MdcKeys.CORRELATION_ID, correlationId.toString());

        try {
            log.info("Received command: type={}, householdId={}", request.type(), request.householdId());

            // Resolve current user from JWT
            User user = userResolver.resolveCurrentUser();

            // Check membership (IDOR prevention - per security-reviewer)
            membershipService.requireMembership(user.getId(), request.householdId());

            // Execute command
            CommandResponseBase response = commandService.execute(request, user, correlationId);

            // Return with correlation ID header
            HttpHeaders headers = new HttpHeaders();
            headers.set(CORRELATION_ID_HEADER, correlationId.toString());

            // Determine HTTP status based on response type
            if (response instanceof CommandDegradedResponse) {
                return ResponseEntity.status(207).headers(headers).body(response);
            } else if (response instanceof CommandNeedsInputResponse) {
                return ResponseEntity.ok().headers(headers).body(response);
            }
            return ResponseEntity.ok().headers(headers).body(response);

        } finally {
            MDC.remove(MdcKeys.CORRELATION_ID);
        }
    }

    private UUID parseUuidOrGenerate(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            log.debug("Invalid correlation ID format, generating new one: {}", value);
            return UUID.randomUUID();
        }
    }

    // Error response schema for OpenAPI documentation
    @Schema(description = "Error response")
    public record ErrorResponse(
            @Schema(description = "Correlation ID") UUID correlationId,
            @Schema(description = "Error code") String errorCode,
            @Schema(description = "Error message") String message) {}
}
