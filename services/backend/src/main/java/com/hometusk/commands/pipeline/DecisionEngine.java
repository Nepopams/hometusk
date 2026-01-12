package com.hometusk.commands.pipeline;

import com.hometusk.commands.domain.DecisionSource;
import com.hometusk.commands.dto.CompleteTaskPayload;
import com.hometusk.commands.dto.CreateTaskPayload;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Makes decisions based on command payload.
 *
 * Stage 1: Rule-based decisions only (confidence = 1.0)
 * Stage 2+: Will integrate with AI/LLM for intent resolution
 */
@Component
public class DecisionEngine {

    private static final Logger log = LoggerFactory.getLogger(DecisionEngine.class);

    /**
     * Makes a decision for create_task command.
     * Stage 1: Simple rule - if no assignee specified, assign to initiator.
     */
    public CreateTaskDecision decideCreateTask(CreateTaskPayload payload, UUID initiatorId) {
        UUID assigneeId = payload.assigneeId();

        // Rule: If no assignee specified, assign to initiator
        if (assigneeId == null) {
            log.debug("No assignee specified, assigning to initiator: {}", initiatorId);
            assigneeId = initiatorId;
        }

        return new CreateTaskDecision(
                payload.title(),
                payload.description(),
                assigneeId,
                payload.zoneId(),
                payload.deadline(),
                DecisionSource.RULE,
                BigDecimal.ONE // Stage 1: always 1.0 for rule-based
                );
    }

    /**
     * Makes a decision for complete_task command.
     * Stage 1: Simple pass-through, task already validated.
     */
    public CompleteTaskDecision decideCompleteTask(CompleteTaskPayload payload) {
        return new CompleteTaskDecision(payload.taskId(), DecisionSource.RULE, BigDecimal.ONE);
    }

    public record CreateTaskDecision(
            String title,
            String description,
            UUID assigneeId,
            UUID zoneId,
            java.time.Instant deadline,
            DecisionSource source,
            BigDecimal confidence) {}

    public record CompleteTaskDecision(UUID taskId, DecisionSource source, BigDecimal confidence) {}
}
