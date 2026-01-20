package com.hometusk.commands.pipeline.guardrails;

import com.hometusk.commands.pipeline.decision.DecisionResult.StartJob.ProposedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Policy that prefers zone owner for task assignment.
 *
 * <p>When a task is created for a specific zone and has no assignee,
 * this policy suggests the zone owner if one exists.
 *
 * <p>Behavior:
 * <ul>
 *   <li>If zone has owner and no assignee specified → MODIFY to add owner as assignee</li>
 *   <li>If zone has owner but different assignee specified → ACCEPT (user choice)</li>
 *   <li>If no zone or no owner → ACCEPT</li>
 * </ul>
 */
@Component
public class ZoneOwnerFirstPolicy implements GuardrailPolicy {

    private static final Logger log = LoggerFactory.getLogger(ZoneOwnerFirstPolicy.class);
    private static final String NAME = "ZoneOwnerFirst";

    @Override
    public GuardrailOutcome evaluate(GuardrailContext context) {
        List<ProposedAction> modifiedActions = new ArrayList<>();
        boolean anyModification = false;

        for (ProposedAction action : context.decision().actions()) {
            if (!"create_task".equals(action.actionType())) {
                modifiedActions.add(action);
                continue;
            }

            // Check if zoneId is specified
            Object zoneIdObj = action.parameters().get("zoneId");
            if (zoneIdObj == null) {
                modifiedActions.add(action);
                continue;
            }

            UUID zoneId;
            try {
                zoneId = UUID.fromString(zoneIdObj.toString());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid zoneId format: {}", zoneIdObj);
                modifiedActions.add(action);
                continue;
            }

            // Check if assignee is already specified
            Object assigneeIdObj = action.parameters().get("assigneeId");
            if (assigneeIdObj != null) {
                // User explicitly chose an assignee, respect their choice
                modifiedActions.add(action);
                continue;
            }

            // Look up zone to check if it has an owner
            HouseholdSnapshot.ZoneInfo zone = context.householdSnapshot().findZone(zoneId);
            if (zone == null || !zone.hasOwner()) {
                modifiedActions.add(action);
                continue;
            }

            // Zone has an owner - suggest them as assignee
            UUID ownerId = zone.ownerId();
            HouseholdSnapshot.MemberInfo owner = context.householdSnapshot().findMember(ownerId);

            if (owner == null) {
                log.warn("Zone owner not found in members: ownerId={}, zoneId={}", ownerId, zoneId);
                modifiedActions.add(action);
                continue;
            }

            log.info(
                    "ZoneOwnerFirst policy: assigning task to zone owner: zone={}, owner={}",
                    zone.name(),
                    owner.name());

            // Create modified action with owner as assignee
            Map<String, Object> modifiedParams = new HashMap<>(action.parameters());
            modifiedParams.put("assigneeId", ownerId.toString());

            modifiedActions.add(new ProposedAction(action.actionType(), modifiedParams));
            anyModification = true;
        }

        if (anyModification) {
            return new GuardrailOutcome.Modify(modifiedActions, "Assigned to zone owner based on zone preference");
        }

        return GuardrailOutcome.accept();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrder() {
        return 100; // Run early to set assignee before MaxOpenTasks checks
    }
}
