# Epic: EP-010 — Recurring Tasks & Scheduling v0

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q3-recurring-tasks-scheduling.md`
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- Existing Task entity: `services/backend/src/main/java/com/hometusk/tasks/domain/Task.java`

---

## Status
**Draft** — Awaiting Human Gate approval

## Initiative Alignment
This epic implements INIT-2026Q3-recurring-tasks-scheduling:
- Routine entity with recurrence rules
- Scheduler for automatic task instance generation
- Assignment policies (fixed/round-robin/manual)
- Pause/resume lifecycle
- Web UI for routine management

**Product Goal Pillar:** Fairness & Transparency (automated fair distribution reduces cognitive load)

---

## Epic Goal
Enable household members to:
1. **Create routines** — define recurring tasks with rules (daily/weekly/monthly/every-N)
2. **Auto-generate task instances** — scheduler creates tasks ahead of time (7-day window)
3. **Distribute fairly** — round-robin assignment rotates responsibility
4. **Control lifecycle** — pause/resume routines without losing history
5. **See relationship** — tasks show "from routine X" indicator

**Core principle:** Reduce mental load by automating repetitive planning, NOT forcing rigid schedules.

---

## Outcome (User Value)
> "Создал рутину 'Мыть посуду каждый день' с round-robin — и теперь не думаю кто сегодня, система сама распределяет. Если нужна пауза на отпуск — ставлю pause. Каждая задача показывает что она от рутины."

---

## Non-Goals (Explicit)

| Item | Reason |
|------|--------|
| Complex RRULE (BYSETPOS, exceptions) | RRULE-lite only for v0 |
| Task dependencies (A blocks B) | Separate initiative |
| Sub-tasks/checklists in templates | Out of scope |
| Auto-assign based on availability/calendar | Requires calendar integration |
| External calendar sync (Google/Apple) | Separate initiative |
| Bulk import routines | Manual CRUD only for v0 |
| Per-user timezone | MVP: household timezone |

---

## Key Decisions (ADR-LITE)

### A) Recurrence Rule Format
**Decision:** RRULE-lite with enum patterns

| Pattern | Example | Storage |
|---------|---------|---------|
| DAILY | Every day | `{ "type": "DAILY" }` |
| WEEKLY | Every Saturday | `{ "type": "WEEKLY", "daysOfWeek": ["SATURDAY"] }` |
| MONTHLY | 1st of month | `{ "type": "MONTHLY", "dayOfMonth": 1 }` |
| EVERY_N_DAYS | Every 3 days | `{ "type": "EVERY_N_DAYS", "interval": 3 }` |

Rationale: Simple JSON, no RFC 5545 parser complexity.

### B) Generation Window
**Decision:** 7 days ahead (configurable per household in future)

Rationale: Enough visibility without flooding task list.

### C) Assignment Policies
| Policy | Behavior |
|--------|----------|
| FIXED | Always same assignee |
| ROUND_ROBIN | Rotate among household members (fair distribution) |
| MANUAL | No auto-assign; assigned when created/edited |

**Round-robin state:** Stored per routine (last assigned member + rotation order).

### D) Scheduler Idempotency & Catch-up Policy
**Decision:** Dedup by (routineId, scheduledDate) + NO backfill

- Scheduler generates only for **[today .. today+window]** — never for past dates
- If scheduler was down for 2 days, missed dates are NOT backfilled (v0 simplicity)
- Only one task instance per routine per scheduled date
- If task already exists for (routine, date) -> skip silently

Rationale: Backfill creates UX confusion ("why 10 overdue tasks appeared?"). Users can manually create if needed. Aligns with ADR-012 idempotency patterns.

### D.1) Scheduler Concurrency
**Decision:** DB-based scheduler lock (single runner)

- Use `SELECT ... FOR UPDATE SKIP LOCKED` on a `scheduler_locks` table or routine row
- Only one scheduler instance processes routines at a time
- Round-robin state updated atomically with task insert (same transaction)
- If task insert skipped (duplicate), round-robin state NOT advanced

Rationale: Simpler than distributed locking; v0 runs single instance anyway.

### E) Delete Semantics
**Decision:** Soft delete routine; pending instances always remain (v0)

- `DELETE /routines/{id}` -> routine.status = DELETED
- Pending (not-started) task instances: remain in task list (user can delete manually)
- Completed instances: always kept for history
- No `deletePendingInstances` flag in v0 (keep simple)

Rationale: Simpler UX; users see remaining tasks and can act on them.

### F) Points Integration
**Decision:** Generated tasks earn points like any task (via EP-009)

- Routine template may have `basePoints` override (future)
- v0: standard 10 points + on-time bonus

---

## Data Model (Proposed)

### New Entities
```
Routine
- id: UUID
- householdId: UUID (FK)
- title: string
- description: string (nullable)
- zoneId: UUID (FK, nullable)
- recurrenceRule: JSON (RRULE-lite)
- assignmentPolicy: enum (FIXED, ROUND_ROBIN, MANUAL)
- fixedAssigneeId: UUID (nullable, for FIXED policy)
- roundRobinState: JSON { lastAssignedUserId, memberOrder[] }
- status: enum (ACTIVE, PAUSED, DELETED)
- generationWindowDays: int (default 7)
- createdBy: UUID (FK)
- createdAt: timestamp
- updatedAt: timestamp
- pausedAt: timestamp (nullable)

