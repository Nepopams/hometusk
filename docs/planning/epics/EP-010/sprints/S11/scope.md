# Sprint S11 Scope: Scheduler Engine & Assignment Policies

## Sources of Truth
- Sprint: `docs/planning/epics/EP-010/sprints/S11/sprint.md`
- Epic: `docs/planning/epics/EP-010/epic.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## In Scope

### ST-1003: RoutineSchedulerService + Idempotent Generation (5 pts)
**Readiness:** Ready

**DoR Checklist:**
- [x] Title: Clear, user-centric
- [x] Description: Context, value, expected behavior defined
- [x] Acceptance Criteria: 10 ACs covering happy path, edge cases, errors
- [x] Test Strategy: Unit + integration tests identified
- [x] Dependencies: ST-1002 completed in S10

**Key deliverables:**
- `RoutineSchedulerService` with generation logic
- Scheduled job (Spring `@Scheduled`)
- Idempotency via DB unique constraint (routineId, scheduledDate)
- 7-day generation window (configurable)
- PAUSED/DELETED routines skipped
- Error handling: continue on single routine failure

**Flags:**
- contract_impact: no (internal scheduler)
- adr_needed: lite (idempotency in epic)
- security_sensitive: no

---

### ST-1004: Assignment Policies (Fixed/Round-Robin/Manual) (3 pts)
**Readiness:** Ready (blocked by ST-1003 within sprint)

**DoR Checklist:**
- [x] Title: Clear, user-centric
- [x] Description: Context, value, expected behavior defined
- [x] Acceptance Criteria: 9 ACs covering all policies + concurrency
- [x] Test Strategy: Unit + integration tests identified
- [x] Dependencies: ST-1003 (within sprint)

**Key deliverables:**
- `AssignmentPolicyService` with strategy pattern
- FIXED policy: assign to `routine.fixedAssigneeId`
- ROUND_ROBIN policy: rotate through household members
- Round-robin state persistence (atomically with task creation)
- MANUAL policy: leave assignee null
- Concurrency safety via DB lock

**Flags:**
- contract_impact: no (internal logic)
- adr_needed: lite (concurrency in epic)
- security_sensitive: no

---

### ST-1007: Task Card "From Routine" Indicator (2 pts)
**Readiness:** Ready

**DoR Checklist:**
- [x] Title: Clear, user-centric
- [x] Description: Context, value, expected behavior defined
- [x] Acceptance Criteria: 7 ACs covering API, UI, edge cases
- [x] Test Strategy: Unit + integration + UI tests identified
- [x] Dependencies: ST-1001 completed in S10

**Key deliverables:**
- Task API extended with `routine` summary object
- Task card badge: "From routine" with icon
- Badge clickable -> navigate to routine detail
- Task detail page routine section
- Handle deleted routine case

**Contract impact:**
```yaml
Task:
  properties:
    routine:
      $ref: '#/components/schemas/RoutineSummary'
      nullable: true

RoutineSummary:
  type: object
  properties:
    id: { type: string, format: uuid }
    title: { type: string }
    status: { type: string, enum: [ACTIVE, PAUSED, DELETED] }
```

**Flags:**
- contract_impact: yes (Task schema extended)
- adr_needed: no
- security_sensitive: no

---

## Out of Scope

### Deferred to S12
| Story | Reason |
|-------|--------|
| ST-1005 (Routines Page) | UI sprint; foundation needed first |
| ST-1006 (Pause/Resume + Upcoming) | Requires ST-1005 UI foundation |

### Explicitly excluded (v0 decisions)
| Item | Reason |
|------|--------|
| Backfill for missed dates | v0 policy: no backfill (confusing UX) |
| Complex RRULE patterns | RRULE-lite only for v0 |
| Availability-based assignment | Requires calendar integration |
| Per-user timezone | MVP: household timezone only |
| Push notifications for generated tasks | Uses existing in-app notification flow |

---

## Readiness Summary

| Story | DoR Status | Blockers | Notes |
|-------|------------|----------|-------|
| ST-1003 | Ready | None | ST-1002 dependency resolved in S10 |
| ST-1004 | Ready | ST-1003 (in-sprint) | Can start after ST-1003 scheduler skeleton |
| ST-1007 | Ready | None | ST-1001 dependency resolved in S10; parallelizable |

**All stories pass DoR.** No discovery stories required.

---

## Work Sequence

### Recommended order
1. **ST-1003** (days 1-3): Implement scheduler service, idempotent generation
2. **ST-1007** (days 2-3): Parallel work on API extension + UI badge (no scheduler dependency)
3. **ST-1004** (days 3-4): Add assignment policies to scheduler

### Parallelization opportunity
ST-1007 only requires Task.routineId field from ST-1001 (done). It can proceed independently from ST-1003/ST-1004 scheduler work.

---

## Contract Changes

### Modified contracts
| Contract | Change | Story |
|----------|--------|-------|
| Task schema | Add `routine: RoutineSummary` field | ST-1007 |

### New contracts
None.

---

## Test Coverage Requirements

### ST-1003
- Unit: `RoutineSchedulerService` (4+ test cases)
- Integration: Scheduler DB tests (4+ scenarios)
- Manual: Verify scheduled job execution

### ST-1004
- Unit: `AssignmentPolicyService` (5+ test cases per policy)
- Integration: Round-robin persistence + concurrency (3+ scenarios)

### ST-1007
- Unit: TaskMapper routine summary
- Integration: Task API response includes routine
- UI: Badge renders, navigates correctly

---

## DoD Checklist (Sprint-level)

At sprint end, all stories must satisfy:
- [ ] Code follows project conventions
- [ ] Spotless formatting applied
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing
- [ ] API contract updated (ST-1007)
- [ ] PR reviewed and approved
- [ ] No cross-household data leaks
- [ ] Scheduler idempotent (verified by tests)
- [ ] Demo-ready increment
