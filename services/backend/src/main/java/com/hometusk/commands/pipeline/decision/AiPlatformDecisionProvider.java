package com.hometusk.commands.pipeline.decision;

import com.hometusk.commands.domain.DecisionSource;
import com.hometusk.commands.pipeline.decision.client.AiDecisionRequest;
import com.hometusk.commands.pipeline.decision.client.AiDecisionResponse;
import com.hometusk.commands.pipeline.decision.client.AiDecisionResponseMapper;
import com.hometusk.commands.pipeline.decision.client.AiPlatformClient;
import com.hometusk.commands.pipeline.decision.client.AiResponseSchemaValidator;
import com.hometusk.commands.pipeline.decision.client.AiResponseSchemaValidator.ValidationResult;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * External AI Platform decision provider.
 * Calls external AI service via HTTP client.
 *
 * <p>Validates AI response against JSON Schema before mapping to DecisionResult.
 * Per CLAUDE.md Rule 1: "AI output MUST be schema-validated before use"
 *
 * Only instantiated when decision.provider=aiplatform.
 */
@Component
@ConditionalOnProperty(name = "decision.provider", havingValue = "aiplatform")
public class AiPlatformDecisionProvider implements DecisionProvider {

    private static final Logger log = LoggerFactory.getLogger(AiPlatformDecisionProvider.class);

    private final AiPlatformClient client;
    private final AiDecisionResponseMapper mapper;
    private final AiResponseSchemaValidator schemaValidator;

    public AiPlatformDecisionProvider(
            AiPlatformClient client, AiDecisionResponseMapper mapper, AiResponseSchemaValidator schemaValidator) {
        this.client = client;
        this.mapper = mapper;
        this.schemaValidator = schemaValidator;
        log.info("AI Platform decision provider initialized with schema validation");
    }

    @Override
    public DecisionResult decide(DecisionContext context) {
        log.debug(
                "Requesting AI decision: commandId={}, correlationId={}", context.commandId(), context.correlationId());

        AiDecisionRequest request = AiDecisionRequest.from(context);
        AiDecisionResponse response = client.requestDecision(request);

        // Validate response against JSON Schema before mapping
        ValidationResult validationResult = schemaValidator.validate(response);
        if (!validationResult.valid()) {
            log.error(
                    "AI response schema validation failed: commandId={}, errors={}",
                    context.commandId(),
                    validationResult.getErrorSummary());

            // Return Reject with validation error details
            return new DecisionResult.Reject(
                    DecisionSource.AI_PLATFORM,
                    BigDecimal.ZERO,
                    response.decisionId(),
                    serializeResponseForLog(response),
                    "AI response failed schema validation: " + validationResult.getErrorSummary(),
                    "AI_RESPONSE_INVALID");
        }

        log.debug("AI response schema validation passed: commandId={}", context.commandId());
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

    private String serializeResponseForLog(AiDecisionResponse response) {
        try {
            return "{\"decisionId\":\"" + response.decisionId() + "\",\"type\":\"" + response.type() + "\"}";
        } catch (Exception e) {
            return "{\"error\":\"Unable to serialize response\"}";
        }
    }
}
