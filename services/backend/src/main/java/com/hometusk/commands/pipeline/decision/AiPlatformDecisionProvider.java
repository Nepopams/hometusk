package com.hometusk.commands.pipeline.decision;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.commands.domain.DecisionSource;
import com.hometusk.commands.pipeline.decision.client.AiDecisionRequest;
import com.hometusk.commands.pipeline.decision.client.AiDecisionResponse;
import com.hometusk.commands.pipeline.decision.client.AiDecisionResponseMapper;
import com.hometusk.commands.pipeline.decision.client.AiPlatformClient;
import com.hometusk.commands.pipeline.decision.client.AiPlatformClient.AiDecisionClientResponse;
import com.hometusk.commands.pipeline.decision.client.AiResponseSchemaValidator;
import com.hometusk.commands.pipeline.decision.client.AiResponseSchemaValidator.ValidationResult;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
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
    private final ObjectMapper objectMapper;

    public AiPlatformDecisionProvider(
            AiPlatformClient client,
            AiDecisionResponseMapper mapper,
            AiResponseSchemaValidator schemaValidator,
            ObjectMapper objectMapper) {
        this.client = client;
        this.mapper = mapper;
        this.schemaValidator = schemaValidator;
        this.objectMapper = objectMapper;
        log.info("AI Platform decision provider initialized with schema validation");
    }

    @Override
    public DecisionResult decide(DecisionContext context) {
        log.debug(
                "Requesting AI decision: commandId={}, correlationId={}", context.commandId(), context.correlationId());

        AiDecisionRequest request = AiDecisionRequest.from(context);
        AiDecisionClientResponse clientResponse = client.requestDecision(request);
        String rawPayload = clientResponse.rawPayload();

        // Validate response against JSON Schema before mapping
        ValidationResult validationResult = schemaValidator.validateRaw(rawPayload);
        if (!validationResult.valid()) {
            String auditableRawPayload = auditableRawPayload(rawPayload, validationResult);
            log.error(
                    "AI response schema validation failed: commandId={}, errors={}",
                    context.commandId(),
                    validationResult.getErrorSummary());

            // Return Reject with validation error details
            return new DecisionResult.Reject(
                    DecisionSource.AI_PLATFORM,
                    BigDecimal.ZERO,
                    decisionUuidOrNull(rawPayload),
                    auditableRawPayload,
                    "AI response failed schema validation: " + validationResult.getErrorSummary(),
                    "AI_RESPONSE_INVALID");
        }

        AiDecisionResponse response = parseValidatedResponse(rawPayload);
        if (response == null) {
            return new DecisionResult.Reject(
                    DecisionSource.AI_PLATFORM,
                    BigDecimal.ZERO,
                    decisionUuidOrNull(rawPayload),
                    rawPayload,
                    "AI response failed schema validation: unreadable after validation",
                    "AI_RESPONSE_INVALID");
        }

        log.debug(
                "Received valid AI decision: decisionId={}, status={}, action={}",
                response.decisionId(),
                response.status(),
                response.action());
        log.debug("AI response schema validation passed: commandId={}", context.commandId());
        return mapper.toDecisionResult(response, rawPayload);
    }

    private AiDecisionResponse parseValidatedResponse(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, AiDecisionResponse.class);
        } catch (Exception e) {
            log.error("AI response passed schema validation but could not be parsed into DTO", e);
            return null;
        }
    }

    private String auditableRawPayload(String rawPayload, ValidationResult validationResult) {
        try {
            objectMapper.readTree(rawPayload);
            return rawPayload;
        } catch (Exception ignored) {
            try {
                return objectMapper.writeValueAsString(Map.of(
                        "raw_payload_format",
                        "invalid_json",
                        "raw_payload",
                        rawPayload != null ? rawPayload : "",
                        "validation_error",
                        validationResult.getErrorSummary()));
            } catch (Exception e) {
                log.warn("Failed to encode invalid AI response for DecisionLog", e);
                return "{\"raw_payload_format\":\"invalid_json\",\"raw_payload\":\"\",\"validation_error\":\"unavailable\"}";
            }
        }
    }

    private UUID decisionUuidOrNull(String rawPayload) {
        try {
            String decisionId =
                    objectMapper.readTree(rawPayload).path("decision_id").asText(null);
            if (decisionId == null || decisionId.isBlank()) {
                return null;
            }
            return UUID.fromString(decisionId);
        } catch (Exception ignored) {
            return null;
        }
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
