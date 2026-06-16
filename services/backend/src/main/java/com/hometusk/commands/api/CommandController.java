package com.hometusk.commands.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.commands.dto.CommandConfirmationCancelRequest;
import com.hometusk.commands.dto.CommandRequest;
import com.hometusk.commands.dto.CommandResponseBase;
import com.hometusk.commands.dto.ContinueCommandRequest;
import com.hometusk.commands.idempotency.CommandIdempotencyService;
import com.hometusk.commands.idempotency.CommandIdempotencyService.IdempotencySession;
import com.hometusk.commands.idempotency.CommandIdempotencyService.StoredResponse;
import com.hometusk.commands.service.CommandService;
import com.hometusk.shared.exception.BusinessException;
import com.hometusk.shared.exception.ErrorCode;
import com.hometusk.shared.exception.GlobalExceptionHandler;
import com.hometusk.shared.exception.ValidationException;
import com.hometusk.shared.logging.MdcKeys;
import com.hometusk.shared.security.CurrentUser;
import com.hometusk.users.domain.User;
import com.hometusk.users.service.MembershipService;
import com.hometusk.users.service.UserResolver;
import com.hometusk.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/commands")
@Tag(name = "Commands", description = "Command execution endpoints")
public class CommandController {

    private static final Logger log = LoggerFactory.getLogger(CommandController.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final CommandService commandService;
    private final CommandIdempotencyService idempotencyService;
    private final UserResolver userResolver;
    private final MembershipService membershipService;
    private final ObjectMapper objectMapper;
    private final UserService userService;

    public CommandController(
            CommandService commandService,
            CommandIdempotencyService idempotencyService,
            UserResolver userResolver,
            MembershipService membershipService,
            ObjectMapper objectMapper,
            UserService userService) {
        this.commandService = commandService;
        this.idempotencyService = idempotencyService;
        this.userResolver = userResolver;
        this.membershipService = membershipService;
        this.objectMapper = objectMapper;
        this.userService = userService;
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

            **Response status values:**
            - `executed` — Command executed successfully
            - `needs_input` — Guardrails require user clarification
            - `rejected` — Command rejected by AI or guardrails
            - `executed_degraded` — Executed with fallback due to AI unavailability
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Command processed (status in body)"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household")
    })
    public ResponseEntity<Object> executeCommand(
            @RequestBody @Valid CommandRequest request,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false)
                    @Parameter(description = "Client-provided correlation ID")
                    String correlationIdHeader,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false)
                    @Parameter(description = "Idempotency key for safe retries")
                    List<String> idempotencyKeys) {

        // Generate or use provided correlation ID
        UUID correlationId = correlationIdHeader != null ? parseUuidOrGenerate(correlationIdHeader) : UUID.randomUUID();

        // Set correlation ID in MDC for logging
        MDC.put(MdcKeys.CORRELATION_ID, correlationId.toString());

        IdempotencySession idempotencySession = null;
        try {
            log.info("Received command: type={}, householdId={}", request.type(), request.householdId());

            // Resolve current user from JWT
            CurrentUser currentUser = userResolver.resolveCurrentUser();
            User user = userService.getById(currentUser.id());

            // Idempotency handling (optional)
            idempotencySession = idempotencyService
                    .begin(idempotencyKeys, user.getId(), request)
                    .orElse(null);
            if (idempotencySession != null && idempotencySession.isReplay()) {
                return replayResponse(idempotencySession.storedResponse());
            }

            // Check membership (IDOR prevention - per security-reviewer)
            membershipService.requireMembership(user.getId(), request.householdId());

            // Execute command
            CommandResponseBase response = commandService.execute(request, user, correlationId);
            if (idempotencySession != null) {
                idempotencyService.storeResponse(idempotencySession.recordId(), HttpStatus.OK.value(), response);
            }

            // Return with correlation ID header
            HttpHeaders headers = new HttpHeaders();
            headers.set(CORRELATION_ID_HEADER, correlationId.toString());
            return ResponseEntity.ok().headers(headers).body(response);

        } catch (ValidationException e) {
            if (idempotencySession != null) {
                var errorResponse = toValidationErrorResponse(correlationId, e);
                idempotencyService.storeResponse(
                        idempotencySession.recordId(), HttpStatus.BAD_REQUEST.value(), errorResponse);
            }
            throw e;
        } catch (BusinessException e) {
            if (idempotencySession != null) {
                HttpStatus status = mapStatus(e.getErrorCode());
                var errorResponse = toBusinessErrorResponse(correlationId, e);
                idempotencyService.storeResponse(idempotencySession.recordId(), status.value(), errorResponse);
            }
            throw e;
        } catch (Exception e) {
            if (idempotencySession != null) {
                var errorResponse = new GlobalExceptionHandler.ErrorResponse(
                        correlationId, ErrorCode.INTERNAL_ERROR.name(), "An unexpected error occurred", null, null);
                idempotencyService.storeResponse(
                        idempotencySession.recordId(), HttpStatus.INTERNAL_SERVER_ERROR.value(), errorResponse);
            }
            throw e;
        } finally {
            MDC.remove(MdcKeys.CORRELATION_ID);
        }
    }

    @PostMapping("/{commandId}/continue")
    @Operation(summary = "Continue a command", description = "Continues a command that is awaiting additional input.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Command processed (status in body)"),
        @ApiResponse(
                responseCode = "400",
                description = "Command cannot be continued in current state",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(
                responseCode = "403",
                description = "Access denied",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Command not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CommandResponseBase> continueCommand(
            @PathVariable UUID commandId,
            @RequestBody @Valid ContinueCommandRequest request,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false)
                    @Parameter(description = "Client-provided correlation ID")
                    String correlationIdHeader) {
        UUID correlationId = correlationIdHeader != null ? parseUuidOrGenerate(correlationIdHeader) : UUID.randomUUID();

        MDC.put(MdcKeys.CORRELATION_ID, correlationId.toString());
        try {
            User currentUser = getCurrentUser();
            CommandResponseBase response =
                    commandService.continueCommand(commandId, request, currentUser, correlationId);
            HttpHeaders headers = new HttpHeaders();
            headers.set(CORRELATION_ID_HEADER, correlationId.toString());
            return ResponseEntity.ok().headers(headers).body(response);
        } finally {
            MDC.remove(MdcKeys.CORRELATION_ID);
        }
    }

    @PostMapping("/{commandId}/confirmations/{confirmationId}/approve")
    @Operation(
            summary = "Approve a pending command confirmation",
            description =
                    """
            Approves a HomeTusk-owned pending confirmation and executes its stored proposed actions.
            This lifecycle slice is initiator-only and revalidates guardrails before mutation.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Confirmation approval processed"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Only the original initiator may approve"),
        @ApiResponse(responseCode = "404", description = "Confirmation not found"),
        @ApiResponse(responseCode = "409", description = "Confirmation is not pending")
    })
    public ResponseEntity<CommandResponseBase> approveConfirmation(
            @PathVariable UUID commandId,
            @PathVariable UUID confirmationId,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false)
                    @Parameter(description = "Client-provided correlation ID")
                    String correlationIdHeader) {
        UUID correlationId = correlationIdHeader != null ? parseUuidOrGenerate(correlationIdHeader) : UUID.randomUUID();
        MDC.put(MdcKeys.CORRELATION_ID, correlationId.toString());
        try {
            User currentUser = getCurrentUser();
            CommandResponseBase response =
                    commandService.approveConfirmation(commandId, confirmationId, currentUser, correlationId);
            HttpHeaders headers = new HttpHeaders();
            headers.set(CORRELATION_ID_HEADER, correlationId.toString());
            return ResponseEntity.ok().headers(headers).body(response);
        } finally {
            MDC.remove(MdcKeys.CORRELATION_ID);
        }
    }

    @PostMapping("/{commandId}/confirmations/{confirmationId}/cancel")
    @Operation(
            summary = "Cancel a pending command confirmation",
            description = "Cancels a HomeTusk-owned pending confirmation without executing proposed actions.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Confirmation cancelled"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Only the original initiator may cancel"),
        @ApiResponse(responseCode = "404", description = "Confirmation not found"),
        @ApiResponse(responseCode = "409", description = "Confirmation is not pending")
    })
    public ResponseEntity<CommandResponseBase> cancelConfirmation(
            @PathVariable UUID commandId,
            @PathVariable UUID confirmationId,
            @RequestBody(required = false) @Valid CommandConfirmationCancelRequest request,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false)
                    @Parameter(description = "Client-provided correlation ID")
                    String correlationIdHeader) {
        UUID correlationId = correlationIdHeader != null ? parseUuidOrGenerate(correlationIdHeader) : UUID.randomUUID();
        MDC.put(MdcKeys.CORRELATION_ID, correlationId.toString());
        try {
            User currentUser = getCurrentUser();
            CommandResponseBase response =
                    commandService.cancelConfirmation(commandId, confirmationId, request, currentUser, correlationId);
            HttpHeaders headers = new HttpHeaders();
            headers.set(CORRELATION_ID_HEADER, correlationId.toString());
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

    private ResponseEntity<Object> replayResponse(StoredResponse storedResponse) {
        HttpHeaders headers = new HttpHeaders();
        Object body = storedResponse.bodyJson();
        try {
            JsonNode jsonNode = objectMapper.readTree(storedResponse.bodyJson());
            body = jsonNode;
            UUID correlationId = extractCorrelationId(jsonNode);
            if (correlationId != null) {
                headers.set(CORRELATION_ID_HEADER, correlationId.toString());
            }
        } catch (Exception e) {
            log.warn("Failed to parse idempotency response JSON", e);
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return ResponseEntity.status(storedResponse.httpStatus())
                .headers(headers)
                .body(body);
    }

    private UUID extractCorrelationId(JsonNode node) {
        JsonNode correlationId = node.get("correlationId");
        if (correlationId != null && correlationId.isTextual()) {
            try {
                return UUID.fromString(correlationId.asText());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    private User getCurrentUser() {
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        return userService.getById(currentUser.id());
    }

    private GlobalExceptionHandler.ErrorResponse toValidationErrorResponse(UUID correlationId, ValidationException ex) {
        return new GlobalExceptionHandler.ErrorResponse(
                correlationId,
                ErrorCode.SCHEMA_INVALID.name(),
                ex.getMessage(),
                ex.getErrors().stream()
                        .map(e -> new GlobalExceptionHandler.ErrorResponse.ValidationError(
                                e.path(), e.code(), e.message()))
                        .toList(),
                null);
    }

    private GlobalExceptionHandler.ErrorResponse toBusinessErrorResponse(UUID correlationId, BusinessException ex) {
        return new GlobalExceptionHandler.ErrorResponse(
                correlationId,
                ex.getErrorCode().name(),
                ex.getMessage(),
                null,
                ex.getViolations().stream()
                        .map(v -> new GlobalExceptionHandler.ErrorResponse.BusinessViolation(v.rule(), v.message()))
                        .toList());
    }

    private HttpStatus mapStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case HOUSEHOLD_NOT_FOUND,
                    TASK_NOT_FOUND,
                    USER_NOT_FOUND,
                    ZONE_NOT_FOUND,
                    NOTIFICATION_NOT_FOUND,
                    CONFIRMATION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVITE_EXPIRED, INVITE_REDEEMED, INVITE_REVOKED -> HttpStatus.GONE;
            case IDEMPOTENCY_CONFLICT, CONFIRMATION_NOT_PENDING, CONFIRMATION_EXPIRED -> HttpStatus.CONFLICT;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    // Error response schema for OpenAPI documentation
    @Schema(description = "Error response")
    public record ErrorResponse(
            @Schema(description = "Correlation ID") UUID correlationId,
            @Schema(description = "Error code") String errorCode,
            @Schema(description = "Error message") String message) {}
}
