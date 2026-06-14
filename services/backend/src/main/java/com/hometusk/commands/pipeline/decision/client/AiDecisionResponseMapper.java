package com.hometusk.commands.pipeline.decision.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.commands.domain.DecisionSource;
import com.hometusk.commands.pipeline.decision.DecisionResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Maps upstream AI Platform decisions to internal HomeTusk DecisionResult values.
 */
@Component
public class AiDecisionResponseMapper {

    private static final Logger log = LoggerFactory.getLogger(AiDecisionResponseMapper.class);

    private static final String STATUS_ERROR = "error";
    private static final String ACTION_START_JOB = "start_job";
    private static final String ACTION_PROPOSE_CREATE_TASK = "propose_create_task";
    private static final String ACTION_PROPOSE_ADD_SHOPPING_ITEM = "propose_add_shopping_item";
    private static final String ACTION_CLARIFY = "clarify";

    private final ObjectMapper objectMapper;

    public AiDecisionResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DecisionResult toDecisionResult(AiDecisionResponse response) {
        String rawPayload = toJson(response);

        if (STATUS_ERROR.equals(response.status())) {
            return mapToReject(response, rawPayload, "AI_PLATFORM_ERROR");
        }

        return switch (response.action()) {
            case ACTION_START_JOB -> mapStartJob(response, rawPayload);
            case ACTION_PROPOSE_CREATE_TASK -> mapSingleAction(
                    response, rawPayload, response.action(), response.payload());
            case ACTION_PROPOSE_ADD_SHOPPING_ITEM -> mapSingleAction(
                    response, rawPayload, response.action(), response.payload());
            case ACTION_CLARIFY -> mapClarify(response, rawPayload);
            default -> unknownDecisionAction(response, rawPayload);
        };
    }

    private DecisionResult mapStartJob(AiDecisionResponse response, String rawPayload) {
        List<DecisionResult.StartJob.ProposedAction> actions = new ArrayList<>();
        Object rawActions = payload(response).get("proposed_actions");

        if (rawActions instanceof List<?> proposedActions) {
            for (Object rawAction : proposedActions) {
                DecisionResult.StartJob.ProposedAction mapped = mapProposedAction(rawAction);
                if (mapped != null) {
                    actions.add(mapped);
                }
            }
        }

        if (actions.isEmpty()) {
            log.warn(
                    "AI Platform start_job did not include executable proposed actions: decisionId={}",
                    response.decisionId());
            return new DecisionResult.Clarify(
                    DecisionSource.AI_PLATFORM,
                    confidenceOrZero(response),
                    response.decisionUuidOrNull(),
                    rawPayload,
                    "AI Platform did not return an executable action. Please rephrase the command.",
                    List.of(),
                    Map.of("reason", "missing_proposed_actions"));
        }

        return new DecisionResult.StartJob(
                DecisionSource.AI_PLATFORM,
                confidenceOrZero(response),
                response.decisionUuidOrNull(),
                rawPayload,
                actions);
    }

    private DecisionResult mapSingleAction(
            AiDecisionResponse response, String rawPayload, String upstreamAction, Map<String, Object> payload) {
        DecisionResult.StartJob.ProposedAction action =
                mapProposedAction(Map.of("action", upstreamAction, "payload", payload != null ? payload : Map.of()));
        if (action == null) {
            return unknownDecisionAction(response, rawPayload);
        }

        return new DecisionResult.StartJob(
                DecisionSource.AI_PLATFORM,
                confidenceOrZero(response),
                response.decisionUuidOrNull(),
                rawPayload,
                List.of(action));
    }

    private DecisionResult.StartJob.ProposedAction mapProposedAction(Object rawAction) {
        if (!(rawAction instanceof Map<?, ?> actionMap)) {
            return null;
        }

        String upstreamAction = stringValue(actionMap.get("action"));
        Map<String, Object> payload = mapValue(actionMap.get("payload"));

        return switch (upstreamAction) {
            case ACTION_PROPOSE_CREATE_TASK -> new DecisionResult.StartJob.ProposedAction(
                    "create_task", mapTaskPayload(payload));
            case ACTION_PROPOSE_ADD_SHOPPING_ITEM -> new DecisionResult.StartJob.ProposedAction(
                    "add_shopping_item", mapShoppingItemPayload(payload));
            default -> null;
        };
    }

    private Map<String, Object> mapTaskPayload(Map<String, Object> payload) {
        Map<String, Object> task = mapValue(payload.get("task"));
        Map<String, Object> params = new LinkedHashMap<>();

        copyIfPresent(task, params, "title", "title");
        copyIfPresent(task, params, "description", "description");
        copyIfPresent(task, params, "assignee_id", "assigneeId");
        copyIfPresent(task, params, "zone_id", "zoneId");
        copyIfPresent(task, params, "due", "deadline");

        return params;
    }

    private Map<String, Object> mapShoppingItemPayload(Map<String, Object> payload) {
        Map<String, Object> item = mapValue(payload.get("item"));
        Map<String, Object> params = new LinkedHashMap<>();

        copyIfPresent(item, params, "name", "name");
        copyIfPresent(item, params, "quantity", "quantity");
        copyIfPresent(item, params, "unit", "unit");
        copyIfPresent(item, params, "list_id", "listId");

        return params;
    }

    private DecisionResult.Clarify mapClarify(AiDecisionResponse response, String rawPayload) {
        Map<String, Object> payload = payload(response);
        String question = stringValue(payload.get("question"));
        if (question == null || question.isBlank()) {
            question = "Please add more details to complete the command.";
        }

        return new DecisionResult.Clarify(
                DecisionSource.AI_PLATFORM,
                confidenceOrZero(response),
                response.decisionUuidOrNull(),
                rawPayload,
                question,
                stringList(payload.get("missing_fields")),
                suggestions(payload));
    }

    private DecisionResult.Reject mapToReject(AiDecisionResponse response, String rawPayload, String errorCode) {
        return new DecisionResult.Reject(
                DecisionSource.AI_PLATFORM,
                confidenceOrZero(response),
                response.decisionUuidOrNull(),
                rawPayload,
                response.explanation(),
                errorCode);
    }

    private DecisionResult unknownDecisionAction(AiDecisionResponse response, String rawPayload) {
        log.error(
                "Unknown decision action from AI Platform: action={}, decisionId={}",
                response.action(),
                response.decisionId());

        return new DecisionResult.Reject(
                DecisionSource.AI_PLATFORM,
                BigDecimal.ZERO,
                response.decisionUuidOrNull(),
                rawPayload,
                "Unknown AI Platform decision action: " + response.action(),
                "UNKNOWN_DECISION_ACTION");
    }

    private Map<String, Object> suggestions(Map<String, Object> payload) {
        Object options = payload.get("options");
        if (options == null) {
            return Map.of();
        }
        return Map.of("options", options);
    }

    private Map<String, Object> payload(AiDecisionResponse response) {
        return response.payload() != null ? response.payload() : Map.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(Object::toString).toList();
    }

    private void copyIfPresent(
            Map<String, Object> source, Map<String, Object> target, String sourceKey, String targetKey) {
        Object value = source.get(sourceKey);
        if (value != null) {
            target.put(targetKey, value);
        }
    }

    private String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    private BigDecimal confidenceOrZero(AiDecisionResponse response) {
        return response.confidence() != null ? response.confidence() : BigDecimal.ZERO;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
