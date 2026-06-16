package com.hometusk.commands.dto;

import java.util.UUID;

public record CommandConfirmationCancelResponse(
        UUID commandId,
        UUID confirmationId,
        String status,
        Integer executionMs,
        UUID cancelledBy,
        boolean idempotentReplay,
        String reason)
        implements CommandResponseBase {}
