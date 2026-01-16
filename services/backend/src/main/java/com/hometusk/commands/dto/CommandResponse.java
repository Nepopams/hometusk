package com.hometusk.commands.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CommandResponse(
        UUID commandId,
        UUID correlationId,
        String status,
        CommandResult result,
        Integer executionMs,
        UUID initiatorId)
        implements CommandResponseBase {

    public static CommandResponse success(
            UUID commandId, UUID correlationId, CommandResult result, int executionMs, UUID initiatorId) {
        return new CommandResponse(commandId, correlationId, "executed", result, executionMs, initiatorId);
    }

    public static CommandResponseBase degraded(
            UUID commandId,
            UUID correlationId,
            CommandResult result,
            int executionMs,
            UUID initiatorId,
            String degradedReason) {
        return new CommandDegradedResponse(
                commandId, correlationId, "executed_degraded", result, executionMs, initiatorId, degradedReason, null);
    }

    /** Stage 2+: AI/Guardrails need clarification from user */
    public static CommandResponseBase needsInput(
            UUID commandId,
            UUID correlationId,
            String question,
            List<String> requiredFields,
            Map<String, Object> suggestions,
            String policyName,
            int executionMs,
            UUID initiatorId) {
        return new CommandNeedsInputResponse(
                commandId,
                correlationId,
                "needs_input",
                question,
                requiredFields,
                suggestions,
                policyName,
                executionMs,
                initiatorId);
    }

    public static CommandResponseBase rejected(
            UUID commandId,
            UUID correlationId,
            String errorCode,
            String reason,
            int executionMs,
            UUID initiatorId) {
        return new CommandRejectedResponse(
                commandId, correlationId, "rejected", errorCode, reason, executionMs, initiatorId);
    }

    public record CommandResult(UUID taskId, UUID assigneeId, Double decisionConfidence) {

        public static CommandResult forTask(UUID taskId, UUID assigneeId) {
            return new CommandResult(taskId, assigneeId, 1.0);
        }

        public static CommandResult empty() {
            return new CommandResult(null, null, 1.0);
        }
    }
}
