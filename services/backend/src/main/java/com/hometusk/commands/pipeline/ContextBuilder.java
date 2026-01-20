package com.hometusk.commands.pipeline;

import com.hometusk.commands.pipeline.guardrails.HouseholdSnapshot;
import com.hometusk.commands.pipeline.guardrails.HouseholdSnapshot.MemberInfo;
import com.hometusk.commands.pipeline.guardrails.HouseholdSnapshot.ZoneInfo;
import com.hometusk.households.domain.Zone;
import com.hometusk.households.repository.ZoneRepository;
import com.hometusk.shopping.domain.ShoppingList;
import com.hometusk.shopping.repository.ShoppingListRepository;
import com.hometusk.tasks.domain.TaskStatus;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.repository.MembershipRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Builds household context for AI Platform requests and guardrails evaluation.
 *
 * <p>This component is deterministic and does NOT use AI. It queries the database
 * to build a complete snapshot of the household state.
 *
 * <p>Fallback behavior: If context cannot be fully built (database failure),
 * returns an incomplete snapshot. GuardrailsOrchestrator must handle this
 * by degrading to CLARIFY or REJECT - we do NOT invent missing data.
 */
@Component
public class ContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(ContextBuilder.class);

    private static final List<TaskStatus> OPEN_STATUSES = List.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS);

    private final MembershipRepository membershipRepository;
    private final ZoneRepository zoneRepository;
    private final TaskRepository taskRepository;
    private final ShoppingListRepository shoppingListRepository;
    private final com.hometusk.commands.pipeline.guardrails.GuardrailsConfig guardrailsConfig;

    public ContextBuilder(
            MembershipRepository membershipRepository,
            ZoneRepository zoneRepository,
            TaskRepository taskRepository,
            ShoppingListRepository shoppingListRepository,
            com.hometusk.commands.pipeline.guardrails.GuardrailsConfig guardrailsConfig) {
        this.membershipRepository = membershipRepository;
        this.zoneRepository = zoneRepository;
        this.taskRepository = taskRepository;
        this.shoppingListRepository = shoppingListRepository;
        this.guardrailsConfig = guardrailsConfig;
    }

    /**
     * Builds a complete household snapshot for guardrails evaluation.
     *
     * @param householdId the household to build context for
     * @param correlationId for logging/tracing
     * @return HouseholdSnapshot (complete or incomplete with reason)
     */
    public HouseholdSnapshot buildSnapshot(UUID householdId, UUID correlationId) {
        log.debug("Building household snapshot: householdId={}, correlationId={}", householdId, correlationId);

        try {
            // 1. Load members
            List<Membership> memberships = membershipRepository.findByHouseholdId(householdId);
            if (memberships.isEmpty()) {
                log.warn(
                        "No members found for household: householdId={}, correlationId={}", householdId, correlationId);
                return HouseholdSnapshot.incomplete(householdId, "No members found in household");
            }

            List<MemberInfo> members = memberships.stream()
                    .map(m -> new MemberInfo(m.getUserId(), m.getUser().getDisplayName(), m.getRole()))
                    .toList();

            // 2. Load zones
            List<Zone> zoneEntities = zoneRepository.findByHouseholdId(householdId);
            List<ZoneInfo> zones = zoneEntities.stream()
                    .map(z -> new ZoneInfo(z.getId(), z.getName(), z.getOwnerId()))
                    .toList();

            // 3. Count open tasks per assignee (batch query)
            Map<UUID, Integer> taskCounts = countOpenTasksByAssignee(householdId);

            log.debug(
                    "Household snapshot built: householdId={}, members={}, zones={}, correlationId={}",
                    householdId,
                    members.size(),
                    zones.size(),
                    correlationId);

            return HouseholdSnapshot.complete(householdId, members, zones, taskCounts);

        } catch (Exception e) {
            log.error(
                    "Failed to build household snapshot: householdId={}, correlationId={}",
                    householdId,
                    correlationId,
                    e);
            return HouseholdSnapshot.incomplete(householdId, "Database error: " + e.getMessage());
        }
    }

    /**
     * Builds minimal context for AI Platform request (no internal data like task counts).
     *
     * @param householdId the household
     * @param correlationId for logging/tracing
     * @return Map suitable for AI Platform request, or empty map on failure
     */
    public Map<String, Object> buildHouseholdContextForAi(UUID householdId, UUID correlationId) {
        log.debug("Building AI context: householdId={}, correlationId={}", householdId, correlationId);

        try {
            // 1. Count open tasks per assignee (needed for workload_score)
            Map<UUID, Integer> taskCounts = countOpenTasksByAssignee(householdId);
            int maxTasks = guardrailsConfig.getMaxOpenTasksPerAssignee();

            // 2. Load members with workload_score
            List<Membership> memberships = membershipRepository.findByHouseholdId(householdId);
            List<Map<String, Object>> membersList = memberships.stream()
                    .map(m -> {
                        int openTasks = taskCounts.getOrDefault(m.getUserId(), 0);
                        double workloadScore = calculateWorkloadScore(openTasks, maxTasks);
                        Map<String, Object> memberMap = new HashMap<>();
                        memberMap.put("id", m.getUserId().toString());
                        memberMap.put("name", m.getUser().getDisplayName());
                        memberMap.put("workload_score", workloadScore);
                        return memberMap;
                    })
                    .toList();

            // 3. Load zones with owner_id (if present)
            List<Zone> zoneEntities = zoneRepository.findByHouseholdId(householdId);
            List<Map<String, Object>> zonesList = zoneEntities.stream()
                    .map(z -> {
                        Map<String, Object> zoneMap = new HashMap<>();
                        zoneMap.put("id", z.getId().toString());
                        zoneMap.put("name", z.getName());
                        if (z.getOwnerId() != null) {
                            zoneMap.put("owner_id", z.getOwnerId().toString());
                        }
                        return zoneMap;
                    })
                    .toList();

            // 4. Load shopping lists (Stage 5)
            List<ShoppingList> shoppingListEntities =
                    shoppingListRepository.findByHouseholdIdOrderByCreatedAtDesc(householdId);
            List<Map<String, Object>> shoppingLists = shoppingListEntities.stream()
                    .map(l -> {
                        Map<String, Object> listMap = new HashMap<>();
                        listMap.put("id", l.getId().toString());
                        listMap.put("name", l.getName());
                        return listMap;
                    })
                    .toList();

            log.debug(
                    "AI context built: householdId={}, members={}, zones={}, shoppingLists={}, correlationId={}",
                    householdId,
                    membersList.size(),
                    zonesList.size(),
                    shoppingLists.size(),
                    correlationId);

            return Map.of("members", membersList, "zones", zonesList, "shopping_lists", shoppingLists);

        } catch (Exception e) {
            log.error("Failed to build AI context: householdId={}, correlationId={}", householdId, correlationId, e);
            // Return empty context - AI Platform will work with limited info
            // The actual guardrails will catch incomplete context later
            return Map.of();
        }
    }

    private Map<UUID, Integer> countOpenTasksByAssignee(UUID householdId) {
        List<Object[]> results = taskRepository.countTasksByAssigneeAndStatuses(householdId, OPEN_STATUSES);
        Map<UUID, Integer> counts = new HashMap<>();
        for (Object[] row : results) {
            UUID assigneeId = (UUID) row[0];
            Long count = (Long) row[1];
            counts.put(assigneeId, count.intValue());
        }
        return counts;
    }

    /**
     * Calculate workload score for a member based on their open task count.
     *
     * @param openTasks number of open tasks assigned to the member
     * @param maxTasks maximum allowed open tasks per assignee
     * @return workload score between 0.0 (no tasks) and 1.0 (at or over capacity)
     */
    private double calculateWorkloadScore(int openTasks, int maxTasks) {
        if (maxTasks <= 0) {
            log.warn("Invalid maxTasks configuration: {}, defaulting to 10", maxTasks);
            maxTasks = 10;
        }
        double score = (double) openTasks / maxTasks;
        return Math.min(1.0, Math.max(0.0, score));
    }
}
