package com.hometusk.commands.dto;

import java.util.UUID;

public record CommandResponse(
        UUID commandId,
        UUID correlationId,
        String status,
        CommandResult result,
        Integer executionMs,
        UUID initiatorId) {

    public static CommandResponse success(
            UUID commandId, UUID correlationId, CommandResult result, int executionMs, UUID initiatorId) {
        return new CommandResponse(commandId, correlationId, "executed", result, executionMs, initiatorId);
    }

    public static CommandResponse degraded(
            UUID commandId,
            UUID correlationId,
            CommandResult result,
            int executionMs,
            UUID initiatorId,
            String degradedReason) {
        return new CommandDegradedResponse(
                commandId, correlationId, "executed_degraded", result, executionMs, initiatorId, degradedReason, null);
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
