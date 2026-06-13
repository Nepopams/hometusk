package com.hometusk.commands.dto;

import java.time.Instant;
import java.util.UUID;

public record CommandScheduledResponse(
        UUID commandId, UUID correlationId, String status, Instant scheduleAt, Integer executionMs, UUID initiatorId)
        implements CommandResponseBase {}
