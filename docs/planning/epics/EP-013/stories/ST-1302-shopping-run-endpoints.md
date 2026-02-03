# Story: ST-1302 — ShoppingRun REST Endpoints

## Status: NOT READY
**Blocker**: Blocked by ST-1301 + needs contract approval

## Description
Implement REST endpoints for ShoppingRun lifecycle: create a run from a shopping list, list runs, close a run, and update item status within a run.

**User Value**: API foundation for shopping trip management.

## In Scope
- `POST /api/v1/households/{householdId}/shopping-runs` — Create run from list
- `GET /api/v1/households/{householdId}/shopping-runs` — List runs (with status filter)
- `GET /api/v1/households/{householdId}/shopping-runs/{runId}` — Get run details with items
- `POST /api/v1/households/{householdId}/shopping-runs/{runId}/close` — Close run (COMPLETED/CANCELLED)
- `PATCH /api/v1/households/{householdId}/shopping-runs/{runId}/items/{itemId}` — Mark item purchased/skipped
- DTO classes: CreateShoppingRunRequest, ShoppingRunDto, ShoppingRunItemDto, CloseShoppingRunRequest
- ShoppingRunService with business logic
- OpenAPI annotations for Swagger docs

## Out of Scope
- Web UI (ST-1307, ST-1308)
- Run editing (add/remove items mid-run)
- Run sharing between households

## Acceptance Criteria

### AC-1: Create Run
```
Given user is member of household
And household has a shopping list with 3 unpurchased items
When POST /shopping-runs with listId
Then 201 Created with run having 3 snapshotted items
And run status = ACTIVE
```

### AC-2: List Runs
```
Given household has 2 ACTIVE and 1 COMPLETED runs
When GET /shopping-runs?status=ACTIVE
Then only 2 ACTIVE runs returned
And each includes item counts (total, purchased)
```

### AC-3: Get Run Details
```
Given an ACTIVE run with 5 items (2 purchased)
When GET /shopping-runs/{runId}
Then response includes all 5 items with purchase status
```

### AC-4: Close Run
```
Given an ACTIVE run
When POST /shopping-runs/{runId}/close with status=COMPLETED
Then run status = COMPLETED, closedAt = now
And response includes purchase summary
```

### AC-5: Mark Item Purchased
```
Given an ACTIVE run with unpurchased item
When PATCH /shopping-runs/{runId}/items/{itemId} with purchased=true
Then item marked purchased with purchasedAt timestamp
And original ShoppingItem also marked purchased (optional sync)
```

### AC-6: Household Boundary
```
Given a run in household A
When accessed from household B
Then 403 Forbidden or 404 Not Found
```

### AC-7: Idempotency
```
Given create run request with same Idempotency-Key
When called twice
Then same run returned (no duplicate)
```

## Test Strategy

**Unit Tests**:
- ShoppingRunService business logic
- DTO mapping
- Status transition validation

**Integration Tests**:
- Full endpoint flow (create -> mark items -> close)
- Household boundary tests
- Idempotency tests
- Error cases (invalid runId, already closed)

**Test Data**:
- Household with list + items
- Pre-existing runs in various states

## Flags
- contract_impact: yes (new endpoints)
- adr_needed: no (covered by ST-1301 ADR)
- security_sensitive: no
- diagrams_needed: no

## Dependencies
- ST-1301: ShoppingRun entity (BLOCKER)
- Contract: OpenAPI spec for endpoints (BLOCKER)

## Points: 8
