package com.hometusk.commands.pipeline.decision;

import com.hometusk.commands.domain.DecisionSource;

/**
 * Abstraction for decision-making in the command pipeline.
 *
 * Implementations:
 * - ManualDecisionProvider: rule-based (Stage 1 logic)
 * - AiPlatformDecisionProvider: external AI Platform calls (Stage 2)
 */
public interface DecisionProvider {

    /**
     * Makes a decision for a command.
     *
     * @param context All context needed for decision-making
     * @return DecisionResult with one of: START_JOB, CLARIFY, REJECT
     */
    DecisionResult decide(DecisionContext context);

    /**
     * Returns the source identifier for this provider.
     */
    DecisionSource getSource();

    /**
     * Returns true if this provider is available (health check).
     */
    boolean isAvailable();
}
