package com.hometusk.commands.pipeline.decision;

import com.hometusk.commands.domain.DecisionSource;
import com.hometusk.commands.pipeline.decision.client.AiDecisionRequest;
import com.hometusk.commands.pipeline.decision.client.AiDecisionResponse;
import com.hometusk.commands.pipeline.decision.client.AiDecisionResponseMapper;
import com.hometusk.commands.pipeline.decision.client.AiPlatformClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * External AI Platform decision provider.
 * Calls external AI service via HTTP client.
 *
 * Only instantiated when decision.provider=aiplatform.
 */
@Component
@ConditionalOnProperty(name = "decision.provider", havingValue = "aiplatform")
public class AiPlatformDecisionProvider implements DecisionProvider {

    private static final Logger log = LoggerFactory.getLogger(AiPlatformDecisionProvider.class);

    private final AiPlatformClient client;
    private final AiDecisionResponseMapper mapper;

    public AiPlatformDecisionProvider(AiPlatformClient client, AiDecisionResponseMapper mapper) {
        this.client = client;
        this.mapper = mapper;
        log.info("AI Platform decision provider initialized");
    }

    @Override
    public DecisionResult decide(DecisionContext context) {
        log.debug("Requesting AI decision: commandId={}, correlationId={}", context.commandId(), context.correlationId());

        AiDecisionRequest request = AiDecisionRequest.from(context);
        AiDecisionResponse response = client.requestDecision(request);

        return mapper.toDecisionResult(response);
    }

    @Override
    public DecisionSource getSource() {
        return DecisionSource.AI_PLATFORM;
    }

    @Override
    public boolean isAvailable() {
        return client.healthCheck();
    }
}
