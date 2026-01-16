package com.hometusk.commands.dto;

import java.util.UUID;

public record CommandRejectedResponse(
        UUID commandId,
        UUID correlationId,
        String status,
        String errorCode,
        String reason,
        Integer executionMs,
        UUID initiatorId)
        implements CommandResponseBase {}
