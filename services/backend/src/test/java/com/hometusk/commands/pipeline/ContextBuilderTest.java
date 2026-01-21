package com.hometusk.commands.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.hometusk.commands.pipeline.guardrails.GuardrailsConfig;
import com.hometusk.commands.pipeline.guardrails.HouseholdSnapshot;
import com.hometusk.households.domain.Household;
import com.hometusk.households.domain.Zone;
import com.hometusk.households.repository.ZoneRepository;
import com.hometusk.shopping.repository.ShoppingListRepository;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import com.hometusk.users.domain.User;
import com.hometusk.users.repository.MembershipRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContextBuilderTest {

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ShoppingListRepository shoppingListRepository;

    private ContextBuilder contextBuilder;

    private GuardrailsConfig guardrailsConfig;

    private UUID householdId;
    private UUID correlationId;
    private Household household;

    @BeforeEach
    void setUp() {
        guardrailsConfig = new GuardrailsConfig();
        contextBuilder = new ContextBuilder(
                membershipRepository, zoneRepository, taskRepository, shoppingListRepository, guardrailsConfig);
        householdId = UUID.randomUUID();
        correlationId = UUID.randomUUID();
        household = new Household("Test Household");
    }

    @Nested
    @DisplayName("buildSnapshot")
    class BuildSnapshot {

        @Test
        @DisplayName("should build complete snapshot with members, zones, and task counts")
        void buildsCompleteSnapshot() {
            // Given
            User user1 = createUser("user1@test.com", "Alice");
            User user2 = createUser("user2@test.com", "Bob");
            Membership membership1 = new Membership(user1, household, MembershipRole.admin);
            Membership membership2 = new Membership(user2, household, MembershipRole.member);

            Zone kitchen = new Zone(household, "Kitchen");
            Zone bathroom = new Zone(household, "Bathroom");
            setZoneId(kitchen, UUID.randomUUID());
            setZoneId(bathroom, UUID.randomUUID());

            when(membershipRepository.findByHousehold_Id(householdId)).thenReturn(List.of(membership1, membership2));
            when(zoneRepository.findByHousehold_Id(householdId)).thenReturn(List.of(kitchen, bathroom));
            when(taskRepository.countTasksByAssigneeAndStatuses(eq(householdId), any()))
                    .thenReturn(List.of(new Object[] {user1.getId(), 3L}, new Object[] {user2.getId(), 5L}));

            // When
            HouseholdSnapshot snapshot = contextBuilder.buildSnapshot(householdId, correlationId);

            // Then
            assertThat(snapshot.complete()).isTrue();
            assertThat(snapshot.householdId()).isEqualTo(householdId);
            assertThat(snapshot.members()).hasSize(2);
            assertThat(snapshot.zones()).hasSize(2);
            assertThat(snapshot.getOpenTaskCount(user1.getId())).isEqualTo(3);
            assertThat(snapshot.getOpenTaskCount(user2.getId())).isEqualTo(5);
        }

        @Test
        @DisplayName("should return incomplete snapshot when no members found")
        void returnsIncompleteWhenNoMembers() {
            // Given
            when(membershipRepository.findByHousehold_Id(householdId)).thenReturn(List.of());

            // When
            HouseholdSnapshot snapshot = contextBuilder.buildSnapshot(householdId, correlationId);

            // Then
            assertThat(snapshot.complete()).isFalse();
            assertThat(snapshot.members()).isEmpty();
        }

        @Test
        @DisplayName("should return incomplete snapshot on database error")
        void returnsIncompleteOnDatabaseError() {
            // Given
            when(membershipRepository.findByHousehold_Id(householdId))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // When
            HouseholdSnapshot snapshot = contextBuilder.buildSnapshot(householdId, correlationId);

            // Then
            assertThat(snapshot.complete()).isFalse();
        }

        @Test
        @DisplayName("should return zero task count for members with no tasks")
        void returnsZeroForMembersWithNoTasks() {
            // Given
            User user = createUser("user@test.com", "Alice");
            Membership membership = new Membership(user, household, MembershipRole.member);

            when(membershipRepository.findByHousehold_Id(householdId)).thenReturn(List.of(membership));
            when(zoneRepository.findByHousehold_Id(householdId)).thenReturn(List.of());
            when(taskRepository.countTasksByAssigneeAndStatuses(eq(householdId), any()))
                    .thenReturn(List.of());

            // When
            HouseholdSnapshot snapshot = contextBuilder.buildSnapshot(householdId, correlationId);

            // Then
            assertThat(snapshot.complete()).isTrue();
            assertThat(snapshot.getOpenTaskCount(user.getId())).isEqualTo(0);
        }

        @Test
        @DisplayName("should include member roles correctly")
        void includesMemberRoles() {
            // Given
            User admin = createUser("admin@test.com", "Admin");
            User member = createUser("member@test.com", "Member");
            Membership adminMembership = new Membership(admin, household, MembershipRole.admin);
            Membership memberMembership = new Membership(member, household, MembershipRole.member);

            when(membershipRepository.findByHousehold_Id(householdId))
                    .thenReturn(List.of(adminMembership, memberMembership));
            when(zoneRepository.findByHousehold_Id(householdId)).thenReturn(List.of());
            when(taskRepository.countTasksByAssigneeAndStatuses(eq(householdId), any()))
                    .thenReturn(List.of());

            // When
            HouseholdSnapshot snapshot = contextBuilder.buildSnapshot(householdId, correlationId);

            // Then
            assertThat(snapshot.findMember(admin.getId()).isAdmin()).isTrue();
            assertThat(snapshot.findMember(member.getId()).isAdmin()).isFalse();
        }
    }

    @Nested
    @DisplayName("buildHouseholdContextForAi")
    class BuildHouseholdContextForAi {

        @Test
        @DisplayName("should build context with members and zones for AI")
        void buildsContextForAi() {
            // Given
            User user = createUser("user@test.com", "Alice");
            Membership membership = new Membership(user, household, MembershipRole.member);
            Zone kitchen = new Zone(household, "Kitchen");
            setZoneId(kitchen, UUID.randomUUID());

            when(membershipRepository.findByHousehold_Id(householdId)).thenReturn(List.of(membership));
            when(zoneRepository.findByHousehold_Id(householdId)).thenReturn(List.of(kitchen));
            when(shoppingListRepository.findByHousehold_IdOrderByCreatedAtDesc(householdId))
                    .thenReturn(List.of());

            // When
            var context = contextBuilder.buildHouseholdContextForAi(householdId, correlationId);

            // Then
            assertThat(context).containsKey("members");
            assertThat(context).containsKey("zones");
            assertThat(context.get("members")).isInstanceOf(List.class);
            assertThat(context.get("zones")).isInstanceOf(List.class);
        }

        @Test
        @DisplayName("should return empty context on database error")
        void returnsEmptyOnError() {
            // Given
            when(membershipRepository.findByHousehold_Id(householdId)).thenThrow(new RuntimeException("Database error"));

            // When
            var context = contextBuilder.buildHouseholdContextForAi(householdId, correlationId);

            // Then
            assertThat(context).isEmpty();
        }

        @Test
        @DisplayName("should not include task counts in AI context")
        void doesNotIncludeTaskCounts() {
            // Given
            User user = createUser("user@test.com", "Alice");
            Membership membership = new Membership(user, household, MembershipRole.member);

            when(membershipRepository.findByHousehold_Id(householdId)).thenReturn(List.of(membership));
            when(zoneRepository.findByHousehold_Id(householdId)).thenReturn(List.of());
            when(shoppingListRepository.findByHousehold_IdOrderByCreatedAtDesc(householdId))
                    .thenReturn(List.of());

            // When
            var context = contextBuilder.buildHouseholdContextForAi(householdId, correlationId);

            // Then
            assertThat(context).doesNotContainKey("openTaskCountByAssignee");
            assertThat(context).doesNotContainKey("taskCounts");
        }
    }

    private User createUser(String email, String name) {
        User user = new User(UUID.randomUUID().toString(), email, name);
        setUserId(user, UUID.randomUUID());
        return user;
    }

    private void setUserId(User user, UUID id) {
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set user id for tests", e);
        }
    }

    private void setZoneId(Zone zone, UUID id) {
        try {
            Field idField = Zone.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(zone, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set zone id for tests", e);
        }
    }
}
