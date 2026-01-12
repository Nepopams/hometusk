package com.hometusk.commands.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.commands.domain.Command;
import com.hometusk.commands.domain.CommandType;
import com.hometusk.commands.domain.DecisionSource;
import com.hometusk.commands.dto.*;
import com.hometusk.commands.pipeline.*;
import com.hometusk.commands.pipeline.decision.DecisionContext;
import com.hometusk.commands.pipeline.decision.DecisionProviderSelector;
import com.hometusk.commands.pipeline.decision.DecisionResult;
import com.hometusk.commands.repository.CommandRepository;
import com.hometusk.households.domain.Household;
import com.hometusk.households.service.HouseholdService;
import com.hometusk.shared.exception.BusinessException;
import com.hometusk.shared.exception.ErrorCode;
import com.hometusk.shared.exception.ValidationException;
import com.hometusk.users.domain.User;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the command processing pipeline.
 *
 * Pipeline flow:
 * 1. Create Command entity (status=received)
 * 2. SchemaValidator (validate payload structure)
 * 3. BusinessValidator (validate domain invariants)
 * 4. DecisionEngine (make rule-based decision)
 * 5. DecisionLogWriter (record decision)
 * 6. ActionExecutor (execute the action)
 * 7. Update Command status
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

    public CommandService(
            CommandRepository commandRepository,
            HouseholdService householdService,
            ObjectMapper objectMapper,
            SchemaValidator schemaValidator,
            BusinessValidator businessValidator,
            DecisionProviderSelector decisionProviderSelector,
            DecisionLogWriter decisionLogWriter,
            ActionExecutor actionExecutor) {
        this.commandRepository = commandRepository;
        this.householdService = householdService;
        this.objectMapper = objectMapper;
        this.schemaValidator = schemaValidator;
        this.businessValidator = businessValidator;
        this.decisionProviderSelector = decisionProviderSelector;
        this.decisionLogWriter = decisionLogWriter;
        this.actionExecutor = actionExecutor;
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
            throw new ValidationException(
                    new ValidationException.ValidationError("$.type", "INVALID_TYPE", "Unknown command type", null));
        }

        // 3. Create Command entity (status=received)
        String payloadJson = toJson(request.payload());
        Command command = new Command(
                correlationId,
                household,
                requester,
                commandType,
                payloadJson,
                request.source(),
                request.clientTimestamp());

        command = commandRepository.save(command);
        log.debug("Command created: id={}, correlationId={}", command.getId(), correlationId);

        try {
            // 4. Mark validating
            command.markValidating();

            // 5. Schema validation
            schemaValidator.validate(commandType, request.payload());

            // 6. Business validation
            switch (commandType) {
                case CREATE_TASK -> {
                    CreateTaskPayload payload = objectMapper.convertValue(request.payload(), CreateTaskPayload.class);
                    businessValidator.validateCreateTask(payload, request.householdId());
                }
                case COMPLETE_TASK -> {
                    CompleteTaskPayload payload = objectMapper.convertValue(request.payload(), CompleteTaskPayload.class);
                    businessValidator.validateCompleteTask(payload, request.householdId());
                }
            }

            command.markProcessing();

            // 7. Build decision context and get decision
            DecisionContext context = buildDecisionContext(command, request, requester.getId(), correlationId);
            DecisionResult result = decisionProviderSelector.decide(context);

            // 8. Handle result based on type
            return handleDecisionResult(result, command, correlationId, requester, startTime);

        } catch (ValidationException e) {
            int executionMs = (int) (System.currentTimeMillis() - startTime);
            command.markRejected(ErrorCode.SCHEMA_INVALID.name(), e.getMessage(), executionMs);
            commandRepository.save(command);

            // Log the validation failure
            decisionLogWriter.writeValidationFailure(
                    command,
                    correlationId,
                    Map.of("type", commandType.name()),
                    e.getErrors(),
                    true);

            throw e;

        } catch (BusinessException e) {
            int executionMs = (int) (System.currentTimeMillis() - startTime);
            command.markRejected(e.getErrorCode().name(), e.getMessage(), executionMs);
            commandRepository.save(command);

            // Log the business validation failure
            decisionLogWriter.writeValidationFailure(
                    command,
                    correlationId,
                    Map.of("type", commandType.name()),
                    e.getViolations(),
                    false);

            throw e;

        } catch (Exception e) {
            int executionMs = (int) (System.currentTimeMillis() - startTime);
            command.markFailed(ErrorCode.INTERNAL_ERROR.name(), e.getMessage(), executionMs);
            commandRepository.save(command);

            log.error("Command failed: id={}, correlationId={}", command.getId(), correlationId, e);
            throw e;
        }
    }

    private DecisionContext buildDecisionContext(
            Command command, CommandRequest request, UUID requesterId, UUID correlationId) {
        return DecisionContext.builder()
                .commandId(command.getId())
                .correlationId(correlationId)
                .commandType(command.getType())
                .payload(request.payload())
                .requesterId(requesterId)
                .householdId(request.householdId())
                .householdContext(Map.of()) // Minimal context for now
                .build();
    }

    private CommandResponseBase handleDecisionResult(
            DecisionResult result,
            Command command,
            UUID correlationId,
            User requester,
            long startTime) {

        return switch (result) {
            case DecisionResult.StartJob startJob -> handleStartJob(
                    startJob, command, correlationId, requester, startTime);
            case DecisionResult.Clarify clarify -> handleClarify(
                    clarify, command, correlationId, requester, startTime);
            case DecisionResult.Reject reject -> handleReject(
                    reject, command, correlationId, requester, startTime);
        };
    }

    private CommandResponseBase handleStartJob(
            DecisionResult.StartJob decision,
            Command command,
            UUID correlationId,
            User requester,
            long startTime) {

        // Log decision
        Map<String, Object> decisionMap = new HashMap<>();
        decisionMap.put("type", "start_job");
        decisionMap.put("source", decision.source().name());
        decisionMap.put("actionsCount", decision.actions().size());

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

        // Execute all actions
        ActionExecutor.ActionResult lastResult = null;
        for (var action : decision.actions()) {
            lastResult = actionExecutor.executeAction(action, command, correlationId);
        }

        // Mark executed
        int executionMs = (int) (System.currentTimeMillis() - startTime);
        command.markExecuted(executionMs);
        commandRepository.save(command);

        log.info(
                "Command executed: id={}, correlationId={}, executionMs={}, source={}",
                command.getId(),
                correlationId,
                executionMs,
                decision.source());

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
            DecisionResult.Clarify decision,
            Command command,
            UUID correlationId,
            User requester,
            long startTime) {

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
                executionMs,
                requester.getId());
    }

    private CommandResponseBase handleReject(
            DecisionResult.Reject decision,
            Command command,
            UUID correlationId,
            User requester,
            long startTime) {

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
        command.markRejected(decision.errorCode(), decision.reason(), executionMs);
        commandRepository.save(command);

        log.info(
                "Command rejected by AI: id={}, correlationId={}, reason={}",
                command.getId(),
                correlationId,
                decision.reason());

        throw new BusinessException(ErrorCode.AI_REJECTED, decision.reason());
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }
}
