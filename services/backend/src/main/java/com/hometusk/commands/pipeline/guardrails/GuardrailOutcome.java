package com.hometusk.commands.pipeline.guardrails;

import com.hometusk.commands.pipeline.decision.DecisionResult.StartJob.ProposedAction;
import java.util.List;
import java.util.Map;

/**
 * Outcome of a single guardrail policy evaluation.
 *
 * <p>Policies return one of these outcomes:
 * <ul>
 *   <li>ACCEPT - Policy approves the decision as-is</li>
 *   <li>MODIFY - Policy suggests modifications to the actions</li>
 *   <li>CLARIFY - Policy needs user input before proceeding</li>
 *   <li>REJECT - Policy rejects the decision entirely</li>
 * </ul>
 *
 * <p>The GuardrailsOrchestrator aggregates outcomes from multiple policies.
 */
public sealed interface GuardrailOutcome {

    /**
     * Policy accepts the decision without modifications.
     */
    record Accept() implements GuardrailOutcome {}

    /**
     * Policy suggests modifications to the proposed actions.
     *
     * @param modifiedActions The modified list of actions
     * @param reason Human-readable reason for modification
     */
    record Modify(List<ProposedAction> modifiedActions, String reason) implements GuardrailOutcome {}

    /**
     * Policy needs user clarification before proceeding.
     *
     * @param question The question to ask the user
     * @param requiredFields Fields that need user input
     * @param suggestions Suggested values for the fields
     */
    record Clarify(String question, List<String> requiredFields, Map<String, Object> suggestions)
            implements GuardrailOutcome {

        public Clarify(String question, List<String> requiredFields) {
            this(question, requiredFields, Map.of());
        }
    }

    /**
     * Policy rejects the decision entirely.
     *
     * @param reason Human-readable rejection reason
     * @param errorCode Machine-readable error code
     */
    record Reject(String reason, String errorCode) implements GuardrailOutcome {}

    /**
     * Factory method for Accept outcome.
     */
    static Accept accept() {
        return new Accept();
    }

    /**
     * Factory method for Clarify outcome.
     */
    static Clarify clarify(String question, List<String> requiredFields) {
        return new Clarify(question, requiredFields);
    }

    /**
     * Factory method for Reject outcome.
     */
    static Reject reject(String reason, String errorCode) {
        return new Reject(reason, errorCode);
    }
}
