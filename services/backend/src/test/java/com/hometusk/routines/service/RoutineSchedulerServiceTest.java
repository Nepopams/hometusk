package com.hometusk.routines.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hometusk.households.domain.Household;
import com.hometusk.routines.domain.AssignmentPolicy;
import com.hometusk.routines.domain.RecurrenceRule;
import com.hometusk.routines.domain.Routine;
import com.hometusk.routines.domain.RoutineStatus;
import com.hometusk.routines.repository.RoutineRepository;
import com.hometusk.tasks.domain.Task;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.domain.User;
import java.time.LocalDate;
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
class RoutineSchedulerServiceTest {

    @Mock
    private RoutineRepository routineRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private RecurrenceRuleParser recurrenceRuleParser;

    @Mock
    private RoutineService routineService;

    @Mock
    private AssignmentPolicyService assignmentPolicyService;

    private RoutineSchedulerService schedulerService;

    @BeforeEach
    void setUp() {
        schedulerService = new RoutineSchedulerService(
                routineRepository, taskRepository, recurrenceRuleParser, routineService, assignmentPolicyService);
    }

    @Test
    void generateUpcomingTasks_createsTasksForWindow() {
        Routine routine = buildRoutine("Daily", 3);
        LocalDate today = LocalDate.now();
        List<LocalDate> dates = List.of(today, today.plusDays(1), today.plusDays(2));

        when(routineRepository.findByStatus(RoutineStatus.ACTIVE)).thenReturn(List.of(routine));
        when(routineRepository.findByIdForUpdate(routine.getId())).thenReturn(Optional.of(routine));
        when(routineService.parseRecurrenceRule(routine.getRecurrenceRuleJson()))
                .thenReturn(new RecurrenceRule.Daily());
        when(recurrenceRuleParser.getOccurrencesInRange(any(), eq(today), eq(3)))
                .thenReturn(dates);
        when(taskRepository.existsByRoutine_IdAndScheduledDate(eq(routine.getId()), any()))
                .thenReturn(false);
        when(assignmentPolicyService.determineAssignee(routine)).thenReturn(null);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoutineSchedulerService.SchedulerResult result = schedulerService.generateUpcomingTasks();

        assertThat(result.routinesProcessed()).isEqualTo(1);
        assertThat(result.tasksCreated()).isEqualTo(3);
        assertThat(result.tasksSkipped()).isZero();
        assertThat(result.errors()).isZero();

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, org.mockito.Mockito.times(3)).save(taskCaptor.capture());
        assertThat(taskCaptor.getAllValues())
                .extracting(Task::getScheduledDate)
                .containsExactlyInAnyOrderElementsOf(dates);
        assertThat(taskCaptor.getAllValues()).extracting(Task::getCreatedVia).containsOnly("scheduler");
    }

    @Test
    void generateUpcomingTasks_skipsExistingDates() {
        Routine routine = buildRoutine("Daily", 2);
        LocalDate today = LocalDate.now();
        List<LocalDate> dates = List.of(today, today.plusDays(1));

        when(routineRepository.findByStatus(RoutineStatus.ACTIVE)).thenReturn(List.of(routine));
        when(routineRepository.findByIdForUpdate(routine.getId())).thenReturn(Optional.of(routine));
        when(routineService.parseRecurrenceRule(routine.getRecurrenceRuleJson()))
                .thenReturn(new RecurrenceRule.Daily());
        when(recurrenceRuleParser.getOccurrencesInRange(any(), eq(today), eq(2)))
                .thenReturn(dates);
        when(taskRepository.existsByRoutine_IdAndScheduledDate(routine.getId(), today))
                .thenReturn(true);
        when(taskRepository.existsByRoutine_IdAndScheduledDate(routine.getId(), today.plusDays(1)))
                .thenReturn(false);
        when(assignmentPolicyService.determineAssignee(routine)).thenReturn(null);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoutineSchedulerService.SchedulerResult result = schedulerService.generateUpcomingTasks();

        assertThat(result.tasksCreated()).isEqualTo(1);
        assertThat(result.tasksSkipped()).isEqualTo(1);
    }

    @Test
    void generateUpcomingTasks_skipsPausedRoutines() {
        when(routineRepository.findByStatus(RoutineStatus.ACTIVE)).thenReturn(List.of());

        RoutineSchedulerService.SchedulerResult result = schedulerService.generateUpcomingTasks();

        assertThat(result.routinesProcessed()).isZero();
        assertThat(result.tasksCreated()).isZero();
        verifyNoInteractions(taskRepository);
    }

    @Test
    void generateUpcomingTasks_skipsDeletedRoutines() {
        when(routineRepository.findByStatus(RoutineStatus.ACTIVE)).thenReturn(List.of());

        RoutineSchedulerService.SchedulerResult result = schedulerService.generateUpcomingTasks();

        assertThat(result.routinesProcessed()).isZero();
        assertThat(result.tasksCreated()).isZero();
        verifyNoInteractions(taskRepository);
    }

    @Test
    void generateUpcomingTasks_respectsWindowConfig() {
        Routine routine = buildRoutine("Daily", 5);

        when(routineRepository.findByStatus(RoutineStatus.ACTIVE)).thenReturn(List.of(routine));
        when(routineRepository.findByIdForUpdate(routine.getId())).thenReturn(Optional.of(routine));
        when(routineService.parseRecurrenceRule(routine.getRecurrenceRuleJson()))
                .thenReturn(new RecurrenceRule.Daily());
        when(recurrenceRuleParser.getOccurrencesInRange(any(), any(), eq(5))).thenReturn(List.of());

        schedulerService.generateUpcomingTasks();

        verify(recurrenceRuleParser).getOccurrencesInRange(any(), any(), eq(5));
    }

    @Test
    void generateUpcomingTasks_handlesErrorsGracefully() {
        Routine routineOk = buildRoutine("Ok", 1, "{\"type\":\"DAILY\"}");
        Routine routineFail = buildRoutine("Fail", 1, "{\"type\":\"WEEKLY\"}");

        when(routineRepository.findByStatus(RoutineStatus.ACTIVE)).thenReturn(List.of(routineOk, routineFail));
        // Use answer to return the correct routine based on input (both have null IDs)
        when(routineRepository.findByIdForUpdate(any()))
                .thenAnswer(invocation -> {
                    // First call returns routineOk, second call returns routineFail
                    return Optional.of(routineOk);
                })
                .thenReturn(Optional.of(routineFail));
        when(routineService.parseRecurrenceRule(routineOk.getRecurrenceRuleJson()))
                .thenReturn(new RecurrenceRule.Daily());
        when(routineService.parseRecurrenceRule(routineFail.getRecurrenceRuleJson()))
                .thenThrow(new IllegalStateException("bad json"));
        when(recurrenceRuleParser.getOccurrencesInRange(any(), any(), eq(1))).thenReturn(List.of(LocalDate.now()));
        when(taskRepository.existsByRoutine_IdAndScheduledDate(any(), any())).thenReturn(false);
        when(assignmentPolicyService.determineAssignee(any())).thenReturn(null);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoutineSchedulerService.SchedulerResult result = schedulerService.generateUpcomingTasks();

        assertThat(result.routinesProcessed()).isEqualTo(1);
        assertThat(result.tasksCreated()).isEqualTo(1);
        assertThat(result.errors()).isEqualTo(1);
    }

    private Routine buildRoutine(String title, int windowDays) {
        return buildRoutine(title, windowDays, "{\"type\":\"DAILY\"}");
    }

    private Routine buildRoutine(String title, int windowDays, String ruleJson) {
        Household household = new Household("Household " + UUID.randomUUID());
        User user = new User("ext-" + UUID.randomUUID(), "test@example.com", "Test User");
        Routine routine = new Routine(household, title, ruleJson, AssignmentPolicy.MANUAL, user);
        routine.setGenerationWindowDays(windowDays);
        return routine;
    }
}
