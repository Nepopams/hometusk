# Workpack: ST-1003 — RoutineSchedulerService + Idempotent Generation

## Sources of Truth
- Story: `docs/planning/epics/EP-010/stories/ST-1003-scheduler-service.md`
- Epic: `docs/planning/epics/EP-010/epic.md`
- ADR-012: `docs/architecture/decisions/012-command-reliability-idempotency.md`
- ADR-013: `docs/adr/013-routine-scheduler-design.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- Existing Task entity: `services/backend/src/main/java/com/hometusk/tasks/domain/Task.java`

---

## Goal
Implement scheduler service that automatically generates task instances from ACTIVE routines with idempotent deduplication (no duplicates on re-run, no backfill for past dates).

## Scope: In / Out

### In Scope
- Add `routineId` and `scheduledDate` fields to Task entity
- DB migration for unique partial index `(routine_id, scheduled_date) WHERE routine_id IS NOT NULL`
- `RoutineSchedulerService` with `generateUpcomingTasks()` method
- `@Scheduled` job configuration (hourly or configurable)
- Idempotency via unique constraint + graceful handling of duplicates
- Skip PAUSED and DELETED routines
- Generation window from routine config (default 7 days)
- Structured logging with metrics (routines processed, tasks created, skipped)

### Out of Scope
- Assignment logic (ST-1004) — tasks created with no assignee (MANUAL policy)
- UI for upcoming instances (ST-1006)
- Backfill for missed dates (v0 policy: no backfill)
- Distributed scheduler lock (v0: single instance)

---

## Anchors (non-negotiables)
| Artifact | Path |
|----------|------|
| Story Spec | `docs/planning/epics/EP-010/stories/ST-1003-scheduler-service.md` |
| Epic | `docs/planning/epics/EP-010/epic.md` |
| Task Entity | `services/backend/src/main/java/com/hometusk/tasks/domain/Task.java` |
| TaskRepository | `services/backend/src/main/java/com/hometusk/tasks/repository/TaskRepository.java` |
| RecurrenceRuleParser | `services/backend/src/main/java/com/hometusk/routines/service/RecurrenceRuleParser.java` |
| RoutineRepository | `services/backend/src/main/java/com/hometusk/routines/repository/RoutineRepository.java` |

---

## Plan Steps

### Step 1-2: SKIPPED (Already Implemented in ST-1001/V021)
**Status:** ✅ Already exists

**Evidence:**
- Task entity: `routine` (FK) and `scheduledDate` fields exist (`Task.java:43-46`)
- V021 migration: columns + CHECK constraint + unique partial index (`V021__create_routines.sql:27-35`)

No action required for these steps.

---

### Step 3: Update TaskRepository with routine lookup method
**Description:** Add method to check if task already exists for routine+date.

**Expected Result:**
- `existsByRoutineIdAndScheduledDate(UUID routineId, LocalDate scheduledDate)`
- Or rely on unique constraint and catch `DataIntegrityViolationException`

**Files touched:**
- MODIFY: `services/backend/src/main/java/com/hometusk/tasks/repository/TaskRepository.java`

### Step 4: Create RoutineSchedulerService
**Description:** Main scheduler logic for generating task instances.

**Expected Result:**
- `@Service` class with `RecurrenceRuleParser`, `RoutineRepository`, `TaskRepository` injected
- Method `generateUpcomingTasks()` that:
  1. Finds all ACTIVE routines
  2. For each routine, calculates dates in [today, today+windowDays]
  3. For each date, attempts to create task (skip if exists)
  4. Logs metrics

**Files touched:**
- CREATE: `services/backend/src/main/java/com/hometusk/routines/service/RoutineSchedulerService.java`

### Step 5: Add task creation method to TaskService or RoutineSchedulerService
**Description:** Create task instance from routine with proper field mapping.

**Expected Result:**
- Task created with: title, description, zone from routine
- routineId, scheduledDate set
- deadline = scheduledDate at 23:59:59 (end of day)
- assignee = null (MANUAL policy for v0)
- status = OPEN

**Files touched:**
- MODIFY: `services/backend/src/main/java/com/hometusk/routines/service/RoutineSchedulerService.java`
- MODIFY: `services/backend/src/main/java/com/hometusk/tasks/domain/Task.java` (constructor or builder)

### Step 6: Configure @Scheduled job
**Description:** Set up periodic execution of scheduler.

**Expected Result:**
- `@Scheduled(cron = "0 0 * * * *")` or `fixedRate` for hourly
- Configurable via `application.yml`: `hometusk.scheduler.cron`
- Enable scheduling with `@EnableScheduling`

**Files touched:**
- CREATE: `services/backend/src/main/java/com/hometusk/routines/scheduler/RoutineSchedulerJob.java`
- MODIFY: `services/backend/src/main/resources/application.yml` (scheduler config)
- MODIFY: `services/backend/src/main/java/com/hometusk/HometuskBackendApplication.java` (if @EnableScheduling needed)

### Step 7: Add unit tests for RoutineSchedulerService
**Description:** Unit tests with mocked dependencies.

**Expected Result:**
- `generateUpcomingTasks_createsTasksForWindow`
- `generateUpcomingTasks_skipsExistingDates`
- `generateUpcomingTasks_skipsPausedRoutines`
- `generateUpcomingTasks_skipsDeletedRoutines`
- `generateUpcomingTasks_respectsWindowConfig`
- `generateUpcomingTasks_handlesErrorsGracefully`

**Files touched:**
- CREATE: `services/backend/src/test/java/com/hometusk/routines/service/RoutineSchedulerServiceTest.java`

### Step 8: Add integration tests
**Description:** Integration tests with real DB.

**Expected Result:**
- `scheduler_createsTasksInDb`
- `scheduler_idempotent_noDuplicates`
- `scheduler_noBackfillForPastDates`
- `scheduler_respectsWindowConfig`

**Files touched:**
- CREATE: `services/backend/src/test/java/com/hometusk/integration/RoutineSchedulerIntegrationTest.java`

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `Task.java` | MODIFY | Add routineId, scheduledDate fields |
| `V*__add_routine_fields_to_tasks.sql` | CREATE | DB migration |
| `TaskRepository.java` | MODIFY | Add routine lookup method |
| `RoutineSchedulerService.java` | CREATE | Main scheduler logic |
| `RoutineSchedulerJob.java` | CREATE | @Scheduled job wrapper |
| `application.yml` | MODIFY | Scheduler cron config |
| `RoutineSchedulerServiceTest.java` | CREATE | Unit tests |
| `RoutineSchedulerIntegrationTest.java` | CREATE | Integration tests |

---

## Tests & Checks

### Required Test Methods
| Test Class | Method | AC |
|------------|--------|-----|
| RoutineSchedulerServiceTest | `generateUpcomingTasks_createsTasksForWindow` | AC-1 |
| RoutineSchedulerServiceTest | `generateUpcomingTasks_skipsExistingDates` | AC-2 |
| RoutineSchedulerServiceTest | `generateUpcomingTasks_skipsPausedRoutines` | AC-4 |
| RoutineSchedulerServiceTest | `generateUpcomingTasks_skipsDeletedRoutines` | AC-5 |
| RoutineSchedulerServiceTest | `generateUpcomingTasks_respectsWindowConfig` | AC-6 |
| RoutineSchedulerServiceTest | `generateUpcomingTasks_handlesErrorsGracefully` | AC-9 |
| RoutineSchedulerIntegrationTest | `scheduler_createsTasksInDb` | AC-1 |
| RoutineSchedulerIntegrationTest | `scheduler_idempotent_noDuplicates` | AC-2, AC-10 |
| RoutineSchedulerIntegrationTest | `scheduler_noBackfillForPastDates` | AC-3 |

### Commands to Run
```bash
cd services/backend && ./gradlew flywayMigrate
cd services/backend && ./gradlew test --tests "com.hometusk.routines.service.RoutineSchedulerServiceTest"
cd services/backend && ./gradlew test --tests "com.hometusk.integration.RoutineSchedulerIntegrationTest"
cd services/backend && ./gradlew spotlessApply
cd services/backend && ./gradlew build
```

---

## Contract Impact
None — internal scheduler, no new API endpoints.

---

## Docs Updates
None required.

---

## Rollout / Rollback

### Rollout
- DB migration adds nullable columns (backward compatible)
- Scheduler disabled by default via config flag
- Enable scheduler after verification: `hometusk.scheduler.enabled=true`

### Rollback Steps
- Disable scheduler in config
- Revert migration (drop columns + index)
- Revert code changes

---

## Done Criteria

| AC | Criteria | Verification |
|----|----------|--------------|
| AC-1 | Scheduler generates tasks for window | Integration test |
| AC-2 | Idempotent - no duplicates | Integration test |
| AC-3 | No backfill for past dates | Integration test |
| AC-4 | PAUSED routines skipped | Unit test |
| AC-5 | DELETED routines skipped | Unit test |
| AC-6 | Window config respected | Unit + Integration test |
| AC-7 | Task inherits routine properties | Unit test |
| AC-8 | Scheduler logs execution | Manual verification |
| AC-9 | Errors handled gracefully | Unit test |
| AC-10 | Unique constraint enforced | Integration test |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Unique constraint violation race | Task duplication | DB constraint + graceful handling |
| Long-running scheduler blocks | Delayed generation | Async processing, batch commits |
| Invalid recurrence rule crashes | No tasks generated | Try-catch per routine, log error |
| Migration breaks existing tasks | Data loss | Nullable columns, no data change |

---

## Prompt Pack
- PLAN: `docs/planning/workpacks/ST-1003/prompt-plan.md`
- APPLY: `docs/planning/workpacks/ST-1003/prompt-apply.md`
