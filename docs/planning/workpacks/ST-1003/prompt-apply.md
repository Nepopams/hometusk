# Codex APPLY Prompt: ST-1003 — RoutineSchedulerService + Idempotent Generation

## Mode: APPLY (Implementation)

**CRITICAL:** This is the implementation phase. You MAY edit files.

---

## Anchors (read first)

```
CLAUDE.md (project root)
docs/planning/workpacks/ST-1003/workpack.md
docs/planning/epics/EP-010/stories/ST-1003-scheduler-service.md
docs/planning/epics/EP-010/epic.md
docs/adr/013-routine-scheduler-design.md
docs/_governance/dod.md
```

---

## PLAN Phase Findings (incorporated)

### Verified State
- Task entity already has `routine` (FK) and `scheduledDate` fields
- V021 migration already includes unique partial index `idx_task_routine_scheduled_date`
- `@EnableScheduling` exists in `SchedulingConfig.java`
- RecurrenceRuleParser: `getOccurrencesInRange(rule, fromDate, count)` available
- RoutineRepository: has status-based finders

### Clarifications Applied
1. **No scheduler lock for v0** — single instance, rely on DB unique constraint
2. **Create Task directly** — NOT via TaskService (need `createdVia="scheduler"`, routine fields)
3. **Use fixedRate** — configurable via `hometusk.scheduler.fixed-rate-ms`

### Adjusted Scope
- SKIP Steps 1-2 (Task fields + migration already exist)
- IMPLEMENT Steps 3-8

---

## Implementation Steps

### Step 3: Add TaskRepository method

**File:** `services/backend/src/main/java/com/hometusk/tasks/repository/TaskRepository.java`

**Add method:**
```java
/**
 * Check if task already exists for routine on scheduled date (idempotency).
 */
boolean existsByRoutine_IdAndScheduledDate(UUID routineId, LocalDate scheduledDate);
```

### Step 4: Add RoutineRepository method for active routines

**File:** `services/backend/src/main/java/com/hometusk/routines/repository/RoutineRepository.java`

**Add method:**
```java
/**
 * Find all ACTIVE routines (for scheduler).
 */
List<Routine> findByStatus(RoutineStatus status);
```

### Step 5: Create RoutineSchedulerService

**File:** `services/backend/src/main/java/com/hometusk/routines/service/RoutineSchedulerService.java`

```java
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

    public RoutineSchedulerService(
            RoutineRepository routineRepository,
            TaskRepository taskRepository,
            RecurrenceRuleParser recurrenceRuleParser,
            RoutineService routineService) {
        this.routineRepository = routineRepository;
        this.taskRepository = taskRepository;
        this.recurrenceRuleParser = recurrenceRuleParser;
        this.routineService = routineService;
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

        log.info("Scheduler run complete: routines={}, created={}, skipped={}, errors={}",
                routinesProcessed, tasksCreated, tasksSkipped, errors);

        return new SchedulerResult(routinesProcessed, tasksCreated, tasksSkipped, errors);
    }

    private RoutineResult generateTasksForRoutine(Routine routine) {
        LocalDate today = LocalDate.now();
        int windowDays = routine.getGenerationWindowDays();

        RecurrenceRule rule = routineService.parseRecurrenceRule(routine.getRecurrenceRuleJson());
        List<LocalDate> dates = recurrenceRuleParser.getOccurrencesInRange(rule, today, windowDays);

        int created = 0;
        int skipped = 0;

        for (LocalDate date : dates) {
            // Check if task already exists (idempotency)
            if (taskRepository.existsByRoutine_IdAndScheduledDate(routine.getId(), date)) {
                skipped++;
                continue;
            }

            try {
                createTaskForDate(routine, date);
                created++;
            } catch (DataIntegrityViolationException e) {
                // Race condition: another process created the task
                log.debug("Task already exists for routine {} on {}", routine.getId(), date);
                skipped++;
            }
        }

        log.debug("Routine {}: created={}, skipped={}", routine.getId(), created, skipped);
        return new RoutineResult(created, skipped);
    }

    private void createTaskForDate(Routine routine, LocalDate date) {
        Task task = new Task(routine.getHousehold(), routine.getTitle(), routine.getCreatedBy());
        task.setDescription(routine.getDescription());
        task.setZone(routine.getZone());
        task.setRoutine(routine);
        task.setScheduledDate(date);

        // Deadline = end of scheduled day (23:59:59 in system timezone)
        Instant deadline = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        task.setDeadline(deadline);

        task.setCreatedVia("scheduler");

        // v0: MANUAL policy - no assignee
        // ST-1004 will add assignment logic

        taskRepository.save(task);
    }

    public record SchedulerResult(int routinesProcessed, int tasksCreated, int tasksSkipped, int errors) {}
    private record RoutineResult(int created, int skipped) {}
}
```

