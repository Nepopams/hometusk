package com.hometusk.routines.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.households.domain.Household;
import com.hometusk.routines.domain.AssignmentPolicy;
import com.hometusk.routines.domain.RoundRobinState;
import com.hometusk.routines.domain.Routine;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import com.hometusk.users.domain.User;
import com.hometusk.users.repository.MembershipRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignmentPolicyServiceTest {

    @Mock
    private MembershipRepository membershipRepository;

    private AssignmentPolicyService assignmentPolicyService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        assignmentPolicyService = new AssignmentPolicyService(membershipRepository, objectMapper);
    }

    @Test
    void fixed_assignsToConfiguredUser() {
        Household household = householdWithId(UUID.randomUUID());
        User fixedAssignee = userWithId(UUID.randomUUID(), "Fixed");
        Routine routine =
                new Routine(household, "Fixed", "{\"type\":\"DAILY\"}", AssignmentPolicy.FIXED, fixedAssignee);
        routine.setFixedAssignee(fixedAssignee);

        User result = assignmentPolicyService.determineAssignee(routine);

        assertThat(result).isEqualTo(fixedAssignee);
    }

    @Test
    void manual_returnsNull() {
        Household household = householdWithId(UUID.randomUUID());
        User creator = userWithId(UUID.randomUUID(), "Creator");
        Routine routine = new Routine(household, "Manual", "{\"type\":\"DAILY\"}", AssignmentPolicy.MANUAL, creator);

        User result = assignmentPolicyService.determineAssignee(routine);

        assertThat(result).isNull();
    }

    @Test
    void roundRobin_firstAssignment_assignsFirstMember() throws Exception {
        Household household = householdWithId(UUID.randomUUID());
        User memberA = userWithId(UUID.randomUUID(), "A");
        User memberB = userWithId(UUID.randomUUID(), "B");
        Routine routine = new Routine(household, "RR", "{\"type\":\"DAILY\"}", AssignmentPolicy.ROUND_ROBIN, memberA);

        when(membershipRepository.findByHousehold_IdWithUser(household.getId()))
                .thenReturn(List.of(
                        membership(memberA, household, Instant.parse("2026-01-01T10:00:00Z")),
                        membership(memberB, household, Instant.parse("2026-01-02T10:00:00Z"))));

        User result = assignmentPolicyService.determineAssignee(routine);

        assertThat(result).isEqualTo(memberA);
        RoundRobinState state = objectMapper.readValue(routine.getRoundRobinStateJson(), RoundRobinState.class);
        assertThat(state.lastAssignedUserId()).isEqualTo(memberA.getId());
        assertThat(state.memberOrder()).containsExactly(memberA.getId(), memberB.getId());
    }

    @Test
    void roundRobin_rotatesCorrectly() throws Exception {
        Household household = householdWithId(UUID.randomUUID());
        User memberA = userWithId(UUID.randomUUID(), "A");
        User memberB = userWithId(UUID.randomUUID(), "B");
        User memberC = userWithId(UUID.randomUUID(), "C");
        Routine routine = new Routine(household, "RR", "{\"type\":\"DAILY\"}", AssignmentPolicy.ROUND_ROBIN, memberA);
        RoundRobinState initialState = new RoundRobinState(memberA.getId(), List.of(memberA.getId(), memberB.getId()));
        routine.setRoundRobinStateJson(objectMapper.writeValueAsString(initialState));

        when(membershipRepository.findByHousehold_IdWithUser(household.getId()))
                .thenReturn(List.of(
                        membership(memberA, household, Instant.parse("2026-01-01T10:00:00Z")),
                        membership(memberB, household, Instant.parse("2026-01-02T10:00:00Z")),
                        membership(memberC, household, Instant.parse("2026-01-03T10:00:00Z"))));

        User result = assignmentPolicyService.determineAssignee(routine);

        assertThat(result).isEqualTo(memberB);
    }

    @Test
    void roundRobin_wrapsAround() throws Exception {
        Household household = householdWithId(UUID.randomUUID());
        User memberA = userWithId(UUID.randomUUID(), "A");
        User memberB = userWithId(UUID.randomUUID(), "B");
        User memberC = userWithId(UUID.randomUUID(), "C");
        Routine routine = new Routine(household, "RR", "{\"type\":\"DAILY\"}", AssignmentPolicy.ROUND_ROBIN, memberA);
        RoundRobinState initialState =
                new RoundRobinState(memberC.getId(), List.of(memberA.getId(), memberB.getId(), memberC.getId()));
        routine.setRoundRobinStateJson(objectMapper.writeValueAsString(initialState));

        when(membershipRepository.findByHousehold_IdWithUser(household.getId()))
                .thenReturn(List.of(
                        membership(memberA, household, Instant.parse("2026-01-01T10:00:00Z")),
                        membership(memberB, household, Instant.parse("2026-01-02T10:00:00Z")),
                        membership(memberC, household, Instant.parse("2026-01-03T10:00:00Z"))));

        User result = assignmentPolicyService.determineAssignee(routine);

        assertThat(result).isEqualTo(memberA);
    }

    @Test
    void roundRobin_handlesMemberRemoval() throws Exception {
        Household household = householdWithId(UUID.randomUUID());
        User memberA = userWithId(UUID.randomUUID(), "A");
        User memberB = userWithId(UUID.randomUUID(), "B");
        User memberC = userWithId(UUID.randomUUID(), "C");
        Routine routine = new Routine(household, "RR", "{\"type\":\"DAILY\"}", AssignmentPolicy.ROUND_ROBIN, memberA);
        RoundRobinState initialState =
                new RoundRobinState(memberB.getId(), List.of(memberA.getId(), memberB.getId(), memberC.getId()));
        routine.setRoundRobinStateJson(objectMapper.writeValueAsString(initialState));

        when(membershipRepository.findByHousehold_IdWithUser(household.getId()))
                .thenReturn(List.of(
                        membership(memberA, household, Instant.parse("2026-01-01T10:00:00Z")),
                        membership(memberC, household, Instant.parse("2026-01-03T10:00:00Z"))));

        User result = assignmentPolicyService.determineAssignee(routine);

        assertThat(result).isEqualTo(memberA);
    }

    @Test
    void roundRobin_emptyHousehold_returnsNull() {
        Household household = householdWithId(UUID.randomUUID());
        User memberA = userWithId(UUID.randomUUID(), "A");
        Routine routine = new Routine(household, "RR", "{\"type\":\"DAILY\"}", AssignmentPolicy.ROUND_ROBIN, memberA);

        when(membershipRepository.findByHousehold_IdWithUser(household.getId())).thenReturn(List.of());

        User result = assignmentPolicyService.determineAssignee(routine);

        assertThat(result).isNull();
        assertThat(routine.getRoundRobinStateJson()).isNull();
    }

    @Test
    void roundRobin_updatesStateJson() throws Exception {
        Household household = householdWithId(UUID.randomUUID());
        User memberA = userWithId(UUID.randomUUID(), "A");
        User memberB = userWithId(UUID.randomUUID(), "B");
        Routine routine = new Routine(household, "RR", "{\"type\":\"DAILY\"}", AssignmentPolicy.ROUND_ROBIN, memberA);

        when(membershipRepository.findByHousehold_IdWithUser(household.getId()))
                .thenReturn(List.of(
                        membership(memberA, household, Instant.parse("2026-01-01T10:00:00Z")),
                        membership(memberB, household, Instant.parse("2026-01-02T10:00:00Z"))));

        assignmentPolicyService.determineAssignee(routine);

        RoundRobinState state = objectMapper.readValue(routine.getRoundRobinStateJson(), RoundRobinState.class);
        assertThat(state.memberOrder()).containsExactly(memberA.getId(), memberB.getId());
    }

    private Household householdWithId(UUID id) {
        Household household = new Household("Household");
        setField(household, "id", id);
        return household;
    }

    private User userWithId(UUID id, String name) {
        User user = new User("ext-" + id, name + "@test.local", name);
        setField(user, "id", id);
        return user;
    }

    private Membership membership(User user, Household household, Instant joinedAt) {
        Membership membership = new Membership(user, household, MembershipRole.member);
        setField(membership, "joinedAt", joinedAt);
        return membership;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set field " + fieldName, e);
        }
    }
}
