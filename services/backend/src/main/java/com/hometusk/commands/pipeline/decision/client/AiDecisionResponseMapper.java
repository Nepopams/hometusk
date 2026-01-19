package com.hometusk.commands.pipeline.decision.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.commands.domain.DecisionSource;
import com.hometusk.commands.pipeline.decision.DecisionResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Maps AI Platform (upstream) response to HomeTusk DecisionResult.
 *
 * <p>Handles all upstream decision types with safe degradation:
 * <ul>
 *   <li>start_job → StartJob (full support)</li>
 *   <li>propose_create_task → StartJob (mapped, execute immediately)</li>
 *   <li>propose_add_shopping_item → Clarify (unsupported action)</li>
 *   <li>clarify → Clarify (full support)</li>
 *   <li>reject → Reject (full support)</li>
 *   <li>unknown → Reject (safe degradation)</li>
 * </ul>
 *
 * <p>See: docs/integration/ai-platform/v1/mapping/hometusk-to-upstream.md
 */
@Component
public class AiDecisionResponseMapper {

    private static final Logger log = LoggerFactory.getLogger(AiDecisionResponseMapper.class);

    // Supported upstream decision types
    private static final String TYPE_START_JOB = "start_job";
    private static final String TYPE_PROPOSE_CREATE_TASK = "propose_create_task";
    private static final String TYPE_PROPOSE_ADD_SHOPPING_ITEM = "propose_add_shopping_item";
    private static final String TYPE_CLARIFY = "clarify";
    private static final String TYPE_REJECT = "reject";

    // Supported action types
    private static final String ACTION_CREATE_TASK = "create_task";
    private static final String ACTION_COMPLETE_TASK = "complete_task";
    private static final String ACTION_ADD_SHOPPING_ITEM = "add_shopping_item";

    private final ObjectMapper objectMapper;

    public AiDecisionResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Maps upstream AI Platform response to HomeTusk DecisionResult.
     *
     * <p>Safe degradation: unknown/unsupported types return Clarify or Reject,
     * never throw exceptions.
     *
     * @param response AI Platform response (upstream format)
     * @return Corresponding DecisionResult
     */
    public DecisionResult toDecisionResult(AiDecisionResponse response) {
        String rawPayload = toJson(response);

        return switch (response.type()) {
            case TYPE_START_JOB -> mapToStartJob(response, rawPayload);
            case TYPE_PROPOSE_CREATE_TASK -> mapProposeCreateTask(response, rawPayload);
            case TYPE_PROPOSE_ADD_SHOPPING_ITEM -> mapProposeAddShoppingItem(response, rawPayload);
            case TYPE_CLARIFY -> mapToClarify(response, rawPayload);
            case TYPE_REJECT -> mapToReject(response, rawPayload);
            default -> unknownDecisionType(response, rawPayload);
        };
    }

    private DecisionResult mapToStartJob(AiDecisionResponse response, String rawPayload) {
        // Filter to only supported action types, return Clarify if any unsupported
        if (response.actions() != null && hasUnsupportedActions(response.actions())) {
            log.warn("start_job contains unsupported action types, filtering: decisionId={}",
                    response.decisionId());
        }

        List<DecisionResult.StartJob.ProposedAction> actions = response.actions() == null
                ? List.of()
                : response.actions().stream()
                        .filter(this::isSupportedAction)
                        .map(dto -> new DecisionResult.StartJob.ProposedAction(dto.actionType(), dto.parameters()))
                        .toList();

        // If all actions were filtered out, return Clarify
        if (actions.isEmpty() && response.actions() != null && !response.actions().isEmpty()) {
            log.warn("All actions filtered as unsupported, returning Clarify: decisionId={}",
                    response.decisionId());
            return new DecisionResult.Clarify(
                    DecisionSource.AI_PLATFORM,
                    BigDecimal.ZERO,
                    response.decisionId(),
                    rawPayload,
                    "Запрошенное действие пока не поддерживается. Попробуйте переформулировать команду.",
                    List.of(),
                    Map.of("reason", "all_actions_unsupported"));
        }

        return new DecisionResult.StartJob(
                DecisionSource.AI_PLATFORM, response.confidence(), response.decisionId(), rawPayload, actions);
    }

    /**
     * Maps propose_create_task to StartJob (execute immediately).
     * HomeTusk doesn't distinguish proposal from execution for tasks.
     */
    private DecisionResult mapProposeCreateTask(AiDecisionResponse response, String rawPayload) {
        log.debug("Mapping propose_create_task to StartJob: decisionId={}", response.decisionId());
        return mapToStartJob(response, rawPayload);
    }

    /**
     * Maps propose_add_shopping_item to StartJob (execute immediately, Stage 5).
     * HomeTusk doesn't distinguish proposal from execution for shopping items.
     */
    private DecisionResult mapProposeAddShoppingItem(AiDecisionResponse response, String rawPayload) {
        log.debug("Mapping propose_add_shopping_item to StartJob: decisionId={}", response.decisionId());
        return mapToStartJob(response, rawPayload);
    }

    /**
     * Safe degradation for unknown decision types.
     * Returns Reject with error code.
     */
    private DecisionResult unknownDecisionType(AiDecisionResponse response, String rawPayload) {
        log.error("Unknown decision type from AI Platform: type={}, decisionId={}",
                response.type(), response.decisionId());

        return new DecisionResult.Reject(
                DecisionSource.AI_PLATFORM,
                BigDecimal.ZERO,
                response.decisionId(),
                rawPayload,
                "Неизвестный тип решения от AI Platform: " + response.type(),
                "UNKNOWN_DECISION_TYPE");
    }

    private DecisionResult.Clarify mapToClarify(AiDecisionResponse response, String rawPayload) {
        return new DecisionResult.Clarify(
                DecisionSource.AI_PLATFORM,
                response.confidence(),
                response.decisionId(),
                rawPayload,
                response.question(),
                response.requiredFields() != null ? response.requiredFields() : List.of(),
                response.suggestions() != null ? response.suggestions() : Map.of());
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

    /**
     * Checks if action type is supported by HomeTusk.
     */
    private boolean isSupportedAction(AiDecisionResponse.ProposedActionDto action) {
        return ACTION_CREATE_TASK.equals(action.actionType())
                || ACTION_COMPLETE_TASK.equals(action.actionType())
                || ACTION_ADD_SHOPPING_ITEM.equals(action.actionType());
    }

    /**
     * Checks if any actions in the list are unsupported.
     */
    private boolean hasUnsupportedActions(List<AiDecisionResponse.ProposedActionDto> actions) {
        return actions.stream().anyMatch(a -> !isSupportedAction(a));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
