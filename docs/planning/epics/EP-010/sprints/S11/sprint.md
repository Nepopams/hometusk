# Sprint S11: EP-010 Recurring Tasks — Scheduler Engine & Assignment Policies

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Epic: `docs/planning/epics/EP-010/epic.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- Scheduler ADR: `docs/adr/013-routine-scheduler-design.md`
- Idempotency ADR: `docs/architecture/decisions/012-command-reliability-idempotency.md`

---

## Status
**Draft** — Awaiting Human Gate B approval

---

## Sprint Goal
> Scheduler automatically generates task instances from routines with correct round-robin assignment; tasks show their routine origin.

**Value delivered:** Users who created routines in S10 now see tasks automatically appearing in their task list, fairly distributed among household members. The relationship between generated tasks and their source routine is visible in the UI.

**Pillar alignment:** Fairness & Transparency (automated fair distribution reduces cognitive load and makes contribution visible)

---

## Committed Scope

| Story ID | Title | Points | Readiness | Dependencies |
|----------|-------|--------|-----------|--------------|
| ST-1003 | RoutineSchedulerService + Idempotent Generation | 5 | Ready | ST-1002 (done in S10) |
| ST-1004 | Assignment Policies (Fixed/Round-Robin/Manual) | 3 | Ready | ST-1003 (within sprint) |
| ST-1007 | Task Card "From Routine" Indicator | 2 | Ready | ST-1001 (done in S10) |

**Total committed:** 10 points

---

## Sprint Sequence & Dependencies

```
S10 (completed)
  ST-1001 Routine Entity + CRUD ----------+
  ST-1002 Recurrence Rule Parser ---------+----> ST-1003 (S11)
  ST-1008 Security Boundaries            |              |
                                         |              v
                                         +----> ST-1007  ST-1004
                                                 (S11)    (S11)
```

**Critical path:** ST-1003 -> ST-1004 (in-sprint dependency)

ST-1007 can be developed in parallel with ST-1003/ST-1004 as it only requires the Task.routineId field from ST-1001.

---

## Stretch Scope
None. Sprint is at 10 points, which aligns with epic velocity estimate.

---

## Out of Scope (Explicit)

| Item | Reason |
|------|--------|
| ST-1005 (Routines Page UI) | Planned for S12 |
| ST-1006 (Pause/Resume + Upcoming View) | Planned for S12; requires UI foundation |
| Backfill for missed scheduler runs | Explicit v0 policy: no backfill (epic decision D) |
| Complex RRULE (BYSETPOS, exceptions) | Out of scope for v0 (epic non-goal) |
| Availability-based assignment | Requires calendar integration (out of scope) |
| Push notifications for generated tasks | Uses existing EP-007 in-app flow |

---

## Dependencies

### Resolved (from S10)
- [x] ST-1001: Routine entity + CRUD endpoints (provides data model)
- [x] ST-1002: RecurrenceRuleParser (provides rule evaluation)
- [x] ST-1008: Security boundaries (provides household scoping patterns)

### External Dependencies
| Dependency | Owner | Status | Impact if blocked |
|------------|-------|--------|-------------------|
| Task entity extension (routineId, scheduledDate) | S10/ST-1001 | Done | Unblocked |
| DB unique constraint (routine_id, scheduled_date) | S10/ST-1001 | Done | Unblocked |
| Household member list API | Existing | Available | None |

---

## Risks (ROAM-lite)

| Risk | Likelihood | Impact | Strategy | Owner |
|------|------------|--------|----------|-------|
| Round-robin state corruption under concurrent scheduler | Low | High | **Mitigate:** DB-level lock (SELECT FOR UPDATE SKIP LOCKED); single scheduler instance for v0 | Dev |
| Scheduler generates too many tasks (clutter) | Medium | Medium | **Accept:** 7-day window is deliberate limit; users can pause routines | Product |
| Task card UI breaks existing layout | Low | Low | **Accept:** Minor UI change; standard component patterns | Dev |
| Integration complexity with existing task flows | Low | Medium | **Mitigate:** Generated tasks use existing TaskService; no special paths | Dev |

---

## Capacity & Buffer

**Assumed velocity:** 10-12 points/sprint (based on S10 completion of 11 points)

**Committed:** 10 points
**Buffer:** ~2 points (stretch capacity if needed)

**Assumptions:**
- 1 developer (full-time equivalent)
- No major blockers from platform/infra
- S10 foundation is stable (no rework)

---

## Demo Plan

**Increment:** Working scheduler that creates tasks from routines with fair assignment + visible routine indicator.

### Demo script
1. Show existing routine "Clean kitchen daily" (created in S10)
2. Trigger scheduler manually (or show automatic run)
3. Show task list: 7 new tasks appeared with correct dates
4. Highlight round-robin: tasks assigned A, B, C, A, B, C, A
5. Show task card with "From routine: Clean kitchen" badge
6. Click badge -> navigate to routine detail
7. Show task detail page with routine section

### Success criteria
- Tasks generated for next 7 days
- Assignment rotates correctly among household members
- Routine indicator visible on task card and detail
- No duplicate tasks on scheduler re-run

---

## Gate Ask

**Human Gate B:** Approve sprint goal and committed scope.

**Decision requested:**
1. Approve Sprint Goal: "Scheduler automatically generates task instances from routines with correct round-robin assignment; tasks show their routine origin."
2. Approve committed scope: ST-1003 (5pts), ST-1004 (3pts), ST-1007 (2pts) = 10 points total
3. Acknowledge out-of-scope items (UI deferred to S12)
4. Acknowledge risks and mitigations

---

## Appendix: Story Links

- ST-1003: `docs/planning/epics/EP-010/stories/ST-1003-scheduler-service.md`
- ST-1004: `docs/planning/epics/EP-010/stories/ST-1004-assignment-policies.md`
- ST-1007: `docs/planning/epics/EP-010/stories/ST-1007-task-routine-indicator.md`
