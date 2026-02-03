# Story: ST-1301 — ShoppingRun Entity + Repository

## Status: READY
**Sprint:** S16 | **Points:** 5
**Artifacts:** ADR-014, Contract approved 2026-02-03

## Description
Create the ShoppingRun domain entity and JPA repository to support "shopping trip" functionality. A ShoppingRun captures a snapshot of items to purchase during a single shopping trip, tracking which items were purchased vs. skipped.

**User Value**: Foundation for tracking shopping trips with clear before/after state.

## In Scope
- `ShoppingRun` entity with fields: id, householdId, shoppingListId, status, createdAt, closedAt, createdBy
- `ShoppingRunItem` entity for item snapshot: id, runId, shoppingItemId, name, quantity, unit, purchased, purchasedAt
- `ShoppingRunStatus` enum: ACTIVE, COMPLETED, CANCELLED
- `ShoppingRunRepository` with household-scoped queries
- Database migration (V015 or next available)
- Unit tests for entity methods

## Out of Scope
- REST endpoints (ST-1302)
- Web UI (ST-1307, ST-1308)
- Multi-store runs
- Run templates/presets

## Acceptance Criteria

### AC-1: Entity Creation
```
Given a household with an active shopping list
When a new ShoppingRun is created
Then it has status=ACTIVE, createdAt=now, closedAt=null
And it belongs to the correct household
```

### AC-2: Item Snapshot
```
Given a ShoppingRun is created
When items are snapshotted from the list
Then ShoppingRunItem records are created with current item data
And each snapshot references the original ShoppingItem.id
```

### AC-3: Run Completion
```
Given an ACTIVE ShoppingRun
When the run is closed as COMPLETED
Then status=COMPLETED, closedAt=now
And purchased counts are available
```

### AC-4: Household Boundary
```
Given a ShoppingRun in household A
When queried from household B
Then the run is not returned (IDOR prevention)
```

### AC-5: Migration Idempotency
```
Given the migration runs twice
When executed
Then no errors occur (IF NOT EXISTS guards)
```

## Test Strategy

**Unit Tests**:
- ShoppingRun entity state transitions
- ShoppingRunItem snapshot creation
- Status enum coverage

**Integration Tests**:
- Repository CRUD operations
- Household boundary enforcement
- Migration verification

**Test Data**:
- Test household with existing shopping list + items
- Multiple runs per list scenario

## Flags
- contract_impact: yes (entity schema affects API DTOs)
- adr_needed: lite (ADR-014 for entity design decisions)
- security_sensitive: no
- diagrams_needed: no

## Dependencies
- ADR-014: ShoppingRun entity design (BLOCKER)
- Contract: ShoppingRun DTOs (BLOCKER)

## Points: 5
