package com.hometusk.commands.pipeline.guardrails;

import com.hometusk.commands.pipeline.guardrails.HouseholdSnapshot.MemberInfo;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Policy that validates assignee is a household member.
 *
 * <p>This policy ensures that tasks can only be assigned to users who are
 * members of the household. This prevents invalid references and maintains
 * data integrity.
 *
 * <p>Behavior:
 * <ul>
 *   <li>If assigneeId is specified and NOT a household member → REJECT</li>
 *   <li>If assigneeId is NULL or valid member → ACCEPT</li>
 * </ul>
 */
@Component
public class MembershipPolicy implements GuardrailPolicy {

    private static final Logger log = LoggerFactory.getLogger(MembershipPolicy.class);
    private static final String NAME = "Membership";

    @Override
    public GuardrailOutcome evaluate(GuardrailContext context) {
        for (var action : context.decision().actions()) {
            if (!"create_task".equals(action.actionType())) {
                continue;
            }

            // Check if assigneeId is specified
            Object assigneeIdObj = action.parameters().get("assigneeId");
            if (assigneeIdObj == null) {
                continue; // No assignee specified, nothing to validate
            }

            UUID assigneeId;
            try {
                assigneeId = UUID.fromString(assigneeIdObj.toString());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid assigneeId format: {}", assigneeIdObj);
                return GuardrailOutcome.reject(
                        "Assignee ID is not a valid UUID", "INVALID_ASSIGNEE_ID");
            }

            // Validate that assignee is a household member
            MemberInfo member = context.householdSnapshot().findMember(assigneeId);
            if (member == null) {
                log.warn(
                        "MembershipPolicy REJECT: assignee not found in household: assigneeId={}, householdId={}",
                        assigneeId,
                        context.householdSnapshot().householdId());
                return GuardrailOutcome.reject(
                        "Выбранный пользователь не является участником этого домохозяйства",
                        "ASSIGNEE_NOT_MEMBER");
            }

            log.debug("MembershipPolicy: assignee {} is valid member", member.name());
        }

        return GuardrailOutcome.accept();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrder() {
        return 50; // Run first - reject invalid assignees early
    }
}
