package com.hometusk.commands.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.commands.domain.Command;
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

    public CommandService(
            CommandRepository commandRepository,
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
            ShoppingService shoppingService) {
        this.commandRepository = commandRepository;
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
                request.clientTimestamp());

        command = commandRepository.save(command);
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
                .contextSnapshot(Map.of(
                        "householdId", command.getHouseholdId(),
                        "scheduleAt", command.getScheduleAt().toString()))
                .decision(Map.of(
                        "type",
                        "scheduled",
                        "scheduleAt",
                        command.getScheduleAt().toString()))
                .source(DecisionSource.USER_OVERRIDE)
                .build());

        int executionMs = (int) (System.currentTimeMillis() - startTime);
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
        }
    }

    private void rejectSchemaValidation(
            Command command, UUID correlationId, CommandType commandType, ValidationException e, long startTime) {
        int executionMs = (int) (System.currentTimeMillis() - startTime);
        command.markRejected(ErrorCode.SCHEMA_INVALID.name(), e.getMessage(), executionMs);
        commandRepository.save(command);

        decisionLogWriter.writeValidationFailure(
                command, correlationId, Map.of("type", commandType.name()), e.getErrors(), true);
    }

    private void rejectBusinessValidation(
            Command command, UUID correlationId, CommandType commandType, BusinessException e, long startTime) {
        int executionMs = (int) (System.currentTimeMillis() - startTime);
        command.markRejected(e.getErrorCode().name(), e.getMessage(), executionMs);
        commandRepository.save(command);

        decisionLogWriter.writeValidationFailure(
                command, correlationId, Map.of("type", commandType.name()), e.getViolations(), false);
    }

    private void failCommand(Command command, UUID correlationId, Exception e, long startTime) {
        int executionMs = (int) (System.currentTimeMillis() - startTime);
        command.markFailed(ErrorCode.INTERNAL_ERROR.name(), e.getMessage(), executionMs);
        commandRepository.save(command);

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
            case DecisionResult.Reject reject -> handleReject(reject, command, correlationId, requester, startTime);
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
                        .contextSnapshot(Map.of("householdId", command.getHouseholdId()))
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
                        .contextSnapshot(Map.of("householdId", command.getHouseholdId()))
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
                .contextSnapshot(Map.of("householdId", command.getHouseholdId()))
                .decision(decisionMap)
                .source(decision.source())
                .confidence(decision.confidence())
                .externalDecisionId(decision.externalDecisionId())
                .rawDecisionPayload(decision.rawPayload())
                .build());

        // Execute all actions and track created entities
        ActionExecutor.ActionResult lastResult = null;
        UUID createdTaskId = null;
        List<UUID> createdItemIds = new ArrayList<>();

        for (var action : decision.actions()) {
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

        CommandResponse.CommandResult commandResult = lastResult != null
                ? CommandResponse.CommandResult.forTask(lastResult.taskId(), lastResult.assigneeId())
                : null;

        // Check if this was a fallback (degraded mode)
        if (decision.source() == DecisionSource.FALLBACK) {
            return CommandResponse.degraded(
                    command.getId(), correlationId, commandResult, executionMs, requester.getId(), "ai_unavailable");
        }

        return CommandResponse.success(command.getId(), correlationId, commandResult, executionMs, requester.getId());
    }

    private CommandResponseBase handleClarify(
            DecisionResult.Clarify decision, Command command, UUID correlationId, User requester, long startTime) {

        // Log decision
        decisionLogWriter.write(DecisionLogWriter.DecisionLogEntry.builder()
                .command(command)
                .correlationId(correlationId)
                .intent(Map.of("type", command.getType().name().toLowerCase()))
                .contextSnapshot(Map.of("householdId", command.getHouseholdId()))
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
                .contextSnapshot(Map.of("householdId", command.getHouseholdId()))
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

        log.info("Command rejected by AI: id={}, correlationId={}, reason={}", command.getId(), correlationId, reason);

        return CommandResponse.rejected(
                command.getId(), correlationId, errorCode, reason, executionMs, requester.getId());
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
