package com.hometusk.commands.pipeline.guardrails;

/**
 * Interface for guardrail policies that evaluate AI decisions.
 *
 * <p>Policies are composable and evaluated in order by GuardrailsOrchestrator.
 * Each policy can:
 * <ul>
 *   <li>Accept - approve the decision</li>
 *   <li>Modify - suggest changes to actions</li>
 *   <li>Clarify - request user input</li>
 *   <li>Reject - block the decision</li>
 * </ul>
 *
 * <p>Implementation guidelines:
 * <ul>
 *   <li>Policies MUST be deterministic (no AI calls)</li>
 *   <li>Policies MUST be fast (no external I/O)</li>
 *   <li>Policies SHOULD log their evaluation rationale</li>
 *   <li>Policies SHOULD use configuration for thresholds</li>
 * </ul>
 */
public interface GuardrailPolicy {

    /**
     * Evaluates the decision against this policy's rules.
     *
     * @param context The guardrail context containing decision and household state
     * @return The policy's outcome (Accept, Modify, Clarify, or Reject)
     */
    GuardrailOutcome evaluate(GuardrailContext context);

    /**
     * Returns the policy name for logging and tracing.
     */
    String getName();

    /**
     * Returns the evaluation order (lower = earlier).
     * Policies with lower order are evaluated first.
     */
    default int getOrder() {
        return 100; // Default middle priority
    }
}
