package com.hometusk.commands.pipeline.decision.client;

import com.hometusk.commands.pipeline.decision.DecisionContext;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for AI Platform decision API.
 */
public record AiDecisionRequest(
        UUID commandId,
        UUID correlationId,
        String commandType,
        Map<String, Object> payload,
        UUID requesterId,
        UUID householdId,
        Map<String, Object> householdContext) {

    public static AiDecisionRequest from(DecisionContext context) {
        return new AiDecisionRequest(
                context.commandId(),
                context.correlationId(),
                context.commandType().name().toLowerCase(),
                context.payload(),
                context.requesterId(),
                context.householdId(),
                context.householdContext());
    }
}
