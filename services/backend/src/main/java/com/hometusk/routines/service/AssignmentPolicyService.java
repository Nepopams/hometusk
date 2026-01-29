package com.hometusk.routines.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.routines.domain.RoundRobinState;
import com.hometusk.routines.domain.Routine;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.User;
import com.hometusk.users.repository.MembershipRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AssignmentPolicyService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentPolicyService.class);

    private final MembershipRepository membershipRepository;
    private final ObjectMapper objectMapper;

    public AssignmentPolicyService(MembershipRepository membershipRepository, ObjectMapper objectMapper) {
        this.membershipRepository = membershipRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Determine assignee based on routine's assignment policy.
     * For ROUND_ROBIN, also updates the routine's roundRobinStateJson.
     */
    public User determineAssignee(Routine routine) {
        return switch (routine.getAssignmentPolicy()) {
            case FIXED -> routine.getFixedAssignee();
            case MANUAL -> null;
            case ROUND_ROBIN -> determineRoundRobinAssignee(routine);
        };
    }

    private User determineRoundRobinAssignee(Routine routine) {
        List<User> currentMembers = getCurrentMembers(routine.getHouseholdId());
        if (currentMembers.isEmpty()) {
            log.warn("No members in household {} for round-robin", routine.getHouseholdId());
            return null;
        }

        RoundRobinState state = parseState(routine.getRoundRobinStateJson());
        List<UUID> memberIds = currentMembers.stream().map(User::getId).toList();

        UUID nextAssigneeId = findNextAssignee(state, memberIds);
        User nextAssignee = currentMembers.stream()
                .filter(user -> user.getId().equals(nextAssigneeId))
                .findFirst()
                .orElse(currentMembers.get(0));

        RoundRobinState newState = new RoundRobinState(nextAssignee.getId(), memberIds);
        routine.setRoundRobinStateJson(serializeState(newState));

        return nextAssignee;
    }

    private List<User> getCurrentMembers(UUID householdId) {
        return membershipRepository.findByHousehold_IdWithUser(householdId).stream()
                .sorted(Comparator.comparing(Membership::getJoinedAt)
                        .thenComparing(membership -> membership.getUser().getId()))
                .map(Membership::getUser)
                .toList();
    }

    private UUID findNextAssignee(RoundRobinState state, List<UUID> currentMemberIds) {
        if (state == null || state.lastAssignedUserId() == null) {
            return currentMemberIds.get(0);
        }

        UUID lastAssigned = state.lastAssignedUserId();
        int lastIndex = currentMemberIds.indexOf(lastAssigned);
        if (lastIndex == -1) {
            return currentMemberIds.get(0);
        }

        int nextIndex = (lastIndex + 1) % currentMemberIds.size();
        return currentMemberIds.get(nextIndex);
    }

    private RoundRobinState parseState(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, RoundRobinState.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse round-robin state, will reinitialize: {}", e.getMessage());
            return null;
        }
    }

    private String serializeState(RoundRobinState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize round-robin state", e);
        }
    }
}