Task (extended)
- routineId: UUID (FK, nullable) -- NEW FIELD
- scheduledDate: date (nullable) -- NEW FIELD (for routine instances)
```

### Constraints
- **CHECK:** `(routine_id IS NULL) = (scheduled_date IS NULL)` — both set or both null
- **Partial unique index (Postgres):** `UNIQUE(routine_id, scheduled_date) WHERE routine_id IS NOT NULL`

This ensures:
1. Manual tasks have neither field set
2. Routine-generated tasks have both fields set
3. No duplicate tasks for same routine+date

---

## API Contract (Proposed)

### New Endpoints
```yaml
# Routines CRUD
GET    /api/v1/households/{householdId}/routines
POST   /api/v1/households/{householdId}/routines
GET    /api/v1/households/{householdId}/routines/{routineId}
PATCH  /api/v1/households/{householdId}/routines/{routineId}  # partial update
DELETE /api/v1/households/{householdId}/routines/{routineId}

# Lifecycle
POST   /api/v1/households/{householdId}/routines/{routineId}/pause
POST   /api/v1/households/{householdId}/routines/{routineId}/resume

# Upcoming instances (preview)
GET    /api/v1/households/{householdId}/routines/{routineId}/upcoming
```

### Task Extended
```yaml
Task:
  properties:
    routineId:
      type: string
      format: uuid
      nullable: true
      description: Source routine (if generated)
    scheduledDate:
      type: string
      format: date
      nullable: true
      description: Scheduled date for routine instance
```

---

## Stories

| ID | Title | Status | Priority | Points | Dependencies |
|----|-------|--------|----------|--------|--------------|
| ST-1001 | Routine Entity + CRUD Endpoints | Draft | P1 | 5 | — |
| ST-1002 | Recurrence Rule Parser | Draft | P1 | 3 | ST-1001 |
| ST-1003 | RoutineSchedulerService + Idempotent Generation | Draft | P1 | 5 | ST-1002 |
| ST-1004 | Assignment Policies (Fixed/Round-Robin/Manual) | Draft | P1 | 3 | ST-1003 |
| ST-1005 | Routines Page (List + Create/Edit Form) | Draft | P1 | 5 | ST-1001 |
| ST-1006 | Pause/Resume + Upcoming Instances View | Draft | P2 | 3 | ST-1003, ST-1005 |
| ST-1007 | Task Card "From Routine" Indicator | Draft | P2 | 2 | ST-1001 |
| ST-1008 | Security Boundaries + Integration Tests | Draft | P1 | 3 | ST-1001 |

**Total:** 29 points

### Sprint Allocation (Proposed)
- **Sprint S10:** ST-1001, ST-1002, ST-1008 (foundation + security) = 11 pts
- **Sprint S11:** ST-1003, ST-1004, ST-1007 (engine + policies) = 10 pts
- **Sprint S12:** ST-1005, ST-1006 (UI) = 8 pts

---

## Milestones

| Milestone | Stories | Exit Criteria |
|-----------|---------|---------------|
| M1: Backend foundation | ST-1001, ST-1002 | Routine CRUD works, rules parse correctly |
| M2: Auto-generation | ST-1003, ST-1004 | Scheduler creates tasks with correct assignment |
| M3: Security | ST-1008 | No cross-household leaks, 403 tests pass |
| M4: UI complete | ST-1005, ST-1006, ST-1007 | Full CRUD + lifecycle + indicator in UI |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Over-generation (too many tasks) | UX clutter | 7-day window + pause + soft delete |
| Timezone complexity | Wrong dates | MVP: household timezone only |
| Scheduler failures | Missed tasks | Idempotent catch-up + alerts |
| Round-robin state corruption | Unfair distribution | State in DB + pessimistic lock |
| UX complexity (rule builder) | User confusion | Start with presets, not custom RRULE |

---

## Success Metrics

| Metric | Target |
|--------|--------|
| Routine adoption | >= 30% active households create 1+ routine in 14 days |
| Routine retention | >= 50% routines still active after 30 days |
| Task reduction | >= 20% fewer manually created tasks (for routine users) |
| Scheduler reliability | >= 99.5% uptime (no missed generations) |

---

## Exit Criteria

1. User can create routine "Clean kitchen every Saturday"
2. Scheduler generates task instances 7 days ahead
3. Round-robin assignment rotates among 2+ members
4. Pause routine -> no new instances; resume -> generation continues
5. Delete routine -> pending instances optionally removed
6. Scheduler idempotent (restart = no duplicates)
7. Task shows "from routine" indicator in UI
8. Household boundary enforced (403 tests pass)
9. OpenAPI contract updated
10. Build passes, tests pass

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | yes | New endpoints: routines CRUD + lifecycle + Task.routineId |
| adr_needed | lite | Scheduler idempotency + round-robin policy (inline decisions) |
| diagrams_needed | no | Standard patterns, no new architecture |
| security_sensitive | yes | Household boundary enforcement |

---

## Related Artifacts

| Artifact | Path |
|----------|------|
| Initiative | `docs/planning/initiatives/INIT-2026Q3-recurring-tasks-scheduling.md` |
| Existing Task entity | `services/backend/src/main/java/com/hometusk/tasks/domain/Task.java` |
| Idempotency ADR | `docs/architecture/decisions/012-command-reliability-idempotency.md` |
