package com.hometusk.commands.pipeline.guardrails;

import com.hometusk.commands.pipeline.decision.DecisionResult.ProposedAction;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Policy that limits the number of open tasks per assignee.
 *
 * <p>If an assignee already has reached the maximum number of open tasks,
 * this policy requests clarification from the user to reassign the task.
 *
 * <p>Configuration: guardrails.max-open-tasks-per-assignee (default: 10)
 */
@Component
public class MaxOpenTasksPerAssigneePolicy implements GuardrailPolicy {

    private static final Logger log = LoggerFactory.getLogger(MaxOpenTasksPerAssigneePolicy.class);
    private static final String NAME = "MaxOpenTasksPerAssignee";

    private final GuardrailsConfig config;

    public MaxOpenTasksPerAssigneePolicy(GuardrailsConfig config) {
        this.config = config;
    }

    @Override
    public GuardrailOutcome evaluate(GuardrailContext context) {
        int maxTasks = config.getMaxOpenTasksPerAssignee();

        for (ProposedAction action : context.decision().actions()) {
            if (!"create_task".equals(action.actionType())) {
                continue;
            }

            // Check if assigneeId is specified in the action
            Object assigneeIdObj = action.parameters().get("assigneeId");
            if (assigneeIdObj == null) {
                continue; // No assignee specified, skip
            }

            UUID assigneeId;
            try {
                assigneeId = UUID.fromString(assigneeIdObj.toString());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid assigneeId format: {}", assigneeIdObj);
                continue;
            }

            // Get current open task count for this assignee
            int currentCount = context.householdSnapshot().getOpenTaskCount(assigneeId);

            if (currentCount >= maxTasks) {
                HouseholdSnapshot.MemberInfo member = context.householdSnapshot().findMember(assigneeId);
                String memberName = member != null ? member.name() : "выбранный пользователь";

                log.info(
                        "MaxOpenTasks policy triggered: assigneeId={}, currentCount={}, maxTasks={}",
                        assigneeId,
                        currentCount,
                        maxTasks);

                return GuardrailOutcome.clarify(
                        String.format(
                                "У пользователя %s уже %d открытых задач (максимум: %d). "
                                        + "Назначить задачу на другого участника?",
                                memberName, currentCount, maxTasks),
                        List.of("assigneeId"));
            }
        }

        return GuardrailOutcome.accept();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrder() {
        return 200; // Run after ZoneOwnerFirstPolicy
    }
}
