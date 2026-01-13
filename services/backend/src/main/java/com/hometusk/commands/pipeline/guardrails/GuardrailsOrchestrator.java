package com.hometusk.commands.pipeline.guardrails;

import com.hometusk.commands.pipeline.ContextBuilder;
import com.hometusk.commands.pipeline.decision.DecisionContext;
import com.hometusk.commands.pipeline.decision.DecisionResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Orchestrates guardrail policy evaluation.
 *
 * <p>Evaluates all registered policies in order and aggregates their outcomes:
 * <ul>
 *   <li>Any REJECT → final REJECT</li>
 *   <li>Any CLARIFY (if no REJECT) → final CLARIFY</li>
 *   <li>All ACCEPT/MODIFY → apply modifications and PROCEED</li>
 * </ul>
 *
 * <p>CRITICAL: If household context is incomplete, the orchestrator
 * deterministically returns CLARIFY or REJECT. We do NOT proceed
 * with incomplete data - this is a fail-safe approach.
 */
@Component
public class GuardrailsOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(GuardrailsOrchestrator.class);
    private static final String CONTEXT_INCOMPLETE_POLICY = "ContextCompleteness";

    private final List<GuardrailPolicy> policies;
    private final GuardrailsConfig config;
    private final ContextBuilder contextBuilder;

    public GuardrailsOrchestrator(
            List<GuardrailPolicy> policies, GuardrailsConfig config, ContextBuilder contextBuilder) {
        // Sort policies by order
        this.policies = policies.stream()
                .sorted(Comparator.comparingInt(GuardrailPolicy::getOrder))
                .toList();
        this.config = config;
        this.contextBuilder = contextBuilder;

        log.info(
                "Guardrails orchestrator initialized with {} policies: {}",
                policies.size(),
                policies.stream().map(GuardrailPolicy::getName).toList());
    }

    /**
     * Evaluates the decision against all guardrail policies.
     *
     * @param decision The AI decision (StartJob) to evaluate
     * @param originalContext The original command context
     * @return The aggregated guardrail result
     */
    public GuardrailResult evaluate(DecisionResult.StartJob decision, DecisionContext originalContext) {
        if (!config.isEnabled()) {
            log.debug("Guardrails disabled, proceeding: correlationId={}", originalContext.correlationId());
            return new GuardrailResult.Proceed(decision, List.of("guardrails_disabled"));
        }

        log.debug(
                "Evaluating guardrails: correlationId={}, policies={}",
                originalContext.correlationId(),
                policies.size());

        // Build household snapshot for policy evaluation
        HouseholdSnapshot snapshot = contextBuilder.buildSnapshot(
                originalContext.householdId(), originalContext.correlationId());

        // CRITICAL: Check context completeness first
        if (!snapshot.complete()) {
            log.warn(
                    "Household context incomplete, cannot proceed safely: householdId={}, correlationId={}",
                    originalContext.householdId(),
                    originalContext.correlationId());

            // Fail-safe: request clarification instead of proceeding with incomplete data
            return new GuardrailResult.NeedsClarification(
                    "Unable to verify household context. Please try again or contact support.",
                    List.of(),
                    Map.of("reason", "context_incomplete"),
                    CONTEXT_INCOMPLETE_POLICY);
        }

        // Create guardrail context
        GuardrailContext context = new GuardrailContext(decision, originalContext, snapshot);

        // Evaluate all policies
        List<String> appliedPolicies = new ArrayList<>();
        List<String> modifications = new ArrayList<>();
        DecisionResult.StartJob currentDecision = decision;

        for (GuardrailPolicy policy : policies) {
            log.debug("Evaluating policy: {}", policy.getName());
            appliedPolicies.add(policy.getName());

            GuardrailOutcome outcome = policy.evaluate(context);

            switch (outcome) {
                case GuardrailOutcome.Accept ignored -> {
                    log.debug("Policy {} accepted", policy.getName());
                }

                case GuardrailOutcome.Modify modify -> {
                    log.info("Policy {} suggests modification: {}", policy.getName(), modify.reason());
                    modifications.add(policy.getName() + ": " + modify.reason());
                    // Update the decision with modified actions
                    currentDecision = new DecisionResult.StartJob(
                            currentDecision.source(),
                            currentDecision.confidence(),
                            currentDecision.externalDecisionId(),
                            currentDecision.rawPayload(),
                            modify.modifiedActions());
                    // Update context for next policy
                    context = new GuardrailContext(currentDecision, originalContext, snapshot);
                }

                case GuardrailOutcome.Clarify clarify -> {
                    log.info(
                            "Policy {} requests clarification: correlationId={}, question={}",
                            policy.getName(),
                            originalContext.correlationId(),
                            clarify.question());
                    return new GuardrailResult.NeedsClarification(
                            clarify.question(), clarify.requiredFields(), clarify.suggestions(), policy.getName());
                }

                case GuardrailOutcome.Reject reject -> {
                    log.warn(
                            "Policy {} rejected: correlationId={}, reason={}",
                            policy.getName(),
                            originalContext.correlationId(),
                            reject.reason());
                    return new GuardrailResult.Rejected(reject.reason(), reject.errorCode(), policy.getName());
                }
            }
        }

        log.debug(
                "All guardrails passed: correlationId={}, policies={}, modifications={}",
                originalContext.correlationId(),
                appliedPolicies.size(),
                modifications.size());

        return new GuardrailResult.Proceed(currentDecision, appliedPolicies, modifications);
    }
}
