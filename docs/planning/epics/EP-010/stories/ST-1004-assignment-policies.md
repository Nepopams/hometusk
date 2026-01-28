# Story: ST-1004 — Assignment Policies (Fixed/Round-Robin/Manual)

## Sources of Truth
- Epic: `docs/planning/epics/EP-010/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q3-recurring-tasks-scheduling.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Draft** — Awaiting Human Gate approval

## User Value
> "Выбрал 'round-robin' — и система справедливо распределяет задачи между всеми членами семьи по очереди."

---

## Description
Implement assignment policies that scheduler uses when creating task instances:
- FIXED: always assign to specified user
- ROUND_ROBIN: rotate among household members
- MANUAL: no assignee (assigned manually later)

---

## In Scope
- `AssignmentPolicyService` with strategy pattern
- FIXED policy: assign to `routine.fixedAssigneeId`
- ROUND_ROBIN policy: rotate through household members
- Round-robin state persistence (who was last assigned)
- MANUAL policy: leave assignee null
- Integration with `RoutineSchedulerService`

## Out of Scope
- Availability-based assignment (calendar)
- Skill/preference-based assignment
- Admin override of assignment
- UI for policy selection (ST-1005)

---

## Acceptance Criteria

### AC-1: FIXED policy assigns to specified user
```
Given routine with:
  - assignmentPolicy = FIXED
  - fixedAssigneeId = user-A
When task instance generated
Then task.assignee = user-A
```

### AC-2: FIXED policy - user must be household member
```
Given routine with fixedAssigneeId = user-X
And user-X is NOT member of household
When validating routine
Then error "Fixed assignee must be household member"
```

### AC-3: ROUND_ROBIN rotates through members
```
Given household with members [A, B, C] (in join order)
And routine with assignmentPolicy = ROUND_ROBIN
And roundRobinState = null (first run)
When 3 task instances generated
Then:
  - task 1 assigned to A
  - task 2 assigned to B
  - task 3 assigned to C
And routine.roundRobinState.lastAssignedUserId = C
```

### AC-4: ROUND_ROBIN continues rotation across runs
```
Given routine.roundRobinState.lastAssignedUserId = B
And household members = [A, B, C]
When next task generated
Then assigned to C (next after B)
```

### AC-5: ROUND_ROBIN wraps around
```
Given routine.roundRobinState.lastAssignedUserId = C
And household members = [A, B, C]
When next task generated
Then assigned to A (wrap to beginning)
```

### AC-6: ROUND_ROBIN handles member changes
```
Given routine.roundRobinState.memberOrder = [A, B, C]
And member B leaves household
When rotation state refreshed
Then memberOrder = [A, C]
And rotation continues with remaining members
```

### AC-7: MANUAL policy leaves no assignee
```
Given routine with assignmentPolicy = MANUAL
When task instance generated
Then task.assignee = null
```

### AC-8: State persisted atomically with task creation
```
Given round-robin routine
When task created
Then roundRobinState updated in same transaction
```

### AC-9: Concurrent scheduler runs safe
```
Given two scheduler instances running
When both try to generate for same routine
Then no duplicate assignments (pessimistic lock or optimistic retry)
```

---

## Test Strategy

### Unit Tests
- `AssignmentPolicyService`:
  - `fixed_assignsToConfiguredUser`
  - `manual_returnsNull`
  - `roundRobin_rotatesCorrectly`
  - `roundRobin_wrapsAround`
  - `roundRobin_handlesMemberRemoval`

### Integration Tests
- `AssignmentPolicyIntegrationTest`:
  - `roundRobin_statePersistedCorrectly`
  - `roundRobin_concurrentAccess_noConflict`
  - `fixed_nonMember_validationFails`

---

## Points
**3 points**

## Dependencies
- ST-1001 (Routine entity with policy fields)
- ST-1003 (SchedulerService to integrate with)

## Flags
- contract_impact: no (internal logic)
- adr_needed: lite (round-robin concurrency in epic)
- diagrams_needed: no
- security_sensitive: no
