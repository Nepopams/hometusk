package com.hometusk.commands.pipeline.decision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hometusk.commands.domain.CommandType;
import com.hometusk.commands.domain.DecisionSource;
import com.hometusk.commands.metrics.DecisionMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DecisionProviderSelectorTest {

    @Test
    void usesAiPlatformDecisionWithoutAvailabilityPreflight() {
        ManualDecisionProvider manualProvider = mock(ManualDecisionProvider.class);
        AiPlatformDecisionProvider aiPlatformProvider = mock(AiPlatformDecisionProvider.class);
        DecisionContext context = decisionContext();
        DecisionResult.StartJob aiResult =
                new DecisionResult.StartJob(DecisionSource.AI_PLATFORM, BigDecimal.ONE, List.of());
        when(aiPlatformProvider.decide(context)).thenReturn(aiResult);

        DecisionProviderSelector selector =
                new DecisionProviderSelector(manualProvider, aiPlatformProvider, "aiplatform", true, metrics());

        DecisionResult result = selector.decide(context);

        assertThat(result).isSameAs(aiResult);
        verify(aiPlatformProvider).decide(context);
        verify(aiPlatformProvider, never()).isAvailable();
        verifyNoInteractions(manualProvider);
    }

    @Test
    void fallsBackWhenAiPlatformDecisionFails() {
        ManualDecisionProvider manualProvider = mock(ManualDecisionProvider.class);
        AiPlatformDecisionProvider aiPlatformProvider = mock(AiPlatformDecisionProvider.class);
        DecisionContext context = decisionContext();
        DecisionResult.StartJob manualResult =
                new DecisionResult.StartJob(DecisionSource.MANUAL, BigDecimal.ONE, List.of());
        when(aiPlatformProvider.decide(context)).thenThrow(new RuntimeException("decision endpoint unavailable"));
        when(manualProvider.decide(context)).thenReturn(manualResult);

        DecisionProviderSelector selector =
                new DecisionProviderSelector(manualProvider, aiPlatformProvider, "aiplatform", true, metrics());

        DecisionResult result = selector.decide(context);

        assertThat(result.source()).isEqualTo(DecisionSource.FALLBACK);
        verify(aiPlatformProvider).decide(context);
        verify(aiPlatformProvider, never()).isAvailable();
        verify(manualProvider).decide(context);
    }

    private static DecisionContext decisionContext() {
        return DecisionContext.builder()
                .commandId(UUID.randomUUID())
                .correlationId(UUID.randomUUID())
                .commandType(CommandType.CREATE_TASK)
                .payload(Map.of("title", "Add milk to shopping list"))
                .requesterId(UUID.randomUUID())
                .householdId(UUID.randomUUID())
                .householdContext(Map.of())
                .build();
    }

    private static DecisionMetrics metrics() {
        return new DecisionMetrics(new SimpleMeterRegistry());
    }
}
