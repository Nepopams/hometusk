package com.hometusk.commands.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.commands.domain.Command;
import com.hometusk.commands.domain.CommandConfirmation;
import com.hometusk.commands.domain.CommandConfirmationStatus;
import com.hometusk.commands.domain.CommandStatus;
import com.hometusk.commands.domain.CommandType;
import com.hometusk.commands.domain.DecisionSource;
import com.hometusk.commands.dto.*;
import com.hometusk.commands.metrics.DecisionMetrics;
import com.hometusk.commands.pipeline.*;
import com.hometusk.commands.pipeline.decision.DecisionContext;
import com.hometusk.commands.pipeline.decision.DecisionProviderSelector;
import com.hometusk.commands.pipeline.decision.DecisionResult;
import com.hometusk.commands.pipeline.guardrails.GuardrailResult;
import com.hometusk.commands.pipeline.guardrails.GuardrailsOrchestrator;
import com.hometusk.commands.repository.CommandConfirmationRepository;
import com.hometusk.commands.repository.CommandRepository;
import com.hometusk.households.domain.Household;
import com.hometusk.households.service.HouseholdService;
import com.hometusk.shared.exception.AccessDeniedException;
import com.hometusk.shared.exception.BusinessException;
import com.hometusk.shared.exception.ErrorCode;
import com.hometusk.shared.exception.NotFoundException;
import com.hometusk.shared.exception.ValidationException;
import com.hometusk.shopping.service.ShoppingService;
import com.hometusk.users.domain.User;
import com.hometusk.voice.metrics.VoiceMetrics;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the command processing pipeline.
 *
 * <p>Pipeline flow (Stage 3):
 * <ol>
 *   <li>Create Command entity (status=received)</li>
 *   <li>SchemaValidator (validate payload structure)</li>
 *   <li>BusinessValidator (validate domain invariants)</li>
 *   <li>ContextBuilder (build household snapshot for AI and guardrails)</li>
 *   <li>DecisionProviderSelector (get decision from AI Platform or fallback)</li>
 *   <li>GuardrailsOrchestrator (evaluate policies before action execution)</li>
 *   <li>ActionExecutor (execute the action if guardrails pass)</li>
 *   <li>DecisionLogWriter (record decision with guardrails info)</li>
 *   <li>Update Command status</li>
 * </ol>
 *
 * <p>Guardrails can modify, clarify, or reject decisions before execution.
 */
@Service
public class CommandService {

    private static final Logger log = LoggerFactory.getLogger(CommandService.class);

    private final CommandRepository commandRepository;
    private final CommandConfirmationRepository commandConfirmationRepository;
    private final HouseholdService householdService;
    private final ObjectMapper objectMapper;
    private final SchemaValidator schemaValidator;
    private final BusinessValidator businessValidator;
    private final DecisionProviderSelector decisionProviderSelector;
    private final DecisionLogWriter decisionLogWriter;
    private final ActionExecutor actionExecutor;
    private final ContextBuilder contextBuilder;
    private final GuardrailsOrchestrator guardrailsOrchestrator;
    private final DecisionMetrics metrics;
    private final ShoppingService shoppingService;
    private final VoiceMetrics voiceMetrics;

    public CommandService(
            CommandRepository commandRepository,
            CommandConfirmationRepository commandConfirmationRepository,
            HouseholdService householdService,
            ObjectMapper objectMapper,
            SchemaValidator schemaValidator,
            BusinessValidator businessValidator,
            DecisionProviderSelector decisionProviderSelector,
            DecisionLogWriter decisionLogWriter,
            ActionExecutor actionExecutor,
            ContextBuilder contextBuilder,
            GuardrailsOrchestrator guardrailsOrchestrator,
            DecisionMetrics metrics,
            ShoppingService shoppingService,
            VoiceMetrics voiceMetrics) {
        this.commandRepository = commandRepository;
        this.commandConfirmationRepository = commandConfirmationRepository;
        this.householdService = householdService;
        this.objectMapper = objectMapper;
        this.schemaValidator = schemaValidator;
        this.businessValidator = businessValidator;
        this.decisionProviderSelector = decisionProviderSelector;
        this.decisionLogWriter = decisionLogWriter;
        this.actionExecutor = actionExecutor;
        this.contextBuilder = contextBuilder;
        this.guardrailsOrchestrator = guardrailsOrchestrator;
        this.metrics = metrics;
        this.shoppingService = shoppingService;
        this.voiceMetrics = voiceMetrics;
    }

    @Transactional
    public CommandResponseBase execute(CommandRequest request, User requester, UUID correlationId) {
        long startTime = System.currentTimeMillis();

        log.info(
                "Processing command: type={}, householdId={}, correlationId={}",
                request.type(),
                request.householdId(),
                correlationId);

        // 1. Resolve household
        Household household = householdService.getById(request.householdId());

        // 2. Parse command type
        CommandType commandType;
        try {
            commandType = request.getCommandType();
        } catch (IllegalArgumentException e) {
            throw new ValidationException(List.of(new ValidationException.ValidationError(
                    "$.type", "INVALID_TYPE", "Unknown command type", request.type())));
        }

        // 3. Create Command entity (status=received)
        String payloadJson = toJson(request.payload());
        Command command = new Command(
                correlationId,
                household,
                requester,
                commandType,
                payloadJson,
                request.dueDate(),
                request.assigneeId(),
                request.zoneId(),
                request.scheduleAt(),
                request.source(),
                request.asrTraceId(),
                request.clientTimestamp());

        command = commandRepository.save(command);
        recordVoiceCommandReceived(command);
        log.debug("Command created: id={}, correlationId={}", command.getId(), correlationId);

        try {
            Map<String, Object> effectivePayload = buildEffectivePayload(commandType, request);

            if (request.scheduleAt() != null) {
                return scheduleCommand(command, effectivePayload, correlationId, requester, startTime);
            }

            return processCommand(command, effectivePayload, requester, correlationId, startTime);

        } catch (ValidationException e) {
            rejectSchemaValidation(command, correlationId, commandType, e, startTime);
            throw e;

        } catch (BusinessException e) {
            rejectBusinessValidation(command, correlationId, commandType, e, startTime);
            throw e;

        } catch (Exception e) {
            failCommand(command, correlationId, e, startTime);
            throw e;
        }
    }

