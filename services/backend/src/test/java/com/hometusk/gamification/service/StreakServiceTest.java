package com.hometusk.gamification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hometusk.gamification.domain.StreakState;
import com.hometusk.gamification.repository.StreakStateRepository;
import com.hometusk.households.domain.Household;
import com.hometusk.users.domain.User;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    @Mock
    private StreakStateRepository repository;

    @Mock
    private GamificationSettingsService settingsService;

    private StreakService streakService;

    @BeforeEach
    void setUp() {
        when(settingsService.isGamificationEnabled(any(), any())).thenReturn(true);
        streakService = new StreakService(repository, settingsService);
    }

    @Test
    void updateStreak_firstActivity_setsStreakToOne() throws Exception {
        User user = user("alice");
        Household household = household();
        StreakState state = new StreakState(user, household);

        when(repository.findByUser_IdAndHousehold_Id(user.getId(), household.getId()))
                .thenReturn(Optional.of(state));
        when(repository.save(any(StreakState.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate today = LocalDate.of(2025, 1, 1);
        streakService.updateStreak(user, household, today);

        assertThat(state.getCurrentStreak()).isEqualTo(1);
        assertThat(state.getBestStreak()).isEqualTo(1);
        assertThat(state.getLastActivityDate()).isEqualTo(today);
        assertThat(state.isGraceUsedToday()).isFalse();
    }

    @Test
    void updateStreak_consecutiveDay_incrementsAndResetsGrace() throws Exception {
        User user = user("bob");
        Household household = household();
        StreakState state = new StreakState(user, household);
        state.setCurrentStreak(3);
        state.setBestStreak(3);
        state.setGraceUsedToday(true);
        state.setLastActivityDate(LocalDate.of(2025, 1, 1));

        when(repository.findByUser_IdAndHousehold_Id(user.getId(), household.getId()))
                .thenReturn(Optional.of(state));
        when(repository.save(any(StreakState.class))).thenAnswer(invocation -> invocation.getArgument(0));

        streakService.updateStreak(user, household, LocalDate.of(2025, 1, 2));

        assertThat(state.getCurrentStreak()).isEqualTo(4);
        assertThat(state.isGraceUsedToday()).isFalse();
        assertThat(state.getBestStreak()).isEqualTo(4);
    }

    @Test
    void updateStreak_sameDay_noChange() throws Exception {
        User user = user("cara");
        Household household = household();
        StreakState state = new StreakState(user, household);
        state.setCurrentStreak(2);
        state.setBestStreak(2);
        state.setLastActivityDate(LocalDate.of(2025, 1, 1));

        when(repository.findByUser_IdAndHousehold_Id(user.getId(), household.getId()))
                .thenReturn(Optional.of(state));

        streakService.updateStreak(user, household, LocalDate.of(2025, 1, 1));

        assertThat(state.getCurrentStreak()).isEqualTo(2);
        verify(repository, never()).save(any());
    }

    @Test
    void updateStreak_graceDay_preservesStreak() throws Exception {
        User user = user("dave");
        Household household = household();
        StreakState state = new StreakState(user, household);
        state.setCurrentStreak(4);
        state.setBestStreak(4);
        state.setGraceUsedToday(false);
        state.setLastActivityDate(LocalDate.of(2025, 1, 1));

        when(repository.findByUser_IdAndHousehold_Id(user.getId(), household.getId()))
                .thenReturn(Optional.of(state));
        when(repository.save(any(StreakState.class))).thenAnswer(invocation -> invocation.getArgument(0));

        streakService.updateStreak(user, household, LocalDate.of(2025, 1, 3));

        assertThat(state.getCurrentStreak()).isEqualTo(5);
        assertThat(state.isGraceUsedToday()).isTrue();
        assertThat(state.getBestStreak()).isEqualTo(5);
    }

    @Test
    void updateStreak_graceUsedAlready_resets() throws Exception {
        User user = user("erin");
        Household household = household();
        StreakState state = new StreakState(user, household);
        state.setCurrentStreak(4);
        state.setBestStreak(4);
        state.setGraceUsedToday(true);
        state.setLastActivityDate(LocalDate.of(2025, 1, 1));

        when(repository.findByUser_IdAndHousehold_Id(user.getId(), household.getId()))
                .thenReturn(Optional.of(state));
        when(repository.save(any(StreakState.class))).thenAnswer(invocation -> invocation.getArgument(0));

        streakService.updateStreak(user, household, LocalDate.of(2025, 1, 3));

        assertThat(state.getCurrentStreak()).isEqualTo(1);
        assertThat(state.getBestStreak()).isEqualTo(4);
        assertThat(state.isGraceUsedToday()).isFalse();
    }

    @Test
    void updateStreak_gapMoreThanTwo_resetsAndPreservesBest() throws Exception {
        User user = user("frank");
        Household household = household();
        StreakState state = new StreakState(user, household);
        state.setCurrentStreak(6);
        state.setBestStreak(10);
        state.setGraceUsedToday(false);
        state.setLastActivityDate(LocalDate.of(2025, 1, 1));

        when(repository.findByUser_IdAndHousehold_Id(user.getId(), household.getId()))
                .thenReturn(Optional.of(state));
        when(repository.save(any(StreakState.class))).thenAnswer(invocation -> invocation.getArgument(0));

        streakService.updateStreak(user, household, LocalDate.of(2025, 1, 5));

        assertThat(state.getCurrentStreak()).isEqualTo(1);
        assertThat(state.getBestStreak()).isEqualTo(10);
        assertThat(state.isGraceUsedToday()).isFalse();
    }

    @Test
    void updateStreak_gamificationDisabled_skipsUpdate() throws Exception {
        User user = user("gina");
        Household household = household();
        when(settingsService.isGamificationEnabled(any(), any())).thenReturn(false);

        streakService.updateStreak(user, household, LocalDate.of(2025, 1, 1));

        verifyNoInteractions(repository);
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

    private static void setId(Object entity, UUID id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}
