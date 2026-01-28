# ADR-013: Routine Scheduler Design (EP-010)

**Status:** Accepted
**Date:** 2026-01-28
**Epic:** EP-010 — Recurring Tasks & Scheduling v0

## Context

EP-010 introduces recurring tasks (routines) with automatic task instance generation. The scheduler must:
- Generate task instances for [today..today+window] based on recurrence rules
- Ensure no duplicate tasks for the same routine and scheduled date
- Handle scheduler restarts and failures gracefully
- Support round-robin assignment that rotates fairly among household members
- Run in a single-instance environment (v0) with future multi-instance consideration

### Constraints
- Postgres as primary database
- No external coordination service (Redis/Zookeeper) in v0
- Scheduler may be restarted at any time
- Users may create/pause/delete routines concurrently

### Options Considered

#### Dedup Strategy
| Option | Pros | Cons |
|--------|------|------|
| **A) Partial unique index (routineId, scheduledDate)** | DB-enforced guarantee, no race conditions | Postgres-specific syntax |
| B) Application-level dedup (SELECT before INSERT) | Portable | Race conditions without locking |

**Selected:** Option A — Postgres partial unique index provides strongest guarantee.

#### Catch-up Policy
| Option | Pros | Cons |
|--------|------|------|
| **A) No backfill (forward-only)** | Simple UX, predictable behavior | Missed days not auto-recovered |
| B) Backfill past dates on scheduler recovery | No missed tasks | Confusing UX ("why 10 overdue tasks appeared?") |

**Selected:** Option A — Backfill creates UX confusion. Users can manually create missed tasks if needed.

#### Concurrency Model
| Option | Pros | Cons |
|--------|------|------|
| **A) DB lock (SELECT FOR UPDATE SKIP LOCKED)** | No external deps, simple | Single runner limit |
| B) Distributed lock (Redis/Zookeeper) | Multi-instance support | Complexity, new dependency |

**Selected:** Option A — v0 runs single instance; DB lock is sufficient.

#### Round-Robin State Management
| Option | Pros | Cons |
|--------|------|------|
| **A) State per routine (lastAssignedUserId, memberOrder)** | Atomic with task insert, simple queries | State per routine |
| B) Separate assignment tracking table | Centralized tracking | Extra join, complex queries |

**Selected:** Option A — State stored directly on Routine entity, updated atomically with task insertion.

## Decision

### 1. Scheduler Idempotency

We will use a **partial unique index** on `(routine_id, scheduled_date)` to prevent duplicate task instances:

```sql
CREATE UNIQUE INDEX idx_task_routine_scheduled_date
ON tasks (routine_id, scheduled_date)
WHERE routine_id IS NOT NULL;
```

The scheduler generates tasks only for **[today .. today + generationWindowDays]**. Past dates are never backfilled.

### 2. Data Invariant

We will enforce that `routine_id` and `scheduled_date` are both set or both null:

```sql
ALTER TABLE tasks ADD CONSTRAINT chk_routine_date_consistency
CHECK ((routine_id IS NULL) = (scheduled_date IS NULL));
```

This ensures:
- Manual tasks have neither field set
- Routine-generated tasks have both fields set

### 3. Scheduler Concurrency

We will use **DB-based locking** with `SELECT ... FOR UPDATE SKIP LOCKED`:
- Scheduler acquires lock on `scheduler_locks` table row (or routine row)
- Only one scheduler instance processes routines at a time
- Lock released on transaction commit/rollback

### 4. Round-Robin State

We will store round-robin state on the Routine entity:
```json
{
  "lastAssignedUserId": "uuid-of-last-assigned",
  "memberOrder": ["uuid-1", "uuid-2", "uuid-3"]
}
```

State advances **only when task INSERT succeeds** (not on skip due to duplicate). This is achieved by:
1. Acquiring pessimistic lock on routine row
2. Calculating next assignee
3. Inserting task (may fail due to unique constraint)
4. If insert succeeds: update round-robin state in same transaction
5. If insert skipped (duplicate): rollback state change

## Consequences

### Positive
- **No duplicate tasks:** Partial unique index enforces at DB level
- **Predictable UX:** No surprise backfill of overdue tasks
- **Simple concurrency:** DB lock avoids distributed coordination complexity
- **Fair rotation:** Round-robin state advances only on successful generation

### Negative
- **Postgres-specific:** Partial unique index syntax is Postgres-specific
- **Single runner:** DB lock effectively limits to single scheduler instance in v0
- **Missed days not recovered:** If scheduler is down, those days are simply skipped

### Risks and Mitigations
| Risk | Mitigation |
|------|------------|
| Scheduler outage misses generations | Alert on scheduler failures; manual catch-up possible |
| Round-robin state corruption | Pessimistic lock on routine row prevents concurrent updates |
| Long-running transaction blocking | Keep transaction short (single routine per txn) |

### Follow-ups
- [ ] Add monitoring/alerting for scheduler health
- [ ] Consider TTL cleanup for old scheduler lock entries (if using separate table)
- [ ] Document manual procedure for catch-up after extended outage

## Related

- **Epic:** `docs/planning/epics/EP-010/epic.md`
- **Initiative:** `docs/planning/initiatives/INIT-2026Q3-recurring-tasks-scheduling.md`
- **Idempotency patterns:** `docs/architecture/decisions/012-command-reliability-idempotency.md` (ADR-012)