### Step 6: Create RoutineSchedulerJob

**File:** `services/backend/src/main/java/com/hometusk/routines/scheduler/RoutineSchedulerJob.java`

```java
package com.hometusk.routines.scheduler;

import com.hometusk.routines.service.RoutineSchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "hometusk.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class RoutineSchedulerJob {

    private static final Logger log = LoggerFactory.getLogger(RoutineSchedulerJob.class);

    private final RoutineSchedulerService schedulerService;

    public RoutineSchedulerJob(RoutineSchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Scheduled(fixedRateString = "${hometusk.scheduler.fixed-rate-ms:3600000}")
    public void runScheduler() {
        log.info("Scheduled routine task generation triggered");
        try {
            var result = schedulerService.generateUpcomingTasks();
            log.info("Scheduled run completed: {}", result);
        } catch (Exception e) {
            log.error("Scheduled run failed", e);
        }
    }
}
```

### Step 7: Add application.yml config

**File:** `services/backend/src/main/resources/application.yml`

**Add section:**
```yaml
hometusk:
  scheduler:
    enabled: false  # Enable in production after verification
    fixed-rate-ms: 3600000  # 1 hour
```

### Step 8: Unit Tests

**File:** `services/backend/src/test/java/com/hometusk/routines/service/RoutineSchedulerServiceTest.java`

Create unit tests with mocked dependencies covering:
- `generateUpcomingTasks_createsTasksForWindow` — verifies tasks created for windowDays
- `generateUpcomingTasks_skipsExistingDates` — verifies idempotency via existsBy...
- `generateUpcomingTasks_skipsPausedRoutines` — only ACTIVE routines processed
- `generateUpcomingTasks_skipsDeletedRoutines` — only ACTIVE routines processed
- `generateUpcomingTasks_respectsWindowConfig` — uses routine.generationWindowDays
- `generateUpcomingTasks_handlesErrorsGracefully` — continues on single routine failure

### Step 9: Integration Tests

**File:** `services/backend/src/test/java/com/hometusk/integration/RoutineSchedulerIntegrationTest.java`

Create integration tests with real DB (Testcontainers) covering:
- `scheduler_createsTasksInDb` — tasks persist with correct fields
- `scheduler_idempotent_noDuplicates` — running twice doesn't duplicate
- `scheduler_noBackfillForPastDates` — only future dates in window
- `scheduler_respectsWindowConfig` — honors generationWindowDays

---

## Files to Create/Modify Summary

| File | Action |
|------|--------|
| `TaskRepository.java` | ADD method |
| `RoutineRepository.java` | ADD method |
| `RoutineSchedulerService.java` | CREATE |
| `RoutineSchedulerJob.java` | CREATE (new package: scheduler) |
| `application.yml` | ADD config section |
| `RoutineSchedulerServiceTest.java` | CREATE |
| `RoutineSchedulerIntegrationTest.java` | CREATE |

---

## Verification Commands

```bash
# Format code
cd services/backend && ./gradlew spotlessApply

# Run unit tests
cd services/backend && ./gradlew test --tests "com.hometusk.routines.service.RoutineSchedulerServiceTest"

# Run integration tests
cd services/backend && ./gradlew test --tests "com.hometusk.integration.RoutineSchedulerIntegrationTest"

# Full build
cd services/backend && ./gradlew build
```

---

## STOP-THE-LINE Rules

Stop and report if:
- RoutineService.parseRecurrenceRule() doesn't exist (need to add or use different approach)
- Task constructor doesn't accept household/title/createdBy
- RecurrenceRuleParser signature different than expected
- Unique constraint name different than documented
- Any compilation error

---

## DoD Checklist (verify at end)

- [ ] Code follows project conventions (Java 21, Spring Boot idioms)
- [ ] Spotless formatting applied
- [ ] Unit tests written and passing (6+ test cases)
- [ ] Integration tests written and passing (4+ test cases)
- [ ] Scheduler disabled by default (config flag)
- [ ] No cross-household data leaks (scheduler processes all ACTIVE routines)
- [ ] Idempotency verified (unique constraint + graceful handling)
- [ ] Logs include sufficient context for debugging
- [ ] All tests pass: `./gradlew build`
