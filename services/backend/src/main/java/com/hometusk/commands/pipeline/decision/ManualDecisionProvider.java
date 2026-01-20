package com.hometusk.commands.pipeline.decision;

import com.hometusk.commands.domain.DecisionSource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Rule-based decision provider (Stage 1 behavior).
 * Always returns StartJob with confidence=1.0.
 *
 * Fallback provider when AI Platform is unavailable.
 */
@Component
public class ManualDecisionProvider implements DecisionProvider {

    private static final Logger log = LoggerFactory.getLogger(ManualDecisionProvider.class);

    @Override
    public DecisionResult decide(DecisionContext context) {
        return switch (context.commandType()) {
            case CREATE_TASK -> decideCreateTask(context);
            case COMPLETE_TASK -> decideCompleteTask(context);
        };
    }

    private DecisionResult decideCreateTask(DecisionContext context) {
        Map<String, Object> payload = context.payload();
        Object assigneeIdRaw = payload.get("assigneeId");
        UUID assigneeId = parseUuid(assigneeIdRaw);

        // Rule: If no assignee specified, assign to initiator
        if (assigneeId == null) {
            log.debug("No assignee specified, assigning to initiator: {}", context.requesterId());
            assigneeId = context.requesterId();
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("title", payload.get("title"));
        parameters.put("description", payload.get("description"));
        parameters.put("assigneeId", assigneeId);
        parameters.put("zoneId", parseUuid(payload.get("zoneId")));
        parameters.put("deadline", payload.get("deadline"));

        return new DecisionResult.StartJob(
                DecisionSource.MANUAL,
                BigDecimal.ONE,
                List.of(new DecisionResult.StartJob.ProposedAction("create_task", parameters)));
    }

    private DecisionResult decideCompleteTask(DecisionContext context) {
        Map<String, Object> payload = context.payload();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("taskId", parseUuid(payload.get("taskId")));

        return new DecisionResult.StartJob(
                DecisionSource.MANUAL,
                BigDecimal.ONE,
                List.of(new DecisionResult.StartJob.ProposedAction("complete_task", parameters)));
    }

    @Override
    public DecisionSource getSource() {
        return DecisionSource.MANUAL;
    }

    @Override
    public boolean isAvailable() {
        return true; // Always available
    }

    private UUID parseUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String s) {
            return UUID.fromString(s);
        }
        return null;
    }
}
