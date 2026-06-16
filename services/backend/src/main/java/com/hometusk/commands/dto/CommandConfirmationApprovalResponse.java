package com.hometusk.commands.dto;

import java.util.UUID;

public record CommandConfirmationApprovalResponse(
        UUID commandId,
        UUID confirmationId,
        String status,
        CommandResponse.CommandResult result,
        Integer executionMs,
        UUID approvedBy,
        boolean idempotentReplay,
        String errorCode,
        String reason)
        implements CommandResponseBase {}
