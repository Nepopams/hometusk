package com.hometusk.commands.pipeline.guardrails;

import com.hometusk.users.domain.MembershipRole;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable snapshot of household context for guardrails evaluation.
 * Built by ContextBuilder, consumed by GuardrailPolicy implementations.
 *
 * <p>This is NOT sent to AI Platform - it contains internal data
 * (like task counts) that AI should not see.
 */
public record HouseholdSnapshot(
        UUID householdId,
        List<MemberInfo> members,
        List<ZoneInfo> zones,
        Map<UUID, Integer> openTaskCountByAssignee,
        Instant snapshotTime,
        boolean complete) {

    /**
     * Creates an empty snapshot indicating context could not be loaded.
     * Guardrails should handle this gracefully (typically CLARIFY or REJECT).
     */
    public static HouseholdSnapshot incomplete(UUID householdId, String reason) {
        return new HouseholdSnapshot(householdId, List.of(), List.of(), Map.of(), Instant.now(), false);
    }

    /**
     * Creates a complete snapshot with all required data.
     */
    public static HouseholdSnapshot complete(
            UUID householdId,
            List<MemberInfo> members,
            List<ZoneInfo> zones,
            Map<UUID, Integer> openTaskCountByAssignee) {
        return new HouseholdSnapshot(householdId, members, zones, openTaskCountByAssignee, Instant.now(), true);
    }

    /**
     * Gets the open task count for an assignee, defaulting to 0 if not found.
     */
    public int getOpenTaskCount(UUID assigneeId) {
        return openTaskCountByAssignee.getOrDefault(assigneeId, 0);
    }

    /**
     * Finds a member by ID.
     */
    public MemberInfo findMember(UUID memberId) {
        return members.stream().filter(m -> m.id().equals(memberId)).findFirst().orElse(null);
    }

    /**
     * Finds a zone by ID.
     */
    public ZoneInfo findZone(UUID zoneId) {
        return zones.stream().filter(z -> z.id().equals(zoneId)).findFirst().orElse(null);
    }

    /**
     * Checks if the snapshot has any members.
     */
    public boolean hasMembers() {
        return !members.isEmpty();
    }

    /**
     * Checks if the snapshot has any zones.
     */
    public boolean hasZones() {
        return !zones.isEmpty();
    }

    /**
     * Member information for guardrails.
     */
    public record MemberInfo(UUID id, String name, MembershipRole role) {
        public boolean isAdmin() {
            return role == MembershipRole.admin;
        }
    }

    /**
     * Zone information for guardrails.
     * ownerId is optional - set if zone has a designated owner.
     */
    public record ZoneInfo(UUID id, String name, UUID ownerId) {
        public ZoneInfo(UUID id, String name) {
            this(id, name, null);
        }

        public boolean hasOwner() {
            return ownerId != null;
        }
    }
}
