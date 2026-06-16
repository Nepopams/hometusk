package com.hometusk.commands.dto;

import com.hometusk.commands.domain.CommandType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CommandRequest(
        @NotNull(message = "householdId is required") UUID householdId,
        @NotBlank(message = "type is required") String type,
        @NotNull(message = "payload is required") @Valid Object payload,
        Instant dueDate,
        UUID assigneeId,
        UUID zoneId,
        Instant scheduleAt,
        @NotBlank(message = "source is required") String source,
        @Size(max = 128, message = "asrTraceId must be at most 128 characters") String asrTraceId,
        Instant clientTimestamp) {

    public CommandType getCommandType() {
        return switch (type.toLowerCase()) {
            case "create_task" -> CommandType.CREATE_TASK;
            case "complete_task" -> CommandType.COMPLETE_TASK;
            case "natural_command" -> CommandType.NATURAL_COMMAND;
            default -> throw new IllegalArgumentException("Unknown command type: " + type);
        };
    }
}
