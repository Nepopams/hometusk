package com.hometusk.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.hometusk.analytics.dto.AnalyticsSummaryResponse;
import com.hometusk.analytics.dto.MemberStats;
import com.hometusk.households.domain.Household;
import com.hometusk.households.repository.ZoneRepository;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import com.hometusk.users.domain.User;
import com.hometusk.users.repository.MembershipRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    private static final UUID UNASSIGNED_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private ZoneRepository zoneRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(taskRepository, membershipRepository, zoneRepository);
    }

    @Test
    void getAnalytics_withTasks_calculatesBalance() throws Exception {
        UUID householdId = UUID.randomUUID();
        Membership member1 = membership("Alice", householdId);
        Membership member2 = membership("Bob", householdId);

        when(membershipRepository.findByHousehold_IdWithUser(householdId)).thenReturn(List.of(member1, member2));
        when(zoneRepository.findByHousehold_Id(householdId)).thenReturn(List.of());
        when(taskRepository.countCompletedByAssigneeInPeriod(eq(householdId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(
                        new Object[] {member1.getUser().getId(), 4L},
                        new Object[] {member2.getUser().getId(), 2L}));
        when(taskRepository.countOverdueByAssignee(eq(householdId), any(Instant.class)))
                .thenReturn(List.of());
        when(taskRepository.countOpenByAssignee(eq(householdId), any(Instant.class)))
                .thenReturn(List.of());
        when(taskRepository.countCompletedByZoneInPeriod(eq(householdId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
        when(taskRepository.countOverdueByZone(eq(householdId), any(Instant.class)))
                .thenReturn(List.of());
        when(taskRepository.findTopOverdueTasks(eq(householdId), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());

        AnalyticsSummaryResponse response = analyticsService.getAnalytics(householdId, "7d");

        assertThat(response.fairness().gini()).isCloseTo(0.1666667, org.assertj.core.data.Offset.offset(1e-6));
        assertThat(response.fairness().balance()).isEqualTo(83);
        assertThat(response.fairness().formula()).isEqualTo("Balance = 100 × (1 - Gini coefficient)");
        assertThat(response.fairness().interpretation()).isEqualTo("Good balance — workload reasonably distributed.");

        Map<UUID, MemberStats> statsByMember = response.perMember().stream()
                .collect(java.util.stream.Collectors.toMap(MemberStats::memberId, stat -> stat));
        assertThat(statsByMember.get(member1.getUser().getId()).completedCount())
                .isEqualTo(4);
        assertThat(statsByMember.get(member2.getUser().getId()).completedCount())
                .isEqualTo(2);
    }

    @Test
    void getAnalytics_noTasks_returnsNullBalance() throws Exception {
        UUID householdId = UUID.randomUUID();
        Membership member = membership("Alice", householdId);

        when(membershipRepository.findByHousehold_IdWithUser(householdId)).thenReturn(List.of(member));
        when(zoneRepository.findByHousehold_Id(householdId)).thenReturn(List.of());
        when(taskRepository.countCompletedByAssigneeInPeriod(eq(householdId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
        when(taskRepository.countOverdueByAssignee(eq(householdId), any(Instant.class)))
                .thenReturn(List.of());
        when(taskRepository.countOpenByAssignee(eq(householdId), any(Instant.class)))
                .thenReturn(List.of());
        when(taskRepository.countCompletedByZoneInPeriod(eq(householdId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
        when(taskRepository.countOverdueByZone(eq(householdId), any(Instant.class)))
                .thenReturn(List.of());
        when(taskRepository.findTopOverdueTasks(eq(householdId), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());

        AnalyticsSummaryResponse response = analyticsService.getAnalytics(householdId, "7d");

        assertThat(response.fairness().gini()).isNull();
        assertThat(response.fairness().balance()).isNull();
        assertThat(response.fairness().formula()).isEqualTo("Balance = 100 × (1 - Gini coefficient)");
        assertThat(response.fairness().interpretation()).isEqualTo("N/A — no tasks completed in this period");
    }

    @Test
    void getAnalytics_invalidPeriod_defaultsTo7d() throws Exception {
        UUID householdId = UUID.randomUUID();
        Membership member = membership("Alice", householdId);

        when(membershipRepository.findByHousehold_IdWithUser(householdId)).thenReturn(List.of(member));
        when(zoneRepository.findByHousehold_Id(householdId)).thenReturn(List.of());
        when(taskRepository.countCompletedByAssigneeInPeriod(eq(householdId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
        when(taskRepository.countOverdueByAssignee(eq(householdId), any(Instant.class)))
                .thenReturn(List.of());
        when(taskRepository.countOpenByAssignee(eq(householdId), any(Instant.class)))
                .thenReturn(List.of());
        when(taskRepository.countCompletedByZoneInPeriod(eq(householdId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
        when(taskRepository.countOverdueByZone(eq(householdId), any(Instant.class)))
                .thenReturn(List.of());
        when(taskRepository.findTopOverdueTasks(eq(householdId), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());

        AnalyticsSummaryResponse response = analyticsService.getAnalytics(householdId, "bad");

        assertThat(response.period()).isEqualTo("7d");
    }

    @Test
    void getAnalytics_includesUnassignedCounts() throws Exception {
        UUID householdId = UUID.randomUUID();
        Membership member = membership("Alice", householdId);

        when(membershipRepository.findByHousehold_IdWithUser(householdId)).thenReturn(List.of(member));
        when(zoneRepository.findByHousehold_Id(householdId)).thenReturn(List.of());
        when(taskRepository.countCompletedByAssigneeInPeriod(eq(householdId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.<Object[]>of(new Object[] {null, 1L}));
        when(taskRepository.countOverdueByAssignee(eq(householdId), any(Instant.class)))
                .thenReturn(List.<Object[]>of(new Object[] {null, 2L}));
        when(taskRepository.countOpenByAssignee(eq(householdId), any(Instant.class)))
                .thenReturn(List.<Object[]>of(new Object[] {null, 3L}));
        when(taskRepository.countCompletedByZoneInPeriod(eq(householdId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
        when(taskRepository.countOverdueByZone(eq(householdId), any(Instant.class)))
                .thenReturn(List.of());
        when(taskRepository.findTopOverdueTasks(eq(householdId), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());

        AnalyticsSummaryResponse response = analyticsService.getAnalytics(householdId, "7d");

        assertThat(response.perMember())
                .anyMatch(stat -> stat.memberId().equals(UNASSIGNED_ID)
                        && stat.memberName().equals("Unassigned")
                        && stat.completedCount() == 1
                        && stat.overdueCount() == 2
                        && stat.openCount() == 3);
    }

    @Test
    void calculatePeriodStart_usesSevenDaysByDefault() throws Exception {
        Instant now = Instant.parse("2026-01-31T00:00:00Z");
        Instant start = invokeCalculatePeriodStart(analyticsService, "bad", now);
        assertThat(start).isEqualTo(now.minus(7, ChronoUnit.DAYS));
    }

    @Test
    void calculatePeriodStart_usesThirtyDaysFor30d() throws Exception {
        Instant now = Instant.parse("2026-01-31T00:00:00Z");
        Instant start = invokeCalculatePeriodStart(analyticsService, "30d", now);
        assertThat(start).isEqualTo(now.minus(30, ChronoUnit.DAYS));
    }

    @Test
    void generateInterpretation_null_returnsNA() throws Exception {
        String text = invokeGenerateInterpretation(analyticsService, null);
        assertThat(text).isEqualTo("N/A — no tasks completed in this period");
    }

    @Test
    void generateInterpretation_balance90_returnsExcellent() throws Exception {
        String text = invokeGenerateInterpretation(analyticsService, 90);
        assertThat(text).isEqualTo("Excellent balance — tasks evenly distributed among members.");
    }

    @Test
    void generateInterpretation_balance70_returnsGood() throws Exception {
        String text = invokeGenerateInterpretation(analyticsService, 70);
        assertThat(text).isEqualTo("Good balance — workload reasonably distributed.");
    }

    @Test
    void generateInterpretation_balance50_returnsModerate() throws Exception {
        String text = invokeGenerateInterpretation(analyticsService, 50);
        assertThat(text).isEqualTo("Moderate imbalance — some members completed more tasks than others.");
    }

    @Test
    void generateInterpretation_balance30_returnsSignificant() throws Exception {
        String text = invokeGenerateInterpretation(analyticsService, 30);
        assertThat(text).isEqualTo("Significant imbalance — workload concentrated on fewer members.");
    }

    @Test
    void generateInterpretation_lowBalance_returnsSevere() throws Exception {
        String text = invokeGenerateInterpretation(analyticsService, 10);
        assertThat(text).isEqualTo("Severe imbalance — most tasks completed by one or two members.");
    }

    private Membership membership(String displayName, UUID householdId) throws Exception {
        Household household = new Household("Test Household");
        setId(household, householdId);
        User user = new User("ext-" + displayName, displayName.toLowerCase() + "@test.local", displayName);
        setId(user, UUID.randomUUID());
        return new Membership(user, household, MembershipRole.member);
    }

    private static Instant invokeCalculatePeriodStart(AnalyticsService service, String period, Instant now)
            throws Exception {
        Method method = AnalyticsService.class.getDeclaredMethod("calculatePeriodStart", String.class, Instant.class);
        method.setAccessible(true);
        return (Instant) method.invoke(service, period, now);
    }

    private static String invokeGenerateInterpretation(AnalyticsService service, Integer balance) throws Exception {
        Method method = AnalyticsService.class.getDeclaredMethod("generateInterpretation", Integer.class);
        method.setAccessible(true);
        return (String) method.invoke(service, balance);
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}
