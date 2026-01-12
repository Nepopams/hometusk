package com.hometusk.commands.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response when AI needs clarification from user (Stage 2).
 */
public record CommandNeedsInputResponse(
        UUID commandId,
        UUID correlationId,
        String status,
        String question,
        List<String> requiredFields,
        Map<String, Object> suggestions,
        Integer executionMs,
        UUID initiatorId)
        implements CommandResponseBase {}
