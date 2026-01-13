package com.hometusk.commands.pipeline.decision;

import com.hometusk.commands.domain.DecisionSource;
import com.hometusk.commands.metrics.DecisionMetrics;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Selects the appropriate DecisionProvider based on configuration.
 * Handles fallback when primary provider is unavailable.
 *
 * Simple selection: if-else based on config, no factory pattern.
 */
@Component
public class DecisionProviderSelector {

    private static final Logger log = LoggerFactory.getLogger(DecisionProviderSelector.class);

    private final ManualDecisionProvider manualProvider;
    private final Optional<AiPlatformDecisionProvider> aiPlatformProvider;
    private final String configuredProvider;
    private final boolean fallbackEnabled;
    private final DecisionMetrics metrics;

    public DecisionProviderSelector(
            ManualDecisionProvider manualProvider,
            @Autowired(required = false) AiPlatformDecisionProvider aiPlatformProvider,
            @Value("${decision.provider:manual}") String configuredProvider,
            @Value("${decision.fallback.enabled:true}") boolean fallbackEnabled,
            DecisionMetrics metrics) {
        this.manualProvider = manualProvider;
        this.aiPlatformProvider = Optional.ofNullable(aiPlatformProvider);
        this.configuredProvider = configuredProvider;
        this.fallbackEnabled = fallbackEnabled;
        this.metrics = metrics;

        log.info("DecisionProviderSelector initialized: provider={}, fallback={}, aiPlatformAvailable={}",
                configuredProvider, fallbackEnabled, this.aiPlatformProvider.isPresent());
    }

    /**
     * Makes a decision using the configured provider.
     * Falls back to manual if AI platform is unavailable and fallback is enabled.
     *
     * @param context Decision context
     * @return Decision result (may have FALLBACK source if degraded)
     */
    public DecisionResult decide(DecisionContext context) {
        long startTime = System.currentTimeMillis();
        DecisionResult result;
        String actualSource;

        if ("aiplatform".equals(configuredProvider)) {
            result = decideWithAiPlatform(context);
            // Determine actual source based on result
            actualSource = result.source() == DecisionSource.FALLBACK ? "fallback" : "aiplatform";
        } else {
            result = manualProvider.decide(context);
            actualSource = "manual";
        }

        long duration = System.currentTimeMillis() - startTime;
        metrics.recordDecisionLatency(actualSource, duration);
        metrics.recordDecisionSource(actualSource);

        return result;
    }

    private DecisionResult decideWithAiPlatform(DecisionContext context) {
        if (aiPlatformProvider.isEmpty()) {
            log.warn("AI Platform provider not configured, falling back to manual");
            return decideWithFallback(context, "provider_not_configured");
        }

        AiPlatformDecisionProvider provider = aiPlatformProvider.get();
        if (!provider.isAvailable()) {
            log.warn("AI Platform provider unavailable, falling back to manual");
            return decideWithFallback(context, "provider_unavailable");
        }

        try {
            return provider.decide(context);
        } catch (Exception e) {
            log.error("AI Platform decision failed, falling back to manual", e);
            return decideWithFallback(context, "provider_error");
        }
    }

    private DecisionResult decideWithFallback(DecisionContext context, String reason) {
        if (!fallbackEnabled) {
            throw new DecisionProviderUnavailableException(
                    "AI Platform unavailable and fallback disabled: " + reason);
        }

        DecisionResult manualResult = manualProvider.decide(context);

        // Convert to fallback source
        if (manualResult instanceof DecisionResult.StartJob startJob) {
            return new DecisionResult.StartJob(
                    DecisionSource.FALLBACK, startJob.confidence(), startJob.actions());
        }
        // Clarify and Reject shouldn't happen from manual provider, but handle anyway
        return manualResult;
    }

    /**
     * Returns the currently configured provider name.
     */
    public String getConfiguredProvider() {
        return configuredProvider;
    }

    /**
     * Returns true if fallback is enabled.
     */
    public boolean isFallbackEnabled() {
        return fallbackEnabled;
    }

    /**
     * Returns true if the primary provider is available.
     */
    public boolean isPrimaryAvailable() {
        if ("manual".equals(configuredProvider)) {
            return manualProvider.isAvailable();
        }
        return aiPlatformProvider.map(DecisionProvider::isAvailable).orElse(false);
    }
}
