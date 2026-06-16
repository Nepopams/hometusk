package com.hometusk.commands.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CommandNeedsConfirmationResponse(
        UUID commandId,
        UUID correlationId,
        String status,
        Confirmation confirmation,
        ConfirmationTrace trace,
        Integer executionMs,
        UUID initiatorId)
        implements CommandResponseBase {

    public record Confirmation(
            UUID confirmationId,
            String providerConfirmationId,
            String summary,
            List<String> reasons,
            List<String> riskLabels,
            Instant expiresAt,
            List<ProposedAction> proposedActions) {}

    public record ProposedAction(String type, Map<String, Object> parameters) {}

    public record ConfirmationTrace(
            UUID providerDecisionId, String providerTraceId, String schemaVersion, String decisionVersion) {}
}
