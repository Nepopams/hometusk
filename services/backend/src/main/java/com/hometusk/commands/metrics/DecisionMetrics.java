package com.hometusk.commands.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Metrics for decision pipeline observability.
 *
 * <p>Tracks:
 * <ul>
 *   <li>Decision latency by source (aiplatform, manual, fallback)</li>
 *   <li>Decision outcomes (applied, clarify, reject, degraded)</li>
 *   <li>Guardrails outcomes per policy (accept, clarify, reject, modify)</li>
 *   <li>AI Platform call latency and status</li>
 * </ul>
 *
 * <p>All metrics use standard Micrometer APIs and are exported to configured
 * monitoring systems (Prometheus, etc.).
 */
@Component
public class DecisionMetrics {

    private static final String DECISION_LATENCY = "decision.latency";
    private static final String DECISION_OUTCOME = "decision.outcome";
    private static final String DECISION_SOURCE = "decision.source";
    private static final String GUARDRAILS_OUTCOME = "guardrails.outcome";
    private static final String AI_PLATFORM_LATENCY = "aiplatform.latency";

    private final MeterRegistry registry;

    public DecisionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Record decision latency by source.
     *
     * @param source Decision source: "aiplatform", "manual", "fallback"
     * @param durationMs Duration in milliseconds
     */
    public void recordDecisionLatency(String source, long durationMs) {
        Timer.builder(DECISION_LATENCY)
                .tag("source", source)
                .description("Decision pipeline latency in milliseconds")
                .register(registry)
                .record(Duration.ofMillis(durationMs));
    }

    /**
     * Record decision outcome.
     *
     * @param outcome Outcome: "applied", "clarify", "reject", "degraded"
     */
    public void recordDecisionOutcome(String outcome) {
        Counter.builder(DECISION_OUTCOME)
                .tag("outcome", outcome)
                .description("Decision pipeline outcome count")
                .register(registry)
                .increment();
    }

    /**
     * Record decision source.
     *
     * @param source Source: "aiplatform", "manual", "fallback"
     */
    public void recordDecisionSource(String source) {
        Counter.builder(DECISION_SOURCE)
                .tag("source", source)
                .description("Decision source count")
                .register(registry)
                .increment();
    }

    /**
     * Record guardrail policy outcome.
     *
     * @param policyName Policy name (e.g., "Membership", "DeadlineSanity")
     * @param outcome Outcome: "accept", "clarify", "reject", "modify"
     */
    public void recordGuardrailOutcome(String policyName, String outcome) {
        Counter.builder(GUARDRAILS_OUTCOME)
                .tag("policy", policyName)
                .tag("outcome", outcome)
                .description("Guardrails policy outcome count")
                .register(registry)
                .increment();
    }

    /**
     * Record AI Platform call latency.
     *
     * @param endpoint Endpoint: "/decision" or "/decide"
     * @param status Status: "success", "error", "timeout"
     * @param durationMs Duration in milliseconds
     */
    public void recordAiPlatformLatency(String endpoint, String status, long durationMs) {
        Timer.builder(AI_PLATFORM_LATENCY)
                .tag("endpoint", endpoint)
                .tag("status", status)
                .description("AI Platform call latency in milliseconds")
                .register(registry)
                .record(Duration.ofMillis(durationMs));
    }
}