    @Transactional
    public CommandResponseBase continueCommand(
            UUID commandId, ContinueCommandRequest request, User requester, UUID correlationId) {
        long startTime = System.currentTimeMillis();
        log.info("Continuing command: commandId={}, correlationId={}", commandId, correlationId);

        Command command = commandRepository
                .findById(commandId)
                .orElseThrow(
                        () -> new NotFoundException(ErrorCode.COMMAND_NOT_FOUND, "Command not found: " + commandId));

        if (!command.getRequesterId().equals(requester.getId())) {
            throw new AccessDeniedException("User is not the initiator of this command");
        }

        if (command.getStatus() != CommandStatus.NEEDS_INPUT) {
            throw new BusinessException(ErrorCode.COMMAND_NOT_CONTINUABLE);
        }

        Map<String, Object> originalPayload = readPayload(command.getPayload());
        Map<String, Object> mergedPayload = new HashMap<>(originalPayload);
        mergedPayload.putAll(request.additionalInput());

        Map<String, Object> effectivePayload = buildEffectivePayload(
                command.getType(), mergedPayload, command.getDueDate(), command.getAssigneeId(), command.getZoneId());

        DecisionContext context = buildDecisionContext(command, effectivePayload, requester.getId(), correlationId);
        DecisionResult result = decisionProviderSelector.decide(context);

        return handleDecisionResult(result, command, context, correlationId, requester, startTime);
    }

    @Transactional
    public CommandConfirmationApprovalResponse approveConfirmation(
            UUID commandId, UUID confirmationId, User requester, UUID correlationId) {
        long startTime = System.currentTimeMillis();
        CommandConfirmation confirmation = loadConfirmationForUpdate(commandId, confirmationId);
        Command command = confirmation.getCommand();
        requireConfirmationInitiator(confirmation, requester);

        if (confirmation.getStatus() == CommandConfirmationStatus.EXECUTED) {
            return approvalResponse(
                    command,
                    confirmation,
                    commandResultFromJson(confirmation.getExecutionResult()),
                    (int) (System.currentTimeMillis() - startTime),
                    true,
                    null,
                    null);
        }

        if (!confirmation.isPending()) {
            throw new BusinessException(
                    ErrorCode.CONFIRMATION_NOT_PENDING,
                    "Command confirmation is not pending: " + confirmation.getStatus());
        }

        Instant now = Instant.now();
        if (confirmation.isExpiredAt(now)) {
            int executionMs = (int) (System.currentTimeMillis() - startTime);
            confirmation.markExpired(now);
            command.markRejected(
                    ErrorCode.CONFIRMATION_EXPIRED.name(), "Command confirmation expired before approval", executionMs);
            commandConfirmationRepository.save(confirmation);
            commandRepository.save(command);
            writeConfirmationLifecycleLog(
                    command,
                    confirmation,
                    correlationId,
                    "confirmation_expired",
                    Map.of(
                            "status",
                            "expired",
                            "expiresAt",
                            confirmation.getExpiresAt().toString()));
            recordVoiceCommandOutcome(command, "rejected");
            return approvalResponse(
                    command,
                    confirmation,
                    null,
                    executionMs,
                    false,
                    ErrorCode.CONFIRMATION_EXPIRED.name(),
                    "Command confirmation expired before approval");
        }

        Map<String, Object> originalPayload = readPayload(command.getPayload());
        Map<String, Object> effectivePayload = buildEffectivePayload(
                command.getType(), originalPayload, command.getDueDate(), command.getAssigneeId(), command.getZoneId());
        DecisionContext context = buildDecisionContext(command, effectivePayload, requester.getId(), correlationId);
        DecisionResult.StartJob decision = new DecisionResult.StartJob(
                DecisionSource.USER_OVERRIDE,
                BigDecimal.ONE,
                confirmation.getProviderDecisionId(),
                null,
                storedProposedActions(confirmation));

        GuardrailResult guardrailResult = guardrailsOrchestrator.evaluate(decision, context);
        return switch (guardrailResult) {
            case GuardrailResult.Proceed proceed -> {
                confirmation.markConfirmed(requester, now);
                CommandResponse.CommandResult result =
                        executeActions(proceed.decision().actions(), command, correlationId);
                int executionMs = (int) (System.currentTimeMillis() - startTime);
                command.markExecuted(executionMs);
                confirmation.markExecuted(toJson(result), Instant.now());
                commandRepository.save(command);
                commandConfirmationRepository.save(confirmation);
                writeConfirmationLifecycleLog(
                        command,
                        confirmation,
                        correlationId,
                        "confirmation_approved",
                        Map.of(
                                "status",
                                "executed",
                                "actionsCount",
                                proceed.decision().actions().size(),
                                "guardrails_policies",
                                proceed.appliedPolicies()));
                recordVoiceCommandOutcome(command, "executed");
                yield approvalResponse(command, confirmation, result, executionMs, false, null, null);
            }
            case GuardrailResult.NeedsClarification clarify -> {
                int executionMs = (int) (System.currentTimeMillis() - startTime);
                confirmation.markRejected("CONFIRMATION_GUARDRAILS_CLARIFY", clarify.question(), Instant.now());
                command.markRejected("CONFIRMATION_GUARDRAILS_CLARIFY", clarify.question(), executionMs);
                commandRepository.save(command);
                commandConfirmationRepository.save(confirmation);
                writeConfirmationLifecycleLog(
                        command,
                        confirmation,
                        correlationId,
                        "confirmation_guardrails_clarify",
                        Map.of(
                                "status", "rejected",
                                "policy", clarify.triggeredPolicy(),
                                "question", clarify.question()));
                recordVoiceCommandOutcome(command, "rejected");
                yield approvalResponse(
                        command,
                        confirmation,
                        null,
                        executionMs,
                        false,
                        "CONFIRMATION_GUARDRAILS_CLARIFY",
                        clarify.question());
            }
            case GuardrailResult.Rejected rejected -> {
                int executionMs = (int) (System.currentTimeMillis() - startTime);
                String errorCode =
                        rejected.errorCode() != null ? rejected.errorCode() : ErrorCode.GUARDRAILS_REJECTED.name();
                confirmation.markRejected(errorCode, rejected.reason(), Instant.now());
                command.markRejected(errorCode, rejected.reason(), executionMs);
                commandRepository.save(command);
                commandConfirmationRepository.save(confirmation);
                writeConfirmationLifecycleLog(
                        command,
                        confirmation,
                        correlationId,
                        "confirmation_guardrails_reject",
                        Map.of(
                                "status",
                                "rejected",
                                "policy",
                                rejected.triggeredPolicy(),
                                "reason",
                                rejected.reason(),
                                "errorCode",
                                errorCode));
                recordVoiceCommandOutcome(command, "rejected");
                yield approvalResponse(command, confirmation, null, executionMs, false, errorCode, rejected.reason());
            }
        };
    }

