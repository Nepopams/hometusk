# Checklist: ST-1003 — RoutineSchedulerService + Idempotent Generation

## Sources of Truth
- Story: `docs/planning/epics/EP-010/stories/ST-1003-scheduler-service.md`
- Workpack: `docs/planning/workpacks/ST-1003/workpack.md`
- DoD: `docs/_governance/dod.md`

---

## Acceptance Criteria Verification

| AC | Criteria | Test Method | Status |
|----|----------|-------------|--------|
| AC-1 | Scheduler generates tasks for [today, today+windowDays] | Integration test | [ ] |
| AC-2 | Idempotent - no duplicates on re-run | Integration test | [ ] |
| AC-3 | No backfill for past dates | Integration test | [ ] |
| AC-4 | PAUSED routines skipped | Unit test | [ ] |
| AC-5 | DELETED routines skipped | Unit test | [ ] |
| AC-6 | Window config respected (routine.generationWindowDays) | Unit + Integration | [ ] |
| AC-7 | Task inherits routine properties (title, description, zone) | Unit test | [ ] |
| AC-8 | Scheduler logs execution metrics | Manual verification | [ ] |
| AC-9 | Errors handled gracefully (continue on failure) | Unit test | [ ] |
| AC-10 | Unique constraint enforced in DB | Integration test | [ ] |

---

## Code Quality Checklist

- [ ] Code follows project conventions (Java 21, Spring Boot idioms)
- [ ] Spotless formatting applied (`./gradlew spotlessApply`)
- [ ] No compiler warnings introduced
- [ ] Code reviewed by at least one peer (PR review)

---

## Test Checklist

### Unit Tests (RoutineSchedulerServiceTest)
- [ ] `generateUpcomingTasks_createsTasksForWindow`
- [ ] `generateUpcomingTasks_skipsExistingDates`
- [ ] `generateUpcomingTasks_skipsPausedRoutines`
- [ ] `generateUpcomingTasks_skipsDeletedRoutines`
- [ ] `generateUpcomingTasks_respectsWindowConfig`
- [ ] `generateUpcomingTasks_handlesErrorsGracefully`
- [ ] `createTaskForDate_setsCorrectFields`

### Integration Tests (RoutineSchedulerIntegrationTest)
- [ ] `scheduler_createsTasksInDb`
- [ ] `scheduler_idempotent_noDuplicates`
- [ ] `scheduler_noBackfillForPastDates`
- [ ] `scheduler_respectsWindowConfig`

### Test Commands
```bash
cd services/backend && ./gradlew test --tests "com.hometusk.routines.service.RoutineSchedulerServiceTest"
cd services/backend && ./gradlew test --tests "com.hometusk.integration.RoutineSchedulerIntegrationTest"
cd services/backend && ./gradlew build
```

---

## Documentation Checklist

- [ ] No API contract changes (internal scheduler)
- [ ] No ADR changes required (ADR-013 covers design)
- [ ] No diagram changes required

---

## Security Checklist

- [ ] No cross-household data leaks (scheduler processes ACTIVE routines globally, each task scoped to routine's household)
- [ ] No hardcoded secrets
- [ ] Scheduler disabled by default (config flag)

---

## Observability Checklist

- [ ] Scheduler logs: start, routine count, tasks created/skipped/errors, complete
- [ ] Error logging includes routine ID and exception details
- [ ] Metrics: SchedulerResult record captures counts

---

## Configuration Checklist

- [ ] `hometusk.scheduler.enabled=false` by default
- [ ] `hometusk.scheduler.fixed-rate-ms` configurable
- [ ] `@ConditionalOnProperty` prevents job from running when disabled

---

## Files Created/Modified

| File | Action | Verified |
|------|--------|----------|
| `TaskRepository.java` | ADD method | [ ] |
| `RoutineRepository.java` | ADD method | [ ] |
| `RoutineSchedulerService.java` | CREATE | [ ] |
| `RoutineSchedulerJob.java` | CREATE | [ ] |
| `application.yml` | ADD config | [ ] |
| `RoutineSchedulerServiceTest.java` | CREATE | [ ] |
| `RoutineSchedulerIntegrationTest.java` | CREATE | [ ] |

---

## Final Verification

```bash
# Full build and test
cd services/backend && ./gradlew clean build

# Expected: BUILD SUCCESSFUL
```

- [ ] All tests pass
- [ ] No pre-existing test regressions
- [ ] Ready for PR review
