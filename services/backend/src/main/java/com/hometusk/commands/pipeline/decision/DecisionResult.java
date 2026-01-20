package com.hometusk.commands.pipeline.decision;

import com.hometusk.commands.domain.DecisionSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Result of a decision. Three variants:
 * - StartJob: Execute the proposed actions
 * - Clarify: Need more information from user
 * - Reject: Cannot process this command
 */
public sealed interface DecisionResult permits DecisionResult.StartJob, DecisionResult.Clarify, DecisionResult.Reject {

    DecisionSource source();

    BigDecimal confidence();

    /** External AI Platform decision ID for tracing (null for manual provider) */
    UUID externalDecisionId();

    /** Raw response from AI Platform for audit (null for manual provider) */
    String rawPayload();

    /**
     * AI decided to start the job with proposed actions.
     */
    record StartJob(
            DecisionSource source,
            BigDecimal confidence,
            UUID externalDecisionId,
            String rawPayload,
            List<ProposedAction> actions)
            implements DecisionResult {

        /** Constructor for manual provider (no external ID or raw payload) */
        public StartJob(DecisionSource source, BigDecimal confidence, List<ProposedAction> actions) {
            this(source, confidence, null, null, actions);
        }

        public record ProposedAction(String actionType, Map<String, Object> parameters) {}
    }

    /**
     * AI needs clarification from the user.
     */
    record Clarify(
            DecisionSource source,
            BigDecimal confidence,
            UUID externalDecisionId,
            String rawPayload,
            String question,
            List<String> requiredFields,
            Map<String, Object> suggestions)
            implements DecisionResult {}

    /**
     * AI rejected the command.
     */
    record Reject(
            DecisionSource source,
            BigDecimal confidence,
            UUID externalDecisionId,
            String rawPayload,
            String reason,
            String errorCode)
            implements DecisionResult {}
}