    @Transactional
    public CommandConfirmationCancelResponse cancelConfirmation(
            UUID commandId,
            UUID confirmationId,
            CommandConfirmationCancelRequest request,
            User requester,
            UUID correlationId) {
        long startTime = System.currentTimeMillis();
        CommandConfirmation confirmation = loadConfirmationForUpdate(commandId, confirmationId);
        Command command = confirmation.getCommand();
        requireConfirmationInitiator(confirmation, requester);

        if (confirmation.getStatus() == CommandConfirmationStatus.CANCELLED) {
            return cancelResponse(
                    command,
                    confirmation,
                    (int) (System.currentTimeMillis() - startTime),
                    true,
                    confirmation.getCancelReason());
        }

        if (!confirmation.isPending()) {
            throw new BusinessException(
                    ErrorCode.CONFIRMATION_NOT_PENDING,
                    "Command confirmation is not pending: " + confirmation.getStatus());
        }

        String reason = request != null ? request.reason() : null;
        int executionMs = (int) (System.currentTimeMillis() - startTime);
        confirmation.markCancelled(requester, reason, Instant.now());
        command.markRejected("CONFIRMATION_CANCELLED", "Command confirmation cancelled", executionMs);
        commandConfirmationRepository.save(confirmation);
        commandRepository.save(command);
        writeConfirmationLifecycleLog(
                command,
                confirmation,
                correlationId,
                "confirmation_cancelled",
                Map.of("status", "cancelled", "reason", reason != null ? reason : ""));
        recordVoiceCommandOutcome(command, "rejected");
        return cancelResponse(command, confirmation, executionMs, false, reason);
    }

    @Transactional
    public boolean executeScheduledCommand(UUID commandId, UUID correlationId) {
        long startTime = System.currentTimeMillis();

        Command command = commandRepository.findByIdForUpdate(commandId).orElse(null);
        if (command == null) {
            log.warn("Scheduled command not found: commandId={}", commandId);
            return false;
        }

        if (command.getStatus() != CommandStatus.SCHEDULED) {
            log.debug(
                    "Scheduled command skipped because status changed: commandId={}, status={}",
                    commandId,
                    command.getStatus());
            return false;
        }

        if (command.getScheduleAt() == null || command.getScheduleAt().isAfter(Instant.now())) {
            log.debug(
                    "Scheduled command skipped because it is not due: commandId={}, scheduleAt={}",
                    commandId,
                    command.getScheduleAt());
            return false;
        }

        try {
            Map<String, Object> originalPayload = readPayload(command.getPayload());
            Map<String, Object> effectivePayload = buildEffectivePayload(
                    command.getType(),
                    originalPayload,
                    command.getDueDate(),
                    command.getAssigneeId(),
                    command.getZoneId());

            processCommand(command, effectivePayload, command.getRequester(), correlationId, startTime);
            return true;
        } catch (ValidationException e) {
            rejectSchemaValidation(command, correlationId, command.getType(), e, startTime);
            return true;
        } catch (BusinessException e) {
            rejectBusinessValidation(command, correlationId, command.getType(), e, startTime);
            return true;
        } catch (Exception e) {
            failCommand(command, correlationId, e, startTime);
            return true;
        }
    }

