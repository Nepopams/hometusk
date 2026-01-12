package com.hometusk.commands.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.commands.domain.Command;
import com.hometusk.commands.domain.CommandType;
import com.hometusk.commands.domain.DecisionSource;
import com.hometusk.commands.dto.*;
import com.hometusk.commands.pipeline.*;
import com.hometusk.commands.repository.CommandRepository;
import com.hometusk.households.domain.Household;
import com.hometusk.households.service.HouseholdService;
import com.hometusk.shared.exception.BusinessException;
import com.hometusk.shared.exception.ErrorCode;
import com.hometusk.shared.exception.ValidationException;
import com.hometusk.users.domain.User;
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
    private final DecisionEngine decisionEngine;
    private final DecisionLogWriter decisionLogWriter;
    private final ActionExecutor actionExecutor;

    public CommandService(
            CommandRepository commandRepository,
            HouseholdService householdService,
            ObjectMapper objectMapper,
            SchemaValidator schemaValidator,
            BusinessValidator businessValidator,
            DecisionEngine decisionEngine,
            DecisionLogWriter decisionLogWriter,
            ActionExecutor actionExecutor) {
        this.commandRepository = commandRepository;
        this.householdService = householdService;
        this.objectMapper = objectMapper;
        this.schemaValidator = schemaValidator;
        this.businessValidator = businessValidator;
        this.decisionEngine = decisionEngine;
        this.decisionLogWriter = decisionLogWriter;
        this.actionExecutor = actionExecutor;
    }

    @Transactional
    public CommandResponse execute(CommandRequest request, User requester, UUID correlationId) {
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

            // 6. Business validation & 7. Decision & 8. Action
            CommandResponse.CommandResult result = switch (commandType) {
                case CREATE_TASK -> processCreateTask(request, command, correlationId, requester.getId());
                case COMPLETE_TASK -> processCompleteTask(request, command, correlationId);
            };

            // 9. Mark executed
            int executionMs = (int) (System.currentTimeMillis() - startTime);
            command.markExecuted(executionMs);
            commandRepository.save(command);

            log.info(
                    "Command executed: id={}, correlationId={}, executionMs={}",
                    command.getId(),
                    correlationId,
                    executionMs);

            return CommandResponse.success(command.getId(), correlationId, result, executionMs, requester.getId());

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

    private CommandResponse.CommandResult processCreateTask(
            CommandRequest request, Command command, UUID correlationId, UUID initiatorId) {

        // Parse payload
        CreateTaskPayload payload = objectMapper.convertValue(request.payload(), CreateTaskPayload.class);

        // Business validation
        businessValidator.validateCreateTask(payload, request.householdId());

        command.markProcessing();

        // Decision
        DecisionEngine.CreateTaskDecision decision = decisionEngine.decideCreateTask(payload, initiatorId);

        // Log decision
        decisionLogWriter.write(DecisionLogWriter.DecisionLogEntry.builder()
                .command(command)
                .correlationId(correlationId)
                .intent(Map.of("type", "create_task", "title", payload.title()))
                .contextSnapshot(Map.of(
                        "householdId", request.householdId(),
                        "initiatorId", initiatorId,
                        "hasAssignee", payload.assigneeId() != null,
                        "hasZone", payload.zoneId() != null,
                        "hasDeadline", payload.deadline() != null))
                .decision(Map.of(
                        "action", "create_task",
                        "assigneeId", decision.assigneeId() != null ? decision.assigneeId().toString() : "null",
                        "source", decision.source().name()))
                .source(decision.source())
                .confidence(decision.confidence())
                .build());

        // Execute (pass correlationId for activity recording)
        ActionExecutor.CreateTaskResult result = actionExecutor.executeCreateTask(decision, command, correlationId);

        return CommandResponse.CommandResult.forTask(result.taskId(), result.assigneeId());
    }

    private CommandResponse.CommandResult processCompleteTask(
            CommandRequest request, Command command, UUID correlationId) {

        // Parse payload
        CompleteTaskPayload payload = objectMapper.convertValue(request.payload(), CompleteTaskPayload.class);

        // Business validation
        businessValidator.validateCompleteTask(payload, request.householdId());

        command.markProcessing();

        // Decision
        DecisionEngine.CompleteTaskDecision decision = decisionEngine.decideCompleteTask(payload);

        // Log decision
        decisionLogWriter.write(DecisionLogWriter.DecisionLogEntry.builder()
                .command(command)
                .correlationId(correlationId)
                .intent(Map.of("type", "complete_task", "taskId", payload.taskId().toString()))
                .contextSnapshot(Map.of("householdId", request.householdId(), "taskId", payload.taskId()))
                .decision(Map.of("action", "complete_task", "source", decision.source().name()))
                .source(decision.source())
                .confidence(decision.confidence())
                .build());

        // Execute (pass correlationId for activity recording)
        ActionExecutor.CompleteTaskResult result = actionExecutor.executeCompleteTask(decision, command, correlationId);

        return CommandResponse.CommandResult.forTask(result.taskId(), null);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }
}
