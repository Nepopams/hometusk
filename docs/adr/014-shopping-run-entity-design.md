# ADR-014: ShoppingRun Entity Design

**Status:** Proposed
**Date:** 2026-02-03
**Epic:** EP-013 — Shopping Marketplaces
**Blocks:** ST-1301 (ShoppingRun entity + repository)

## Context

EP-013 introduces ShoppingRun — a "shopping trip" feature that captures a snapshot of items from a shopping list for a single store visit. Users can mark items as purchased within the run and track their shopping progress.

### Requirements (from contract and stories)

1. **Snapshot items** from a ShoppingList into a ShoppingRun
2. **Track progress** (purchased vs. remaining) within the run
3. **Close run** as COMPLETED or CANCELLED
4. **Sync back** to original ShoppingList when marking items purchased
5. **Household boundary** enforcement (IDOR prevention)
6. **Idempotency** for run creation (via Idempotency-Key header)

### Constraints

- Postgres as primary database
- Next available migration: V025
- Must integrate with existing ShoppingList/ShoppingItem entities (ADR-008)
- Contract already defines DTOs: ShoppingRunDto, ShoppingRunItemDto, ItemCounts

### Options Considered

#### Snapshot Strategy

| Option | Pros | Cons |
|--------|------|------|
| **A) Copy item data into ShoppingRunItem** | Immutable snapshot, no N+1 queries | Data duplication |
| B) Reference-only (FK to ShoppingItem) | No duplication | Original item may change, deleted items break run |

**Selected:** Option A — Copy item data (name, quantity, unit) at snapshot time, keep `originalItemId` for sync-back.

#### Sync-to-List Behavior

| Option | Pros | Cons |
|--------|------|------|
| **A) Sync by default (syncToList=true)** | Items marked in run also update list | May confuse if user expects run-only tracking |
| B) No sync by default | Run is isolated | User must manually update list after run |

**Selected:** Option A — Contract defines `syncToList: true` as default.

#### Status Transitions

| From | To | Allowed? |
|------|---|----------|
| ACTIVE | COMPLETED | Yes |
| ACTIVE | CANCELLED | Yes |
| COMPLETED | * | No (terminal) |
| CANCELLED | * | No (terminal) |

**Decision:** Terminal states are immutable. Attempting to close an already-closed run with a *different* status returns 409 Conflict. Same status is idempotent (returns 200).

#### Household Enforcement

| Strategy | Description |
|----------|-------------|
| **Repository-level** | All queries include `householdId` in WHERE clause |
| **Service-level assertion** | Validate `run.householdId == context.householdId` after fetch |

**Selected:** Both — repository methods are household-scoped by design; service layer performs additional assertion before mutations.

## Decision

### 1. Entity Structure

We will create two JPA entities:

```java
@Entity
@Table(name = "shopping_runs")
public class ShoppingRun {
    UUID id;
    UUID householdId;           // denormalized for efficient queries
    ShoppingList sourceList;    // FK to source list
    String listName;            // snapshot of list name at creation
    ShoppingRunStatus status;   // ACTIVE, COMPLETED, CANCELLED
    User createdBy;
    Instant createdAt;
    Instant closedAt;           // nullable until closed
    List<ShoppingRunItem> items;
}

@Entity
@Table(name = "shopping_run_items")
public class ShoppingRunItem {
    UUID id;
    ShoppingRun run;
    UUID originalItemId;        // FK to original ShoppingItem (for sync)
    String name;                // snapshot
    Integer quantity;           // snapshot
    String unit;                // snapshot
    boolean purchased;
    Instant purchasedAt;        // nullable until purchased
}
```

### 2. Database Migration (V025)

```sql
CREATE TABLE shopping_runs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id    UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    source_list_id  UUID NOT NULL REFERENCES shopping_lists(id) ON DELETE RESTRICT,
    list_name       VARCHAR(255) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),
    created_by_id   UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    closed_at       TIMESTAMP WITH TIME ZONE
);

CREATE TABLE shopping_run_items (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id            UUID NOT NULL REFERENCES shopping_runs(id) ON DELETE CASCADE,
    original_item_id  UUID REFERENCES shopping_items(id) ON DELETE SET NULL,
    name              VARCHAR(255) NOT NULL,
    quantity          INTEGER NOT NULL DEFAULT 1,
    unit              VARCHAR(50),
    purchased         BOOLEAN NOT NULL DEFAULT FALSE,
    purchased_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_shopping_runs_household_id ON shopping_runs(household_id);
CREATE INDEX idx_shopping_runs_source_list_id ON shopping_runs(source_list_id);
CREATE INDEX idx_shopping_run_items_run_id ON shopping_run_items(run_id);
CREATE INDEX idx_shopping_run_items_original_item_id ON shopping_run_items(original_item_id)
    WHERE original_item_id IS NOT NULL;
```

### 3. Idempotency

Run creation uses the existing Idempotency-Key mechanism (ADR-012):
- Key stored in `command_idempotency` table
- Same key + same payload returns cached response
- Same key + different payload returns 409

### 4. Sync Behavior

When `PATCH /shopping-runs/{runId}/items/{itemId}` is called with `purchased=true` and `syncToList=true`:
1. Update `ShoppingRunItem.purchased = true, purchasedAt = now()`
2. Lookup original `ShoppingItem` by `originalItemId`
3. If found and not already purchased: call `ShoppingItem.markPurchased()`
4. If original item deleted: log warning, continue (run item still updated)

### 5. Close Semantics

- `POST /shopping-runs/{runId}/close` with `status=COMPLETED` or `CANCELLED`
- If run already closed with same status: return 200 (idempotent)
- If run already closed with different status: return 409 (conflict)
- On close: set `closedAt = now()`, persist status

## Consequences

### Positive

- **Immutable snapshots**: Run items preserve original state at creation time
- **Progress tracking**: ItemCounts (total/purchased/remaining) computed from run items
- **Safe sync**: Original list updated when items purchased in run
- **Household boundary**: Enforced at repository and service levels

### Negative

- **Data duplication**: Item name/quantity/unit copied into run items
- **Orphaned references**: If original item deleted, `originalItemId` becomes dangling (but run still works)
- **Migration complexity**: Two new tables, indexes

### Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Large runs with many items | Pagination on item list if needed (v2) |
| Sync-back fails (original deleted) | Graceful degradation: log warning, update run item only |
| Concurrent item updates | Optimistic locking on ShoppingRunItem if contention observed |

## Related

- **Contract:** `docs/contracts/http/shopping-marketplaces.openapi.yaml`
- **Story:** `docs/planning/epics/EP-013/stories/ST-1301-shopping-run-entity.md`
- **Task-Shopping linkage:** `docs/architecture/decisions/008-stage5-task-shopping-linkage.md`
- **Idempotency:** `docs/architecture/decisions/012-command-reliability-idempotency.md`
