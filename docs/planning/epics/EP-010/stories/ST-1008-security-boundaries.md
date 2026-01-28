# Story: ST-1008 — Security Boundaries + Integration Tests

## Sources of Truth
- Epic: `docs/planning/epics/EP-010/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q3-recurring-tasks-scheduling.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Draft** — Awaiting Human Gate approval

## User Value
> "Мои рутины и задачи видны только членам моего домохозяйства. Никто извне не получит доступ."

---

## Description
Implement and verify security boundaries for routines feature:
- Household membership check on all routine endpoints
- No cross-household data leaks
- Integration tests for security scenarios
- Scheduler respects household isolation

---

## In Scope
- Membership enforcement on all routine endpoints
- 403 response for non-members
- Integration tests for boundary scenarios
- Negative tests (cross-household access attempts)
- Scheduler isolation (only processes own household routines)

## Out of Scope
- Role-based permissions (admin vs member)
- Routine-level sharing
- Audit logging (separate concern)

---

## Acceptance Criteria

### AC-1: List routines - membership required
```
Given user NOT member of householdId
When GET /households/{householdId}/routines
Then response 403 Forbidden
```

### AC-2: Create routine - membership required
```
Given user NOT member of householdId
When POST /households/{householdId}/routines
Then response 403 Forbidden
And no routine created
```

### AC-3: Get routine - membership required
```
Given routine in household A
And user is member of household B (not A)
When GET /households/A/routines/{routineId}
Then response 403 Forbidden
```

### AC-4: Update routine - membership required
```
Given user NOT member of householdId
When PUT /households/{householdId}/routines/{id}
Then response 403 Forbidden
```

### AC-5: Delete routine - membership required
```
Given user NOT member of householdId
When DELETE /households/{householdId}/routines/{id}
Then response 403 Forbidden
```

### AC-6: Pause/resume - membership required
```
Given user NOT member of householdId
When POST /households/{householdId}/routines/{id}/pause
Then response 403 Forbidden
```

### AC-7: Upcoming - membership required
```
Given user NOT member of householdId
When GET /households/{householdId}/routines/{id}/upcoming
Then response 403 Forbidden
```

### AC-8: No cross-household routine listing
```
Given household A with routines [R1, R2]
And household B with routines [R3]
And user member of B only
When GET /households/B/routines
Then only R3 returned (not R1, R2)
```

### AC-9: Routine ID in wrong household
```
Given routine R1 belongs to household A
And user is member of household B
When GET /households/B/routines/R1 (R1 ID but wrong household)
Then response 404 Not Found (not 403, to not leak existence)
```

### AC-10: Scheduler isolation
```
Given two households A and B each with routines
When scheduler runs
Then tasks created only for own household
And no tasks in wrong household
```

### AC-11: Generated tasks inherit household boundary
```
Given routine in household A generates task
Then task.householdId = A
And task follows existing task security (from MVP)
```

---

## Test Strategy

### Integration Tests (Primary)
- `RoutineSecurityIntegrationTest`:
  - `listRoutines_notMember_returns403`
  - `createRoutine_notMember_returns403`
  - `getRoutine_wrongHousehold_returns404`
  - `updateRoutine_notMember_returns403`
  - `deleteRoutine_notMember_returns403`
  - `pauseRoutine_notMember_returns403`
  - `upcoming_notMember_returns403`
  - `listRoutines_crossHousehold_noLeaks`

### Scheduler Tests
- `SchedulerSecurityTest`:
  - `scheduler_onlyGeneratesForOwnHousehold`
  - `generatedTask_hasCorrectHouseholdId`

---

## Points
**3 points**

## Dependencies
- ST-1001 (Routine endpoints exist)

## Flags
- contract_impact: no
- adr_needed: no
- diagrams_needed: no
- security_sensitive: yes (this IS the security story)
