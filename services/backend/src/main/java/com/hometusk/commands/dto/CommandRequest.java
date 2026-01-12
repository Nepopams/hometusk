package com.hometusk.commands.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.hometusk.commands.domain.CommandType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CommandRequest(
        @NotNull(message = "householdId is required") UUID householdId,
        @NotBlank(message = "type is required") String type,
        @NotNull(message = "payload is required") @Valid Object payload,
        @NotBlank(message = "source is required") String source,
        Instant clientTimestamp) {

    public CommandType getCommandType() {
        return switch (type.toLowerCase()) {
            case "create_task" -> CommandType.CREATE_TASK;
            case "complete_task" -> CommandType.COMPLETE_TASK;
            default -> throw new IllegalArgumentException("Unknown command type: " + type);
        };
    }
}
