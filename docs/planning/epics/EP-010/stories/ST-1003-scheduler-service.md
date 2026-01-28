# Story: ST-1003 — RoutineSchedulerService + Idempotent Generation

## Sources of Truth
- Epic: `docs/planning/epics/EP-010/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q3-recurring-tasks-scheduling.md`
- ADR-012: `docs/architecture/decisions/012-command-reliability-idempotency.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Draft** — Awaiting Human Gate approval

## User Value
> "Создал рутину — и задачи появляются сами на неделю вперёд. Не нужно каждый день создавать вручную."

---

## Description
Implement scheduler service that:
- Runs periodically (e.g., every hour or daily)
- For each ACTIVE routine, generates task instances for next N days
- Idempotent: duplicate runs do not create duplicate tasks
- Handles catch-up (if scheduler was down, generates missed dates)

---

## In Scope
- `RoutineSchedulerService` with generation logic
- Scheduled job (Spring `@Scheduled` or configurable cron)
- Idempotency via unique constraint (routineId, scheduledDate)
- Generation window: 7 days ahead (from routine config)
- Task creation with `routineId` and `scheduledDate` set
- Skip PAUSED and DELETED routines
- Basic logging/observability

## Out of Scope
- Assignment logic (ST-1004) — uses MANUAL policy initially
- UI for upcoming instances (ST-1006)
- Points for generated tasks (uses existing EP-009 flow)
- Notifications for generated tasks (uses existing EP-007 flow)

---

## Acceptance Criteria

### AC-1: Scheduler generates tasks for active routines
```
Given ACTIVE routine "Clean kitchen" with DAILY rule
And generation window = 7 days
And today = 2026-01-28
When scheduler runs
Then tasks created for dates: 2026-01-28 through 2026-02-03
And each task has:
  - routineId = routine.id
  - scheduledDate = corresponding date
  - title = routine.title
  - zone = routine.zone
  - status = OPEN
  - createdVia = "routine"
```

### AC-2: Idempotent - no duplicates on re-run
```
Given routine with DAILY rule
And tasks already exist for 2026-01-28 through 2026-02-03
When scheduler runs again
Then no new tasks created for those dates
And existing tasks unchanged
```

### AC-3: Catch-up after downtime
```
Given routine with DAILY rule
And scheduler was down for 2 days
And last generated task was for 2026-01-26
And today = 2026-01-28
When scheduler runs
Then tasks created for: 2026-01-27, 2026-01-28, and next 7 days
```

### AC-4: PAUSED routines skipped
```
Given routine with status = PAUSED
When scheduler runs
Then no new tasks generated for this routine
```

### AC-5: DELETED routines skipped
```
Given routine with status = DELETED
When scheduler runs
Then no new tasks generated for this routine
```

### AC-6: Generation respects window config
```
Given routine with generationWindowDays = 14
When scheduler runs
Then tasks generated for next 14 days (not default 7)
```

### AC-7: Task inherits routine properties
```
Given routine with:
  - title = "Помыть пол"
  - description = "Влажная уборка"
  - zone = Kitchen
When task generated
Then task has:
  - title = "Помыть пол"
  - description = "Влажная уборка"
  - zone = Kitchen
  - deadline = scheduledDate at 23:59 (end of day)
```

### AC-8: Scheduler logs execution
```
Given scheduler run
Then log entry created with:
  - routines processed count
  - tasks generated count
  - tasks skipped (already exist) count
  - duration ms
```

### AC-9: Scheduler handles errors gracefully
```
Given routine A succeeds
And routine B has invalid rule (throws)
When scheduler runs
Then routine A tasks created
And routine B error logged
And scheduler continues to next routine
```

### AC-10: Unique constraint enforced at DB level
```
Given task with routineId=X and scheduledDate=2026-01-28 exists
When attempting to insert another task with same routineId and scheduledDate
Then DB constraint violation
And service handles gracefully (skip, not throw)
```

---

## Test Strategy

### Unit Tests
- `RoutineSchedulerService`:
  - `generateUpcomingTasks_createsTasksForWindow`
  - `generateUpcomingTasks_skipsExistingDates`
  - `generateUpcomingTasks_skipsPausedRoutines`
  - `generateUpcomingTasks_handlesErrorsGracefully`

### Integration Tests
- `RoutineSchedulerServiceIntegrationTest`:
  - `scheduler_createsTasksInDb`
  - `scheduler_idempotent_noDuplicates`
  - `scheduler_catchUp_generatesMissedDates`
  - `scheduler_respectsWindowConfig`

### Manual Test
- Start app, create routine, wait for scheduler tick, verify tasks created

---

## Points
**5 points**

## Dependencies
- ST-1001 (Routine entity)
- ST-1002 (RecurrenceRuleParser)

## Flags
- contract_impact: no (internal scheduler, no new endpoints)
- adr_needed: lite (idempotency strategy documented in epic)
- diagrams_needed: no
- security_sensitive: no (operates on household-scoped data already)
