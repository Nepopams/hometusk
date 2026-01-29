package com.hometusk.routines.service;

import com.hometusk.routines.domain.RecurrenceRule;
import com.hometusk.routines.domain.Routine;
import com.hometusk.routines.domain.RoutineStatus;
import com.hometusk.routines.repository.RoutineRepository;
import com.hometusk.tasks.domain.Task;
import com.hometusk.tasks.repository.TaskRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoutineSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(RoutineSchedulerService.class);

    private final RoutineRepository routineRepository;
    private final TaskRepository taskRepository;
    private final RecurrenceRuleParser recurrenceRuleParser;
    private final RoutineService routineService;
    private final AssignmentPolicyService assignmentPolicyService;

    public RoutineSchedulerService(
            RoutineRepository routineRepository,
            TaskRepository taskRepository,
            RecurrenceRuleParser recurrenceRuleParser,
            RoutineService routineService,
            AssignmentPolicyService assignmentPolicyService) {
        this.routineRepository = routineRepository;
        this.taskRepository = taskRepository;
        this.recurrenceRuleParser = recurrenceRuleParser;
        this.routineService = routineService;
        this.assignmentPolicyService = assignmentPolicyService;
    }

    /**
     * Generate upcoming task instances for all ACTIVE routines.
     * Idempotent: skips dates where task already exists.
     * No backfill: only generates for [today, today + windowDays).
     */
    @Transactional
    public SchedulerResult generateUpcomingTasks() {
        log.info("Starting routine scheduler run");

        List<Routine> activeRoutines = routineRepository.findByStatus(RoutineStatus.ACTIVE);
        log.info("Found {} active routines", activeRoutines.size());

        int routinesProcessed = 0;
        int tasksCreated = 0;
        int tasksSkipped = 0;
        int errors = 0;

        for (Routine routine : activeRoutines) {
            try {
                RoutineResult result = generateTasksForRoutine(routine);
                tasksCreated += result.created();
                tasksSkipped += result.skipped();
                routinesProcessed++;
            } catch (Exception e) {
                log.error("Error processing routine {}: {}", routine.getId(), e.getMessage(), e);
                errors++;
            }
        }

        log.info(
                "Scheduler run complete: routines={}, created={}, skipped={}, errors={}",
                routinesProcessed,
                tasksCreated,
                tasksSkipped,
                errors);

        return new SchedulerResult(routinesProcessed, tasksCreated, tasksSkipped, errors);
    }

    private RoutineResult generateTasksForRoutine(Routine routine) {
        Routine lockedRoutine = routineRepository
                .findByIdForUpdate(routine.getId())
                .orElseThrow(() -> new IllegalStateException("Routine not found: " + routine.getId()));
        LocalDate today = LocalDate.now();
        int windowDays = lockedRoutine.getGenerationWindowDays();
        LocalDate endDateExclusive = today.plusDays(windowDays);

        RecurrenceRule rule = routineService.parseRecurrenceRule(lockedRoutine.getRecurrenceRuleJson());
        List<LocalDate> dates = recurrenceRuleParser.getOccurrencesInRange(rule, today, windowDays).stream()
                .filter(date -> date.isBefore(endDateExclusive))
                .toList();

        int created = 0;
        int skipped = 0;

        for (LocalDate date : dates) {
            if (taskRepository.existsByRoutine_IdAndScheduledDate(lockedRoutine.getId(), date)) {
                skipped++;
                continue;
            }

            try {
                createTaskForDate(lockedRoutine, date);
                created++;
            } catch (DataIntegrityViolationException e) {
                log.debug("Task already exists for routine {} on {}", lockedRoutine.getId(), date);
                skipped++;
            }
        }

        log.debug("Routine {}: created={}, skipped={}", lockedRoutine.getId(), created, skipped);
        return new RoutineResult(created, skipped);
    }

    private void createTaskForDate(Routine routine, LocalDate date) {
        String previousState = routine.getRoundRobinStateJson();

        Task task = new Task(routine.getHousehold(), routine.getTitle(), routine.getCreatedBy());
        task.setDescription(routine.getDescription());
        task.setZone(routine.getZone());
        task.setRoutine(routine);
        task.setScheduledDate(date);

        Instant deadline =
                date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        task.setDeadline(deadline);

        task.setCreatedVia("scheduler");

        if (routine.getAssignmentPolicy() != null) {
            var assignee = assignmentPolicyService.determineAssignee(routine);
            if (assignee != null) {
                task.setAssignee(assignee);
            }
        }

        try {
            taskRepository.save(task);
        } catch (DataIntegrityViolationException e) {
            routine.setRoundRobinStateJson(previousState);
            throw e;
        }
    }

    public record SchedulerResult(int routinesProcessed, int tasksCreated, int tasksSkipped, int errors) {}

    private record RoutineResult(int created, int skipped) {}
}
