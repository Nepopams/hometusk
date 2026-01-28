# Sprint S10 — Routine Foundation (Entity + Parser + Security)

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Scope Anchor: `docs/planning/initiatives/INIT-2026Q3-recurring-tasks-scheduling.md`
- Epic: `docs/planning/epics/EP-010/epic.md`
- PI Charter: `docs/planning/pi/2026Q1-PI01/pi.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/routines.openapi.yaml`
- ADR-013: `docs/adr/013-routine-scheduler-design.md`

---

## Goal

Establish the Routine entity foundation with CRUD endpoints, recurrence rule parsing, and security boundaries to enable scheduler work in S11.

**Success Metric:** Routine CRUD works end-to-end with household boundary enforcement; recurrence rules parse correctly for all supported patterns (DAILY, WEEKLY, MONTHLY, EVERY_N_DAYS).

**Product Goal Alignment:** Reduces cognitive load by preparing infrastructure for automated recurring task creation (Pillar: Fairness & Transparency).

---

## Prioritization Rationale

This sprint implements the **foundation layer** of EP-010 (Recurring Tasks & Scheduling v0):

1. **Routine Entity is foundational:** All subsequent stories (scheduler, UI) depend on entity + CRUD
2. **Parser enables scheduler:** S11's RoutineSchedulerService requires working recurrence rule parsing
3. **Security early:** Catching boundary issues before scheduler runs is cheaper than fixing after

**Why S10 (not earlier):**
- EP-009 (Gamification) completing in S09
- Roadmap marks recurring tasks as NEXT after gamification
- Contract (`routines.openapi.yaml`) and ADR-013 already approved

---

## Scope

### Committed (DoR-ready, P1)

| Story | Title | Points | Dependencies | Flags |
|-------|-------|--------|--------------|-------|
| ST-1001 | Routine Entity + CRUD Endpoints | 5 | None | contract_impact, security_sensitive |
| ST-1002 | Recurrence Rule Parser | 3 | ST-1001 | - |
| ST-1008 | Security Boundaries + Integration Tests | 3 | ST-1001 | security_sensitive |

**Total committed:** 11 points

**Deliverables:**
- `Routine` JPA entity with DB migration
- `Task` entity extended with `routineId`, `scheduledDate` fields
- Partial unique index for dedup: `UNIQUE(routine_id, scheduled_date)`
- REST CRUD endpoints: list, create, get, update (PATCH), delete (soft)
- `RecurrenceRuleParser` service with all 4 patterns
- Household boundary enforcement on all endpoints
- Integration tests for security scenarios

---

### Out of Scope (explicit)

| Item | Reason | Deferred To |
|------|--------|-------------|
| RoutineSchedulerService | Depends on parser; separate concern | S11 |
| Assignment policies (round-robin) | Depends on scheduler | S11 |
| Pause/resume endpoints | Lifecycle; depends on scheduler | S12 |
| Routines UI page | Backend-first; UI in later sprint | S12 |
| Task card "from routine" indicator | UI concern | S12 |
| Upcoming instances preview | Depends on scheduler | S11/S12 |
| Complex RRULE (exceptions, BYSETPOS) | Out of epic scope | LATER |
| Per-user timezone | MVP uses household timezone | LATER |

---

## Capacity Note

**Assumptions:**
- 1 developer (Codex)
- 2-3 days per story (based on S04 velocity)
- Total: ~8-12 days for committed scope

**Constraints:**
- ST-1002 and ST-1008 depend on ST-1001
- ST-1002 and ST-1008 can run in parallel after ST-1001

**Buffer:** 20% (~2 points) for:
- Migration edge cases
- Test fixture setup
- Review feedback rework

---

## Assumptions

1. EP-009 (Gamification) completes in S09 (no resource conflict)
2. Existing Task entity can be extended (no breaking changes)
3. ADR-013 scheduler design is final (DB lock approach)
4. `routines.openapi.yaml` contract is approved
5. Postgres partial unique index syntax is acceptable

---

## Dependencies

| Dependency | Owner | Status | Risk |
|------------|-------|--------|------|
| EP-009 completion | Backend | In Progress (S09) | LOW |
| Task entity | Backend | Exists | LOW |
| Household/membership | Backend | Exists | LOW |
| ADR-013 (Scheduler Design) | Arch | Accepted | LOW |
| OpenAPI contract | Arch | Approved | LOW |

**Critical path:** ST-1001 -> (ST-1002 || ST-1008)

---

## Risks & Mitigations (ROAM-lite)

| Risk | Impact | Probability | Strategy | Notes |
|------|--------|-------------|----------|-------|
| Task entity migration breaks existing tests | HIGH | LOW | Mitigate | Nullable fields + CHECK constraint; run full test suite |
| Partial unique index not supported | MEDIUM | LOW | Resolve | Postgres-specific; verified in ADR-013 |
| RecurrenceRule JSON serialization issues | MEDIUM | MEDIUM | Mitigate | Use Jackson polymorphic with discriminator; unit test all patterns |
| S09 spillover delays S10 start | LOW | MEDIUM | Accept | Stories can start once ST-901 is merged |
| Scope creep to scheduler | MEDIUM | LOW | Own | Strict boundary; scheduler is S11 |

---

## Definition of Ready Check

**DoR Status:** READY (all 3 stories pass)

All prerequisites:
- [x] Epic EP-010 approved
- [x] Initiative INIT-2026Q3-recurring-tasks-scheduling approved
- [x] OpenAPI contract exists (`routines.openapi.yaml`)
- [x] ADR-013 accepted (scheduler design decisions)
- [x] All stories have AC with Given/When/Then
- [x] Test strategies defined for each story
- [x] Dependencies are clear and achievable

---

## Gate B Ask

**Request:** Approve Sprint S10 goal, committed scope (ST-1001, ST-1002, ST-1008), and capacity note.

**What we commit to:**
1. Deliver Routine entity with full CRUD
2. Deliver recurrence rule parser for DAILY/WEEKLY/MONTHLY/EVERY_N_DAYS
3. Deliver security boundary tests (403 on cross-household access)
4. Extend Task entity with routine linkage fields
5. DB migration with proper constraints

**What we won't do:**
- Scheduler service (S11)
- Assignment policies (S11)
- Pause/resume (S12)
- UI (S12)
- Backend changes beyond committed scope

**Approval needed:**
- [ ] Sprint goal approved
- [ ] Committed scope (11 points) approved
- [ ] Out of scope explicit list approved
- [ ] Risks accepted

---

## Sprint Artifacts

| Artifact | Path |
|----------|------|
| Sprint Plan | `docs/planning/pi/2026Q1-PI01/sprints/S10/sprint.md` (this file) |
| Scope Detail | `docs/planning/pi/2026Q1-PI01/sprints/S10/scope.md` |
| Demo Plan | `docs/planning/pi/2026Q1-PI01/sprints/S10/demo.md` |
| Retro | `docs/planning/pi/2026Q1-PI01/sprints/S10/retro.md` |

---

## Exit Criteria

Sprint is **done** when:
1. All committed stories completed (AC verified, DoD met)
2. `./gradlew build` passes
3. All unit and integration tests pass
4. OpenAPI contract matches implementation
5. Migration runs successfully
6. No cross-household data leaks (403 tests pass)
7. Demo prepared

**Sprint fails if:**
- Cannot create/read/update/delete routines
- Recurrence parser fails on any supported pattern
- Cross-household data leak detected
- Task entity extension breaks existing functionality
