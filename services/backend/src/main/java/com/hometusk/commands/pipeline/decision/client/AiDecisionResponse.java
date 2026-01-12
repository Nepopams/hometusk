package com.hometusk.commands.pipeline.decision.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO from AI Platform decision API.
 */
public record AiDecisionResponse(
        UUID decisionId,
        String type, // "start_job", "clarify", "reject"
        BigDecimal confidence,
        // For start_job
        List<ProposedActionDto> actions,
        // For clarify
        String question,
        List<String> requiredFields,
        Map<String, Object> suggestions,
        // For reject
        String reason,
        String errorCode) {

    public record ProposedActionDto(String actionType, Map<String, Object> parameters) {}
}
