package com.hometusk.commands.dto;

import java.util.UUID;

public record CommandDegradedResponse(
        UUID commandId,
        UUID correlationId,
        String status,
        CommandResponse.CommandResult result,
        Integer executionMs,
        UUID initiatorId,
        String degradedReason,
        String fallbackStrategy)
        implements CommandResponseBase {}
