package com.hometusk.commands.pipeline.guardrails;

import com.hometusk.commands.pipeline.decision.DecisionResult;
import java.util.List;
import java.util.Map;

/**
 * Final result of guardrails evaluation from the orchestrator.
 *
 * <p>Aggregates outcomes from all policies into a single actionable result:
 * <ul>
 *   <li>Proceed - Continue with execution (possibly with modified actions)</li>
 *   <li>NeedsClarification - Ask user for more input</li>
 *   <li>Rejected - Cannot proceed with this decision</li>
 * </ul>
 */
public sealed interface GuardrailResult {

    /**
     * Proceed with execution - all policies accepted or modifications applied.
     *
     * @param decision The (possibly modified) decision to execute
     * @param appliedPolicies Names of policies that evaluated the decision
     * @param modifications Description of any modifications made
     */
    record Proceed(DecisionResult.StartJob decision, List<String> appliedPolicies, List<String> modifications)
            implements GuardrailResult {

        public Proceed(DecisionResult.StartJob decision, List<String> appliedPolicies) {
            this(decision, appliedPolicies, List.of());
        }
    }

    /**
     * User clarification needed before proceeding.
     *
     * @param question The question to ask the user
     * @param requiredFields Fields that need user input
     * @param suggestions Suggested values
     * @param triggeredPolicy The policy that requested clarification
     */
    record NeedsClarification(
            String question, List<String> requiredFields, Map<String, Object> suggestions, String triggeredPolicy)
            implements GuardrailResult {}

    /**
     * Decision rejected - cannot proceed.
     *
     * @param reason Human-readable reason
     * @param errorCode Machine-readable error code
     * @param triggeredPolicy The policy that rejected
     */
    record Rejected(String reason, String errorCode, String triggeredPolicy) implements GuardrailResult {}
}
