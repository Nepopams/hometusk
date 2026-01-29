package com.hometusk.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hometusk.routines.domain.AssignmentPolicy;
import com.hometusk.routines.domain.RecurrenceRule;
import com.hometusk.routines.domain.RoundRobinState;
import com.hometusk.routines.domain.Routine;
import com.hometusk.routines.repository.RoutineRepository;
import com.hometusk.routines.service.RoutineSchedulerService;
import com.hometusk.tasks.domain.Task;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import com.hometusk.users.domain.User;
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

    @Test
    @DisplayName("AC-1: Fixed policy assigns to configured user")
    void scheduler_withFixedPolicy_assignsToFixedUser() throws Exception {
        addMember(testUser2);
        Routine routine = saveRoutineWithPolicy(AssignmentPolicy.FIXED, testUser2, 1);

        schedulerService.generateUpcomingTasks();

        List<Task> tasks = tasksForRoutine(routine);
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getAssigneeId()).isEqualTo(testUser2.getId());
    }

    @Test
    @DisplayName("AC-7: Manual policy leaves assignee null")
    void scheduler_withManualPolicy_noAssignee() throws Exception {
        Routine routine = saveRoutineWithPolicy(AssignmentPolicy.MANUAL, null, 1);

        schedulerService.generateUpcomingTasks();

        List<Task> tasks = tasksForRoutine(routine);
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getAssignee()).isNull();
    }

    @Test
    @DisplayName("AC-3: Round-robin rotates members")
    void scheduler_withRoundRobin_rotatesMembers() throws Exception {
        addMember(testUser2);
        Routine routine = saveRoutineWithPolicy(AssignmentPolicy.ROUND_ROBIN, null, 2);

        schedulerService.generateUpcomingTasks();

        List<Task> tasks = tasksForRoutine(routine).stream()
                .sorted((a, b) -> a.getScheduledDate().compareTo(b.getScheduledDate()))
                .toList();

        assertThat(tasks).hasSize(2);
        assertThat(tasks.get(0).getAssigneeId()).isEqualTo(testUser.getId());
        assertThat(tasks.get(1).getAssigneeId()).isEqualTo(testUser2.getId());
    }

    @Test
    @DisplayName("AC-4: Round-robin state persists across runs")
    void scheduler_withRoundRobin_statePersistedAcrossRuns() throws Exception {
        addMember(testUser2);
        Routine routine = saveRoutineWithPolicy(AssignmentPolicy.ROUND_ROBIN, null, 1);

        schedulerService.generateUpcomingTasks();

        RoundRobinState stateAfterFirstRun = objectMapper.readValue(
                routineRepository.findById(routine.getId()).orElseThrow().getRoundRobinStateJson(),
                RoundRobinState.class);
        assertThat(stateAfterFirstRun.lastAssignedUserId()).isEqualTo(testUser.getId());

        taskRepository.deleteAll(tasksForRoutine(routine));
        taskRepository.flush();

        schedulerService.generateUpcomingTasks();

        RoundRobinState stateAfterSecondRun = objectMapper.readValue(
                routineRepository.findById(routine.getId()).orElseThrow().getRoundRobinStateJson(),
                RoundRobinState.class);
        assertThat(stateAfterSecondRun.lastAssignedUserId()).isEqualTo(testUser2.getId());
    }

    private Routine saveRoutine(int windowDays) throws Exception {
        return saveRoutineWithPolicy(AssignmentPolicy.MANUAL, null, windowDays);
    }

    private Routine saveRoutineWithPolicy(AssignmentPolicy policy, User fixedAssignee, int windowDays)
            throws Exception {
        String ruleJson = objectMapper.writeValueAsString(new RecurrenceRule.Daily());
        Routine routine = new Routine(testHousehold, "Daily Routine", ruleJson, policy, testUser);
        routine.setGenerationWindowDays(windowDays);
        if (policy == AssignmentPolicy.FIXED) {
            routine.setFixedAssignee(fixedAssignee);
        }
        return routineRepository.save(routine);
    }

    private void addMember(User user) {
        if (membershipRepository.existsByUser_IdAndHousehold_Id(user.getId(), testHousehold.getId())) {
            return;
        }
        Membership membership = new Membership(user, testHousehold, MembershipRole.member);
        membershipRepository.save(membership);
    }

    private List<Task> tasksForRoutine(Routine routine) {
        return taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId()).stream()
                .filter(task -> routine.getId().equals(task.getRoutineId()))
                .toList();
    }
}
