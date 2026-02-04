package com.hometusk.gamification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import com.hometusk.gamification.domain.PointsLedger;
import com.hometusk.gamification.domain.PointsReason;
import com.hometusk.gamification.repository.PointsLedgerRepository;
import com.hometusk.households.domain.Household;
import com.hometusk.tasks.domain.Task;
import com.hometusk.users.domain.User;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class PointsServiceTest {

    @Mock
    private PointsLedgerRepository pointsLedgerRepository;

    @Mock
    private GamificationSettingsService settingsService;

    private PointsService pointsService;

    @BeforeEach
    void setUp() {
        // lenient: not all tests invoke isGamificationEnabled (e.g., reverseForTaskUncompleted, no-assignee cases)
        lenient().when(settingsService.isGamificationEnabled(any(), any())).thenReturn(true);
        pointsService = new PointsService(pointsLedgerRepository, settingsService);
    }

    @Test
    void awardForTaskCompleted_withAssignee_awardsBasePoints() throws Exception {
        Household household = household();
        User assignee = user("assignee");
        Task task = task(household, assignee);

        when(pointsLedgerRepository.findByTask_IdAndUser_IdAndReason(
                        task.getId(), assignee.getId(), PointsReason.TASK_COMPLETED))
                .thenReturn(Optional.empty());
        when(pointsLedgerRepository.save(any(PointsLedger.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<PointsLedger> entries = pointsService.awardForTaskCompleted(task, assignee);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getPoints()).isEqualTo(10);
        assertThat(entries.get(0).getReason()).isEqualTo(PointsReason.TASK_COMPLETED);
    }

    @Test
    void awardForTaskCompleted_withDeadlineAndOnTime_awardsBonusPoints() throws Exception {
        Household household = household();
        User assignee = user("assignee");
        Task task = task(household, assignee);
        task.setDeadline(Instant.now().plus(1, ChronoUnit.DAYS));

        when(pointsLedgerRepository.findByTask_IdAndUser_IdAndReason(
                        task.getId(), assignee.getId(), PointsReason.TASK_COMPLETED))
                .thenReturn(Optional.empty());
        when(pointsLedgerRepository.findByTask_IdAndUser_IdAndReason(
                        task.getId(), assignee.getId(), PointsReason.ON_TIME_BONUS))
                .thenReturn(Optional.empty());
        when(pointsLedgerRepository.save(any(PointsLedger.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<PointsLedger> entries = pointsService.awardForTaskCompleted(task, assignee);

        assertThat(entries).hasSize(2);
        assertThat(entries)
                .anyMatch(entry -> entry.getReason() == PointsReason.TASK_COMPLETED && entry.getPoints() == 10);
        assertThat(entries)
                .anyMatch(entry -> entry.getReason() == PointsReason.ON_TIME_BONUS && entry.getPoints() == 5);
    }

    @Test
    void awardForTaskCompleted_withDeadlineButLate_noBonusPoints() throws Exception {
        Household household = household();
        User assignee = user("assignee");
        Task task = task(household, assignee);
        task.setDeadline(Instant.now().minus(1, ChronoUnit.DAYS));

        when(pointsLedgerRepository.findByTask_IdAndUser_IdAndReason(
                        task.getId(), assignee.getId(), PointsReason.TASK_COMPLETED))
                .thenReturn(Optional.empty());
        when(pointsLedgerRepository.save(any(PointsLedger.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<PointsLedger> entries = pointsService.awardForTaskCompleted(task, assignee);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getReason()).isEqualTo(PointsReason.TASK_COMPLETED);
    }

    @Test
    void awardForTaskCompleted_withNoDeadline_noBonusPoints() throws Exception {
        Household household = household();
        User assignee = user("assignee");
        Task task = task(household, assignee);

        when(pointsLedgerRepository.findByTask_IdAndUser_IdAndReason(
                        task.getId(), assignee.getId(), PointsReason.TASK_COMPLETED))
                .thenReturn(Optional.empty());
        when(pointsLedgerRepository.save(any(PointsLedger.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<PointsLedger> entries = pointsService.awardForTaskCompleted(task, assignee);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getReason()).isEqualTo(PointsReason.TASK_COMPLETED);
    }

    @Test
    void awardForTaskCompleted_withNoAssignee_noPoints() throws Exception {
        Household household = household();
        User creator = user("creator");
        Task task = task(household, creator);
        task.setAssignee(null);

        List<PointsLedger> entries = pointsService.awardForTaskCompleted(task, creator);

        assertThat(entries).isEmpty();
        verifyNoInteractions(pointsLedgerRepository);
    }

    @Test
    void awardForTaskCompleted_duplicate_idempotent() throws Exception {
        Household household = household();
        User assignee = user("assignee");
        Task task = task(household, assignee);
        PointsLedger existing = new PointsLedger(assignee, household, task, 10, PointsReason.TASK_COMPLETED);

        when(pointsLedgerRepository.findByTask_IdAndUser_IdAndReason(
                        task.getId(), assignee.getId(), PointsReason.TASK_COMPLETED))
                .thenReturn(Optional.of(existing));

        List<PointsLedger> entries = pointsService.awardForTaskCompleted(task, assignee);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0)).isSameAs(existing);
        verify(pointsLedgerRepository, never()).save(any());
    }

    @Test
    void reverseForTaskUncompleted_reversesBaseAndBonus() throws Exception {
        Household household = household();
        User assignee = user("assignee");
        Task task = task(household, assignee);

        when(pointsLedgerRepository.findByTask_IdAndUser_IdAndReason(
                        task.getId(), assignee.getId(), PointsReason.TASK_COMPLETED))
                .thenReturn(Optional.of(new PointsLedger(assignee, household, task, 10, PointsReason.TASK_COMPLETED)));
        when(pointsLedgerRepository.findByTask_IdAndUser_IdAndReason(
                        task.getId(), assignee.getId(), PointsReason.ON_TIME_BONUS))
                .thenReturn(Optional.of(new PointsLedger(assignee, household, task, 5, PointsReason.ON_TIME_BONUS)));
        when(pointsLedgerRepository.findByTask_IdAndUser_IdAndReason(
                        task.getId(), assignee.getId(), PointsReason.TASK_UNCOMPLETED))
                .thenReturn(Optional.empty());
        when(pointsLedgerRepository.findByTask_IdAndUser_IdAndReason(
                        task.getId(), assignee.getId(), PointsReason.ON_TIME_BONUS_REVERSED))
                .thenReturn(Optional.empty());
        when(pointsLedgerRepository.save(any(PointsLedger.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pointsService.reverseForTaskUncompleted(task, assignee);

        ArgumentCaptor<PointsLedger> captor = ArgumentCaptor.forClass(PointsLedger.class);
        verify(pointsLedgerRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(entry -> entry.getReason() == PointsReason.TASK_UNCOMPLETED && entry.getPoints() == -10)
                .anyMatch(entry -> entry.getReason() == PointsReason.ON_TIME_BONUS_REVERSED && entry.getPoints() == -5);
    }

    @Test
    void reverseForTaskUncompleted_idempotent() throws Exception {
        Household household = household();
        User assignee = user("assignee");
        Task task = task(household, assignee);

        when(pointsLedgerRepository.findByTask_IdAndUser_IdAndReason(
                        task.getId(), assignee.getId(), PointsReason.TASK_COMPLETED))
                .thenReturn(Optional.of(new PointsLedger(assignee, household, task, 10, PointsReason.TASK_COMPLETED)));
        when(pointsLedgerRepository.findByTask_IdAndUser_IdAndReason(
                        task.getId(), assignee.getId(), PointsReason.ON_TIME_BONUS))
                .thenReturn(Optional.of(new PointsLedger(assignee, household, task, 5, PointsReason.ON_TIME_BONUS)));
        when(pointsLedgerRepository.findByTask_IdAndUser_IdAndReason(
                        task.getId(), assignee.getId(), PointsReason.TASK_UNCOMPLETED))
                .thenReturn(
                        Optional.of(new PointsLedger(assignee, household, task, -10, PointsReason.TASK_UNCOMPLETED)));
        when(pointsLedgerRepository.findByTask_IdAndUser_IdAndReason(
                        task.getId(), assignee.getId(), PointsReason.ON_TIME_BONUS_REVERSED))
                .thenReturn(Optional.of(
                        new PointsLedger(assignee, household, task, -5, PointsReason.ON_TIME_BONUS_REVERSED)));

        pointsService.reverseForTaskUncompleted(task, assignee);

        verify(pointsLedgerRepository, never()).save(any());
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

    private Task task(Household household, User assignee) throws Exception {
        Task task = new Task(household, "Test Task", assignee);
        task.setAssignee(assignee);
        task.complete();
        setId(task, UUID.randomUUID());
        return task;
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}
