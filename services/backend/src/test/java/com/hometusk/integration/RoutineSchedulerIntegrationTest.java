package com.hometusk.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hometusk.routines.domain.AssignmentPolicy;
import com.hometusk.routines.domain.RecurrenceRule;
import com.hometusk.routines.domain.Routine;
import com.hometusk.routines.repository.RoutineRepository;
import com.hometusk.routines.service.RoutineSchedulerService;
import com.hometusk.tasks.domain.Task;
import com.hometusk.tasks.repository.TaskRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("RoutineScheduler Integration Tests")
class RoutineSchedulerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private RoutineSchedulerService schedulerService;

    @Autowired
    private RoutineRepository routineRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    @DisplayName("AC-1: Scheduler creates tasks for window")
    void scheduler_createsTasksInDb() throws Exception {
        Routine routine = saveRoutine(2);

        schedulerService.generateUpcomingTasks();

        List<Task> tasks = tasksForRoutine(routine);
        assertThat(tasks).hasSize(2);
        assertThat(tasks).extracting(Task::getCreatedVia).containsOnly("scheduler");
        assertThat(tasks).allMatch(task -> task.getScheduledDate() != null);
    }

    @Test
    @DisplayName("AC-2: Scheduler is idempotent")
    void scheduler_idempotent_noDuplicates() throws Exception {
        Routine routine = saveRoutine(2);

        schedulerService.generateUpcomingTasks();
        schedulerService.generateUpcomingTasks();

        List<Task> tasks = tasksForRoutine(routine);
        assertThat(tasks).hasSize(2);
    }

    @Test
    @DisplayName("AC-3: Scheduler does not backfill past dates")
    void scheduler_noBackfillForPastDates() throws Exception {
        Routine routine = saveRoutine(3);

        schedulerService.generateUpcomingTasks();

        LocalDate today = LocalDate.now();
        List<Task> tasks = tasksForRoutine(routine);
        assertThat(tasks).extracting(Task::getScheduledDate).allMatch(date -> !date.isBefore(today));
    }

    @Test
    @DisplayName("AC-6: Scheduler respects window config")
    void scheduler_respectsWindowConfig() throws Exception {
        Routine routine = saveRoutine(1);

        schedulerService.generateUpcomingTasks();

        List<Task> tasks = tasksForRoutine(routine);
        assertThat(tasks).hasSize(1);
    }

    private Routine saveRoutine(int windowDays) throws Exception {
        String ruleJson = objectMapper.writeValueAsString(new RecurrenceRule.Daily());
        Routine routine = new Routine(testHousehold, "Daily Routine", ruleJson, AssignmentPolicy.MANUAL, testUser);
        routine.setGenerationWindowDays(windowDays);
        return routineRepository.save(routine);
    }

    private List<Task> tasksForRoutine(Routine routine) {
        return taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId()).stream()
                .filter(task -> routine.getId().equals(task.getRoutineId()))
                .toList();
    }
}
