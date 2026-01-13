package com.hometusk.commands.pipeline.guardrails;

import com.hometusk.commands.pipeline.decision.DecisionContext;
import com.hometusk.commands.pipeline.decision.DecisionResult;
import java.util.UUID;

/**
 * Context provided to guardrail policies for evaluation.
 *
 * <p>Contains:
 * <ul>
 *   <li>The AI decision to evaluate (StartJob only)</li>
 *   <li>The original command context</li>
 *   <li>Household snapshot for policy checks</li>
 * </ul>
 */
public record GuardrailContext(
        DecisionResult.StartJob decision, DecisionContext originalContext, HouseholdSnapshot householdSnapshot) {

    /**
     * Convenience method to get the household ID.
     */
    public UUID householdId() {
        return originalContext.householdId();
    }

    /**
     * Convenience method to get the command ID.
     */
    public UUID commandId() {
        return originalContext.commandId();
    }

    /**
     * Convenience method to get the correlation ID.
     */
    public UUID correlationId() {
        return originalContext.correlationId();
    }

    /**
     * Convenience method to get the requester ID.
     */
    public UUID requesterId() {
        return originalContext.requesterId();
    }

    /**
     * Checks if the household snapshot is complete.
     * Guardrails should handle incomplete snapshots appropriately.
     */
    public boolean hasCompleteContext() {
        return householdSnapshot != null && householdSnapshot.complete();
    }
}
