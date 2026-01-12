package com.hometusk.commands.domain;

/**
 * Source of the decision.
 *
 * Stage 1: RULE (rule-based, deprecated alias for MANUAL)
 * Stage 2: MANUAL (rule-based), AI_PLATFORM (external AI)
 */
public enum DecisionSource {
    /** @deprecated Use MANUAL instead */
    @Deprecated
    RULE,

    /** Rule-based decision (Stage 1 logic, Stage 2 fallback) */
    MANUAL,

    /** External AI Platform decision */
    AI_PLATFORM,

    /** Fallback when AI Platform unavailable */
    FALLBACK,

    /** User confirmed or modified the AI suggestion */
    USER_OVERRIDE
}
