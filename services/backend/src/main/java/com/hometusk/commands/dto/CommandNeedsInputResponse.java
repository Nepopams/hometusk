package com.hometusk.commands.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response when AI needs clarification from user (Stage 2+).
 * Used when guardrails request user input before proceeding.
 */
public record CommandNeedsInputResponse(
        UUID commandId,
        UUID correlationId,
        String status,
        String question,
        List<String> requiredFields,
        Map<String, Object> suggestions,
        String policyName,
        Integer executionMs,
        UUID initiatorId)
        implements CommandResponseBase {}
