package com.hometusk.commands.domain;

/**
 * Source of the decision.
 * In Stage 1, always RULE (no AI/LLM).
 * Stage 2+ will add AI, USER_OVERRIDE.
 */
public enum DecisionSource {
    RULE,
    FALLBACK,
    USER_OVERRIDE
}