    private CommandResponseBase scheduleCommand(
            Command command, Map<String, Object> effectivePayload, UUID correlationId, User requester, long startTime) {
        if (!command.getScheduleAt().isAfter(Instant.now())) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Schedule time must be in the future",
                    List.of(new BusinessException.Violation(
                            "SCHEDULE_AT_MUST_BE_FUTURE", "Schedule time must be in the future")));
        }

        command.markValidating();
        validateCommandPayload(command, effectivePayload);
        command.markScheduled();
        commandRepository.save(command);

        decisionLogWriter.write(DecisionLogWriter.DecisionLogEntry.builder()
                .command(command)
                .correlationId(correlationId)
                .intent(Map.of("type", command.getType().name().toLowerCase()))
                .contextSnapshot(commandContextSnapshot(
                        command, Map.of("scheduleAt", command.getScheduleAt().toString())))
                .decision(Map.of(
                        "type",
                        "scheduled",
                        "scheduleAt",
                        command.getScheduleAt().toString()))
                .source(DecisionSource.USER_OVERRIDE)
                .build());

        int executionMs = (int) (System.currentTimeMillis() - startTime);
        recordVoiceCommandOutcome(command, "scheduled");
        return CommandResponse.scheduled(
                command.getId(), correlationId, command.getScheduleAt(), executionMs, requester.getId());
    }

    private CommandResponseBase processCommand(
            Command command, Map<String, Object> effectivePayload, User requester, UUID correlationId, long startTime) {
        command.markValidating();
        validateCommandPayload(command, effectivePayload);
        command.markProcessing();

        DecisionContext context = buildDecisionContext(command, effectivePayload, requester.getId(), correlationId);
        DecisionResult result = decisionProviderSelector.decide(context);

        return handleDecisionResult(result, command, context, correlationId, requester, startTime);
    }

    private void validateCommandPayload(Command command, Map<String, Object> effectivePayload) {
        schemaValidator.validate(command.getType(), effectivePayload);

        switch (command.getType()) {
            case CREATE_TASK -> {
                CreateTaskPayload payload = objectMapper.convertValue(effectivePayload, CreateTaskPayload.class);
                businessValidator.validateCreateTask(payload, command.getHouseholdId());
            }
            case COMPLETE_TASK -> {
                CompleteTaskPayload payload = objectMapper.convertValue(effectivePayload, CompleteTaskPayload.class);
                businessValidator.validateCompleteTask(payload, command.getHouseholdId());
            }
            case NATURAL_COMMAND -> {
                // Natural command payload is schema-validated here. Domain invariants are enforced after
                // AI mapping, guardrail proposal checks, and eventual explicit approval.
            }
        }
    }

    private void rejectSchemaValidation(
            Command command, UUID correlationId, CommandType commandType, ValidationException e, long startTime) {
        int executionMs = (int) (System.currentTimeMillis() - startTime);
        command.markRejected(ErrorCode.SCHEMA_INVALID.name(), e.getMessage(), executionMs);
        commandRepository.save(command);
        recordVoiceCommandOutcome(command, "rejected");

        decisionLogWriter.writeValidationFailure(
                command, correlationId, Map.of("type", commandType.name()), e.getErrors(), true);
    }

    private void rejectBusinessValidation(
            Command command, UUID correlationId, CommandType commandType, BusinessException e, long startTime) {
        int executionMs = (int) (System.currentTimeMillis() - startTime);
        command.markRejected(e.getErrorCode().name(), e.getMessage(), executionMs);
        commandRepository.save(command);
        recordVoiceCommandOutcome(command, "rejected");

        decisionLogWriter.writeValidationFailure(
                command, correlationId, Map.of("type", commandType.name()), e.getViolations(), false);
    }

    private void failCommand(Command command, UUID correlationId, Exception e, long startTime) {
        int executionMs = (int) (System.currentTimeMillis() - startTime);
        command.markFailed(ErrorCode.INTERNAL_ERROR.name(), e.getMessage(), executionMs);
        commandRepository.save(command);
        recordVoiceCommandOutcome(command, "failed");

        log.error("Command failed: id={}, correlationId={}", command.getId(), correlationId, e);
    }

    private Map<String, Object> buildEffectivePayload(CommandType commandType, CommandRequest request) {
        return buildEffectivePayload(
                commandType, request.payload(), request.dueDate(), request.assigneeId(), request.zoneId());
    }

    private Map<String, Object> buildEffectivePayload(
            CommandType commandType, Object rawPayload, Instant dueDate, UUID assigneeId, UUID zoneId) {
        Map<String, Object> payload =
                new HashMap<>(objectMapper.convertValue(rawPayload, new TypeReference<Map<String, Object>>() {}));

        if (commandType == CommandType.CREATE_TASK) {
            mergeUuidAttribute(payload, "assigneeId", assigneeId);
            mergeUuidAttribute(payload, "zoneId", zoneId);
            mergeDueDate(payload, dueDate);
        }

        return payload;
    }

    private void mergeUuidAttribute(Map<String, Object> payload, String field, UUID topLevelValue) {
        if (topLevelValue == null) {
            return;
        }

        Object payloadValue = payload.get(field);
        if (payloadValue != null && !topLevelValue.toString().equals(payloadValue.toString())) {
            throw attributeConflict(field, field);
        }

        payload.put(field, topLevelValue.toString());
    }

    private void mergeDueDate(Map<String, Object> payload, Instant dueDate) {
        if (dueDate == null) {
            return;
        }

        Object payloadValue = payload.get("deadline");
        if (payloadValue != null && !sameInstant(payloadValue, dueDate)) {
            throw attributeConflict("dueDate", "deadline");
        }

        payload.put("deadline", dueDate.toString());
    }

    private boolean sameInstant(Object payloadValue, Instant topLevelValue) {
        try {
            return Instant.parse(payloadValue.toString()).equals(topLevelValue);
        } catch (RuntimeException e) {
            return topLevelValue.toString().equals(payloadValue.toString());
        }
    }

    private BusinessException attributeConflict(String topLevelField, String payloadField) {
        return new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                "Command attribute conflicts with payload",
                List.of(new BusinessException.Violation(
                        "COMMAND_ATTRIBUTE_CONFLICT",
                        "Command field " + topLevelField + " conflicts with payload field " + payloadField)));
    }

    private DecisionContext buildDecisionContext(
            Command command, Map<String, Object> payload, UUID requesterId, UUID correlationId) {
        Map<String, Object> householdContext =
                contextBuilder.buildHouseholdContextForAi(command.getHouseholdId(), correlationId);

        return DecisionContext.builder()
                .commandId(command.getId())
                .correlationId(correlationId)
                .commandType(command.getType())
                .payload(payload)
                .requesterId(requesterId)
                .householdId(command.getHouseholdId())
                .householdContext(householdContext)
                .build();
    }

    private CommandResponseBase handleDecisionResult(
            DecisionResult result,
            Command command,
            DecisionContext context,
            UUID correlationId,
            User requester,
            long startTime) {

        return switch (result) {
            case DecisionResult.StartJob startJob -> handleStartJob(
                    startJob, command, context, correlationId, requester, startTime);
            case DecisionResult.Clarify clarify -> handleClarify(clarify, command, correlationId, requester, startTime);
            case DecisionResult.Confirm confirm -> handleConfirm(
                    confirm, command, context, correlationId, requester, startTime);
            case DecisionResult.Reject reject -> handleReject(reject, command, correlationId, requester, startTime);
        };
    }

    private CommandResponseBase handleConfirm(
            DecisionResult.Confirm decision,
            Command command,
            DecisionContext context,
            UUID correlationId,
            User requester,
            long startTime) {

        DecisionResult.StartJob proposal = new DecisionResult.StartJob(
                decision.source(),
                decision.confidence(),
                decision.externalDecisionId(),
                decision.rawPayload(),
                decision.actions());

        GuardrailResult guardrailResult = guardrailsOrchestrator.evaluate(proposal, context);

        return switch (guardrailResult) {
            case GuardrailResult.Proceed proceed -> {
                metrics.recordDecisionOutcome("needs_confirmation");
                List<CommandNeedsConfirmationResponse.ProposedAction> proposedActions =
                        proposedActionDtos(proceed.decision().actions());
                List<String> reasons = defaultReasons(decision.reasons());
                List<String> riskLabels = defaultRiskLabels(decision.riskLabels());

                CommandConfirmation confirmation = new CommandConfirmation(
                        command,
                        decision.providerConfirmationId(),
                        decision.externalDecisionId(),
                        decision.providerTraceId(),
                        decision.schemaVersion(),
                        decision.decisionVersion(),
                        decision.summary(),
                        toJson(reasons),
                        toJson(riskLabels),
                        toJson(proposedActions),
                        decision.expiresAt());
                confirmation = commandConfirmationRepository.save(confirmation);

                decisionLogWriter.write(DecisionLogWriter.DecisionLogEntry.builder()
                        .command(command)
                        .correlationId(correlationId)
                        .intent(Map.of("type", command.getType().name().toLowerCase()))
                        .contextSnapshot(commandContextSnapshot(command))
                        .decision(Map.of(
                                "type",
                                "needs_confirmation",
                                "confirmationId",
                                confirmation.getId().toString(),
                                "providerConfirmationId",
                                nullableString(decision.providerConfirmationId()),
                                "actionsCount",
                                proposedActions.size(),
                                "guardrails_policies",
                                proceed.appliedPolicies()))
                        .source(decision.source())
                        .confidence(decision.confidence())
                        .externalDecisionId(decision.externalDecisionId())
                        .rawDecisionPayload(decision.rawPayload())
                        .build());

                int executionMs = (int) (System.currentTimeMillis() - startTime);
                command.markNeedsConfirmation(decision.summary());
                commandRepository.save(command);
                recordVoiceCommandOutcome(command, "needs_confirmation");

                yield CommandResponse.needsConfirmation(
                        command.getId(),
                        correlationId,
                        new CommandNeedsConfirmationResponse.Confirmation(
                                confirmation.getId(),
                                decision.providerConfirmationId(),
                                decision.summary(),
                                reasons,
                                riskLabels,
                                decision.expiresAt(),
                                proposedActions),
                        new CommandNeedsConfirmationResponse.ConfirmationTrace(
                                decision.externalDecisionId(),
                                decision.providerTraceId(),
                                decision.schemaVersion(),
                                decision.decisionVersion()),
                        executionMs,
                        requester.getId());
            }

            case GuardrailResult.NeedsClarification clarify -> {
                metrics.recordDecisionOutcome("clarify");
                decisionLogWriter.write(DecisionLogWriter.DecisionLogEntry.builder()
                        .command(command)
                        .correlationId(correlationId)
                        .intent(Map.of("type", command.getType().name().toLowerCase()))
                        .contextSnapshot(commandContextSnapshot(command))
                        .decision(Map.of(
                                "type", "confirmation_guardrails_clarify",
                                "policy", clarify.triggeredPolicy(),
                                "question", clarify.question()))
                        .source(decision.source())
                        .confidence(decision.confidence())
                        .externalDecisionId(decision.externalDecisionId())
                        .rawDecisionPayload(decision.rawPayload())
                        .build());

                int executionMs = (int) (System.currentTimeMillis() - startTime);
                command.markNeedsInput(clarify.question());
                commandRepository.save(command);
                recordVoiceCommandOutcome(command, "needs_input");

                yield CommandResponse.needsInput(
                        command.getId(),
                        correlationId,
                        clarify.question(),
                        clarify.requiredFields(),
                        clarify.suggestions(),
                        clarify.triggeredPolicy(),
                        executionMs,
                        requester.getId());
            }

            case GuardrailResult.Rejected rejected -> {
                metrics.recordDecisionOutcome("reject");
                decisionLogWriter.write(DecisionLogWriter.DecisionLogEntry.builder()
                        .command(command)
                        .correlationId(correlationId)
                        .intent(Map.of("type", command.getType().name().toLowerCase()))
                        .contextSnapshot(commandContextSnapshot(command))
                        .decision(Map.of(
                                "type", "confirmation_guardrails_reject",
                                "policy", rejected.triggeredPolicy(),
                                "reason", rejected.reason(),
                                "errorCode", rejected.errorCode()))
                        .source(decision.source())
                        .confidence(decision.confidence())
                        .externalDecisionId(decision.externalDecisionId())
                        .rawDecisionPayload(decision.rawPayload())
                        .build());

                int executionMs = (int) (System.currentTimeMillis() - startTime);
                String errorCode =
                        rejected.errorCode() != null ? rejected.errorCode() : ErrorCode.GUARDRAILS_REJECTED.name();
                command.markRejected(errorCode, rejected.reason(), executionMs);
                commandRepository.save(command);
                recordVoiceCommandOutcome(command, "rejected");
                yield CommandResponse.rejected(
                        command.getId(), correlationId, errorCode, rejected.reason(), executionMs, requester.getId());
            }
        };
    }

    private CommandResponseBase handleStartJob(
            DecisionResult.StartJob decision,
            Command command,
            DecisionContext context,
            UUID correlationId,
            User requester,
            long startTime) {

        // Evaluate guardrails BEFORE executing actions
        GuardrailResult guardrailResult = guardrailsOrchestrator.evaluate(decision, context);

        return switch (guardrailResult) {
            case GuardrailResult.Proceed proceed -> {
                metrics.recordDecisionOutcome("applied");
                yield executeStartJob(
                        proceed.decision(), command, correlationId, requester, startTime, proceed.appliedPolicies());
            }

            case GuardrailResult.NeedsClarification clarify -> {
                metrics.recordDecisionOutcome("clarify");
                log.info(
                        "Guardrails requested clarification: correlationId={}, policy={}, question={}",
                        correlationId,
                        clarify.triggeredPolicy(),
                        clarify.question());

                // Log guardrails clarification
                decisionLogWriter.write(DecisionLogWriter.DecisionLogEntry.builder()
                        .command(command)
                        .correlationId(correlationId)
                        .intent(Map.of("type", command.getType().name().toLowerCase()))
                        .contextSnapshot(commandContextSnapshot(command))
                        .decision(Map.of(
                                "type", "guardrails_clarify",
                                "policy", clarify.triggeredPolicy(),
                                "question", clarify.question()))
                        .source(decision.source())
                        .confidence(decision.confidence())
                        .externalDecisionId(decision.externalDecisionId())
                        .rawDecisionPayload(decision.rawPayload())
                        .build());

                int executionMs = (int) (System.currentTimeMillis() - startTime);
                command.markNeedsInput(clarify.question());
                commandRepository.save(command);
                recordVoiceCommandOutcome(command, "needs_input");

                yield CommandResponse.needsInput(
                        command.getId(),
                        correlationId,
                        clarify.question(),
                        clarify.requiredFields(),
                        clarify.suggestions(),
                        clarify.triggeredPolicy(),
                        executionMs,
                        requester.getId());
            }

            case GuardrailResult.Rejected rejected -> {
                metrics.recordDecisionOutcome("reject");
                log.warn(
                        "Guardrails rejected: correlationId={}, policy={}, reason={}",
                        correlationId,
                        rejected.triggeredPolicy(),
                        rejected.reason());

                // Log guardrails rejection
                decisionLogWriter.write(DecisionLogWriter.DecisionLogEntry.builder()
                        .command(command)
                        .correlationId(correlationId)
                        .intent(Map.of("type", command.getType().name().toLowerCase()))
                        .contextSnapshot(commandContextSnapshot(command))
                        .decision(Map.of(
                                "type", "guardrails_reject",
                                "policy", rejected.triggeredPolicy(),
                                "reason", rejected.reason(),
                                "errorCode", rejected.errorCode()))
                        .source(decision.source())
                        .confidence(decision.confidence())
                        .externalDecisionId(decision.externalDecisionId())
                        .rawDecisionPayload(decision.rawPayload())
                        .build());

                int executionMs = (int) (System.currentTimeMillis() - startTime);
                String errorCode =
                        rejected.errorCode() != null ? rejected.errorCode() : ErrorCode.GUARDRAILS_REJECTED.name();
                command.markRejected(errorCode, rejected.reason(), executionMs);
                commandRepository.save(command);
                recordVoiceCommandOutcome(command, "rejected");
                yield CommandResponse.rejected(
                        command.getId(), correlationId, errorCode, rejected.reason(), executionMs, requester.getId());
            }
        };
    }

    private CommandResponseBase executeStartJob(
            DecisionResult.StartJob decision,
            Command command,
            UUID correlationId,
            User requester,
            long startTime,
            java.util.List<String> appliedPolicies) {

        // Log decision with guardrails info
        Map<String, Object> decisionMap = new HashMap<>();
        decisionMap.put("type", "start_job");
        decisionMap.put("source", decision.source().name());
        decisionMap.put("actionsCount", decision.actions().size());
        decisionMap.put("guardrails_policies", appliedPolicies);

        decisionLogWriter.write(DecisionLogWriter.DecisionLogEntry.builder()
                .command(command)
                .correlationId(correlationId)
                .intent(Map.of("type", command.getType().name().toLowerCase()))
                .contextSnapshot(commandContextSnapshot(command))
                .decision(decisionMap)
                .source(decision.source())
                .confidence(decision.confidence())
                .externalDecisionId(decision.externalDecisionId())
                .rawDecisionPayload(decision.rawPayload())
                .build());

        CommandResponse.CommandResult commandResult = executeActions(decision.actions(), command, correlationId);

        // Mark executed
        int executionMs = (int) (System.currentTimeMillis() - startTime);
        command.markExecuted(executionMs);
        commandRepository.save(command);

        log.info(
                "Command executed: id={}, correlationId={}, executionMs={}, source={}, guardrails={}",
                command.getId(),
                correlationId,
                executionMs,
                decision.source(),
                appliedPolicies);

        // Check if this was a fallback (degraded mode)
        if (decision.source() == DecisionSource.FALLBACK) {
            recordVoiceCommandOutcome(command, "executed_degraded");
            return CommandResponse.degraded(
                    command.getId(), correlationId, commandResult, executionMs, requester.getId(), "ai_unavailable");
        }

        recordVoiceCommandOutcome(command, "executed");
        return CommandResponse.success(command.getId(), correlationId, commandResult, executionMs, requester.getId());
    }

    private CommandResponse.CommandResult executeActions(
            List<DecisionResult.StartJob.ProposedAction> actions, Command command, UUID correlationId) {
        ActionExecutor.ActionResult lastResult = null;
        UUID createdTaskId = null;
        List<UUID> createdItemIds = new ArrayList<>();

        for (var action : actions) {
            ActionExecutor.ActionResult result = actionExecutor.executeAction(action, command, correlationId);
            lastResult = result;

            // Track created entities for linking
            if ("create_task".equals(result.actionType())) {
                createdTaskId = result.entityId();
            } else if ("add_shopping_item".equals(result.actionType())) {
                createdItemIds.add(result.entityId());
            }
        }

        // Link shopping items to task if both created in same decision (Stage 5)
        if (createdTaskId != null && !createdItemIds.isEmpty()) {
            shoppingService.linkItemsToTask(createdItemIds, createdTaskId, command.getHouseholdId());
            log.debug("Linked {} shopping items to task {}", createdItemIds.size(), createdTaskId);
        }

        return lastResult != null
                ? CommandResponse.CommandResult.forTask(lastResult.taskId(), lastResult.assigneeId())
                : null;
    }

    private CommandResponseBase handleClarify(
            DecisionResult.Clarify decision, Command command, UUID correlationId, User requester, long startTime) {

        // Log decision
        decisionLogWriter.write(DecisionLogWriter.DecisionLogEntry.builder()
                .command(command)
                .correlationId(correlationId)
                .intent(Map.of("type", command.getType().name().toLowerCase()))
                .contextSnapshot(commandContextSnapshot(command))
                .decision(Map.of(
                        "type", "clarify",
                        "question", decision.question(),
                        "requiredFields", decision.requiredFields()))
                .source(decision.source())
                .confidence(decision.confidence())
                .externalDecisionId(decision.externalDecisionId())
                .rawDecisionPayload(decision.rawPayload())
                .build());

        // Mark needs input
        int executionMs = (int) (System.currentTimeMillis() - startTime);
        command.markNeedsInput(decision.question());
        commandRepository.save(command);
        recordVoiceCommandOutcome(command, "needs_input");

        log.info(
                "Command needs input: id={}, correlationId={}, question={}",
                command.getId(),
                correlationId,
                decision.question());

        return CommandResponse.needsInput(
                command.getId(),
                correlationId,
                decision.question(),
                decision.requiredFields(),
                decision.suggestions(),
                null,
                executionMs,
                requester.getId());
    }

    private CommandResponseBase handleReject(
            DecisionResult.Reject decision, Command command, UUID correlationId, User requester, long startTime) {

        // Log decision
        decisionLogWriter.write(DecisionLogWriter.DecisionLogEntry.builder()
                .command(command)
                .correlationId(correlationId)
                .intent(Map.of("type", command.getType().name().toLowerCase()))
                .contextSnapshot(commandContextSnapshot(command))
                .decision(Map.of("type", "reject", "reason", decision.reason(), "errorCode", decision.errorCode()))
                .source(decision.source())
                .confidence(decision.confidence())
                .externalDecisionId(decision.externalDecisionId())
                .rawDecisionPayload(decision.rawPayload())
                .build());

        // Mark rejected
        int executionMs = (int) (System.currentTimeMillis() - startTime);
        String errorCode = decision.errorCode() != null ? decision.errorCode() : ErrorCode.AI_REJECTED.name();
        String reason = decision.reason() != null && !decision.reason().isBlank()
                ? decision.reason()
                : ErrorCode.AI_REJECTED.getDefaultMessage();
        command.markRejected(errorCode, reason, executionMs);
        commandRepository.save(command);
        recordVoiceCommandOutcome(command, "rejected");

        log.info("Command rejected by AI: id={}, correlationId={}, reason={}", command.getId(), correlationId, reason);

        return CommandResponse.rejected(
                command.getId(), correlationId, errorCode, reason, executionMs, requester.getId());
    }

    private Map<String, Object> commandContextSnapshot(Command command) {
        return commandContextSnapshot(command, Map.of());
    }

    private Map<String, Object> commandContextSnapshot(Command command, Map<String, Object> extra) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("householdId", command.getHouseholdId());
        if (command.getAsrTraceId() != null && !command.getAsrTraceId().isBlank()) {
            snapshot.put("asrTraceId", command.getAsrTraceId());
        }
        snapshot.putAll(extra);
        return snapshot;
    }

    private List<CommandNeedsConfirmationResponse.ProposedAction> proposedActionDtos(
            List<DecisionResult.StartJob.ProposedAction> actions) {
        return actions.stream()
                .map(action ->
                        new CommandNeedsConfirmationResponse.ProposedAction(action.actionType(), action.parameters()))
                .toList();
    }

    private List<String> defaultReasons(List<String> reasons) {
        return reasons == null || reasons.isEmpty() ? List.of("AI Platform requested confirmation.") : reasons;
    }

    private List<String> defaultRiskLabels(List<String> riskLabels) {
        return riskLabels == null || riskLabels.isEmpty() ? List.of("provider_confirmation") : riskLabels;
    }

    private String nullableString(String value) {
        return value != null ? value : "";
    }

    private CommandConfirmation loadConfirmationForUpdate(UUID commandId, UUID confirmationId) {
        return commandConfirmationRepository
                .findWithLockByIdAndCommand_Id(confirmationId, commandId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.CONFIRMATION_NOT_FOUND, "Command confirmation not found: " + confirmationId));
    }

    private void requireConfirmationInitiator(CommandConfirmation confirmation, User requester) {
        if (!confirmation.getInitiatorId().equals(requester.getId())) {
            throw new AccessDeniedException("Only the original command initiator can approve or cancel confirmation");
        }
    }

    private List<DecisionResult.StartJob.ProposedAction> storedProposedActions(CommandConfirmation confirmation) {
        try {
            List<CommandNeedsConfirmationResponse.ProposedAction> actions = objectMapper.readValue(
                    confirmation.getProposedActions(),
                    new TypeReference<List<CommandNeedsConfirmationResponse.ProposedAction>>() {});
            return actions.stream()
                    .map(action -> new DecisionResult.StartJob.ProposedAction(action.type(), action.parameters()))
                    .toList();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize stored confirmation actions", e);
        }
    }

    private CommandResponse.CommandResult commandResultFromJson(String executionResultJson) {
        if (executionResultJson == null || executionResultJson.isBlank() || "null".equals(executionResultJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(executionResultJson, CommandResponse.CommandResult.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize stored confirmation execution result", e);
        }
    }

    private CommandConfirmationApprovalResponse approvalResponse(
            Command command,
            CommandConfirmation confirmation,
            CommandResponse.CommandResult result,
            int executionMs,
            boolean idempotentReplay,
            String errorCode,
            String reason) {
        String status = errorCode == null ? "executed" : "rejected";
        return new CommandConfirmationApprovalResponse(
                command.getId(),
                confirmation.getId(),
                status,
                result,
                executionMs,
                confirmation.getApprovedBy(),
                idempotentReplay,
                errorCode,
                reason);
    }

    private CommandConfirmationCancelResponse cancelResponse(
            Command command,
            CommandConfirmation confirmation,
            int executionMs,
            boolean idempotentReplay,
            String reason) {
        return new CommandConfirmationCancelResponse(
                command.getId(),
                confirmation.getId(),
                "cancelled",
                executionMs,
                confirmation.getCancelledBy(),
                idempotentReplay,
                reason);
    }

    private void writeConfirmationLifecycleLog(
            Command command,
            CommandConfirmation confirmation,
            UUID correlationId,
            String lifecycleType,
            Map<String, Object> lifecycleDetails) {
        Map<String, Object> decision = new HashMap<>(lifecycleDetails);
        decision.put("type", lifecycleType);
        decision.put("confirmationId", confirmation.getId().toString());
        decision.put("confirmationStatus", confirmation.getStatus().name().toLowerCase());

        decisionLogWriter.write(DecisionLogWriter.DecisionLogEntry.builder()
                .command(command)
                .correlationId(correlationId)
                .intent(Map.of("type", command.getType().name().toLowerCase()))
                .contextSnapshot(commandContextSnapshot(command))
                .decision(decision)
                .source(DecisionSource.USER_OVERRIDE)
                .externalDecisionId(confirmation.getProviderDecisionId())
                .build());
    }

    private void recordVoiceCommandReceived(Command command) {
        if (isVoiceCommand(command)) {
            voiceMetrics.recordVoiceCommandReceived();
        }
    }

    private void recordVoiceCommandOutcome(Command command, String outcome) {
        if (isVoiceCommand(command)) {
            voiceMetrics.recordVoiceCommandOutcome(outcome);
        }
    }

    private boolean isVoiceCommand(Command command) {
        return command.getSource() != null && "voice".equalsIgnoreCase(command.getSource());
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    private Map<String, Object> readPayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize payload", e);
        }
    }
}
