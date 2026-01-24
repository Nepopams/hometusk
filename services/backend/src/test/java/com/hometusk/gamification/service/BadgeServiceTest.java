package com.hometusk.gamification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hometusk.gamification.domain.Badge;
import com.hometusk.gamification.domain.PointsReason;
import com.hometusk.gamification.domain.UserBadge;
import com.hometusk.gamification.repository.BadgeRepository;
import com.hometusk.gamification.repository.PointsLedgerRepository;
import com.hometusk.gamification.repository.UserBadgeRepository;
import com.hometusk.households.domain.Household;
import com.hometusk.notifications.service.NotificationService;
import com.hometusk.users.domain.User;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @Mock
    private PointsLedgerRepository pointsLedgerRepository;

    @Mock
    private NotificationService notificationService;

    private BadgeService badgeService;

    @BeforeEach
    void setUp() {
        badgeService =
                new BadgeService(badgeRepository, userBadgeRepository, pointsLedgerRepository, notificationService);
    }

    @Test
    void checkAndAwardBadges_firstTask_awardsBadge() throws Exception {
        Household household = household();
        User user = user("alice");
        Badge badge = badge("FIRST_TASK");

        stubCounts(user, household, 1, 0, 0, List.of());
        when(userBadgeRepository.findBadgeCodesByUserAndHousehold(user.getId(), household.getId()))
                .thenReturn(List.of());
        when(badgeRepository.findByCode("FIRST_TASK")).thenReturn(Optional.of(badge));
        when(userBadgeRepository.save(any(UserBadge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        badgeService.checkAndAwardBadges(user, household);

        ArgumentCaptor<UserBadge> captor = ArgumentCaptor.forClass(UserBadge.class);
        verify(userBadgeRepository).save(captor.capture());
        assertThat(captor.getValue().getBadge().getCode()).isEqualTo("FIRST_TASK");
        verify(notificationService).notifyBadgeEarned(user, household, badge);
    }

    @Test
    void checkAndAwardBadges_tenTasks_awardsBadge() throws Exception {
        Household household = household();
        User user = user("bob");
        Badge badge = badge("TEN_TASKS");

        stubCounts(user, household, 10, 0, 0, List.of());
        when(userBadgeRepository.findBadgeCodesByUserAndHousehold(user.getId(), household.getId()))
                .thenReturn(List.of("FIRST_TASK"));
        when(badgeRepository.findByCode("TEN_TASKS")).thenReturn(Optional.of(badge));
        when(userBadgeRepository.save(any(UserBadge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        badgeService.checkAndAwardBadges(user, household);

        ArgumentCaptor<UserBadge> captor = ArgumentCaptor.forClass(UserBadge.class);
        verify(userBadgeRepository).save(captor.capture());
        assertThat(captor.getValue().getBadge().getCode()).isEqualTo("TEN_TASKS");
        verify(notificationService).notifyBadgeEarned(user, household, badge);
    }

    @Test
    void checkAndAwardBadges_weekWarrior_awardsBadge() throws Exception {
        Household household = household();
        User user = user("carol");
        Badge badge = badge("WEEK_WARRIOR");

        stubCounts(user, household, 0, 0, 7, List.of());
        when(userBadgeRepository.findBadgeCodesByUserAndHousehold(user.getId(), household.getId()))
                .thenReturn(List.of());
        when(badgeRepository.findByCode("WEEK_WARRIOR")).thenReturn(Optional.of(badge));
        when(userBadgeRepository.save(any(UserBadge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        badgeService.checkAndAwardBadges(user, household);

        ArgumentCaptor<UserBadge> captor = ArgumentCaptor.forClass(UserBadge.class);
        verify(userBadgeRepository).save(captor.capture());
        assertThat(captor.getValue().getBadge().getCode()).isEqualTo("WEEK_WARRIOR");
        verify(notificationService).notifyBadgeEarned(user, household, badge);
    }

    @Test
    void checkAndAwardBadges_zoneSpecialist_awardsBadge() throws Exception {
        Household household = household();
        User user = user("dave");
        Badge badge = badge("ZONE_SPECIALIST");

        stubCounts(user, household, 0, 0, 0, List.<Object[]>of(new Object[] {UUID.randomUUID(), 5L}));
        when(userBadgeRepository.findBadgeCodesByUserAndHousehold(user.getId(), household.getId()))
                .thenReturn(List.of());
        when(badgeRepository.findByCode("ZONE_SPECIALIST")).thenReturn(Optional.of(badge));
        when(userBadgeRepository.save(any(UserBadge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        badgeService.checkAndAwardBadges(user, household);

        ArgumentCaptor<UserBadge> captor = ArgumentCaptor.forClass(UserBadge.class);
        verify(userBadgeRepository).save(captor.capture());
        assertThat(captor.getValue().getBadge().getCode()).isEqualTo("ZONE_SPECIALIST");
        verify(notificationService).notifyBadgeEarned(user, household, badge);
    }

    @Test
    void checkAndAwardBadges_onTimeHero_awardsBadge() throws Exception {
        Household household = household();
        User user = user("erin");
        Badge badge = badge("ON_TIME_HERO");

        stubCounts(user, household, 0, 5, 0, List.of());
        when(userBadgeRepository.findBadgeCodesByUserAndHousehold(user.getId(), household.getId()))
                .thenReturn(List.of());
        when(badgeRepository.findByCode("ON_TIME_HERO")).thenReturn(Optional.of(badge));
        when(userBadgeRepository.save(any(UserBadge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        badgeService.checkAndAwardBadges(user, household);

        ArgumentCaptor<UserBadge> captor = ArgumentCaptor.forClass(UserBadge.class);
        verify(userBadgeRepository).save(captor.capture());
        assertThat(captor.getValue().getBadge().getCode()).isEqualTo("ON_TIME_HERO");
        verify(notificationService).notifyBadgeEarned(user, household, badge);
    }

    @Test
    void checkAndAwardBadges_alreadyEarned_noDuplicate() throws Exception {
        Household household = household();
        User user = user("frank");

        stubCounts(user, household, 1, 0, 0, List.of());
        when(userBadgeRepository.findBadgeCodesByUserAndHousehold(user.getId(), household.getId()))
                .thenReturn(List.of("FIRST_TASK"));

        badgeService.checkAndAwardBadges(user, household);

        verify(userBadgeRepository, never()).save(any());
        verify(notificationService, never()).notifyBadgeEarned(any(), any(), any());
    }

    private void stubCounts(
            User user,
            Household household,
            long completedCount,
            long onTimeCount,
            long weekCount,
            List<Object[]> zoneCounts) {
        when(pointsLedgerRepository.countByUser_IdAndHousehold_IdAndReason(
                        user.getId(), household.getId(), PointsReason.TASK_COMPLETED))
                .thenReturn(completedCount);
        when(pointsLedgerRepository.countByUser_IdAndHousehold_IdAndReason(
                        user.getId(), household.getId(), PointsReason.ON_TIME_BONUS))
                .thenReturn(onTimeCount);
        when(pointsLedgerRepository.countByUser_IdAndHousehold_IdAndReasonAndCreatedAtAfter(
                        eq(user.getId()), eq(household.getId()), eq(PointsReason.TASK_COMPLETED), any(Instant.class)))
                .thenReturn(weekCount);
        when(pointsLedgerRepository.countCompletedByZone(user.getId(), household.getId(), PointsReason.TASK_COMPLETED))
                .thenReturn(zoneCounts);
    }

    private Household household() throws Exception {
        Household household = new Household("Test Household");
        setId(household, UUID.randomUUID());
        return household;
    }

    private User user(String name) throws Exception {
        User user = new User("ext-" + name, name + "@test.local", name);
        setId(user, UUID.randomUUID());
        return user;
    }

    private Badge badge(String code) throws Exception {
        Badge badge = new Badge(code, code, code + " badge", "Criteria", "icon");
        setId(badge, UUID.randomUUID());
        return badge;
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}
