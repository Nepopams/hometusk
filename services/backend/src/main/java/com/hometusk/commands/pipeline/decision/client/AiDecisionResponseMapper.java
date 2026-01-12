package com.hometusk.commands.pipeline.decision.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.commands.domain.DecisionSource;
import com.hometusk.commands.pipeline.decision.DecisionResult;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Maps AI Platform response to DecisionResult.
 */
@Component
public class AiDecisionResponseMapper {

    private final ObjectMapper objectMapper;

    public AiDecisionResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Maps AI Platform response to DecisionResult.
     *
     * @param response AI Platform response
     * @return Corresponding DecisionResult
     */
    public DecisionResult toDecisionResult(AiDecisionResponse response) {
        String rawPayload = toJson(response);

        return switch (response.type()) {
            case "start_job" -> mapToStartJob(response, rawPayload);
            case "clarify" -> mapToClarify(response, rawPayload);
            case "reject" -> mapToReject(response, rawPayload);
            default -> throw new IllegalArgumentException("Unknown decision type: " + response.type());
        };
    }

    private DecisionResult.StartJob mapToStartJob(AiDecisionResponse response, String rawPayload) {
        List<DecisionResult.StartJob.ProposedAction> actions = response.actions() == null
                ? List.of()
                : response.actions().stream()
                        .map(dto -> new DecisionResult.StartJob.ProposedAction(dto.actionType(), dto.parameters()))
                        .toList();

        return new DecisionResult.StartJob(
                DecisionSource.AI_PLATFORM, response.confidence(), response.decisionId(), rawPayload, actions);
    }

    private DecisionResult.Clarify mapToClarify(AiDecisionResponse response, String rawPayload) {
        return new DecisionResult.Clarify(
                DecisionSource.AI_PLATFORM,
                response.confidence(),
                response.decisionId(),
                rawPayload,
                response.question(),
                response.requiredFields() != null ? response.requiredFields() : List.of(),
                response.suggestions() != null ? response.suggestions() : java.util.Map.of());
    }

    private DecisionResult.Reject mapToReject(AiDecisionResponse response, String rawPayload) {
        return new DecisionResult.Reject(
                DecisionSource.AI_PLATFORM,
                response.confidence(),
                response.decisionId(),
                rawPayload,
                response.reason(),
                response.errorCode());
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
