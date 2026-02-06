# Workpack: ST-1302 — ShoppingRun REST Endpoints

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Scope Anchor: `docs/planning/releases/MVP.md`
- Epic: `docs/planning/epics/EP-013/epic.md`
- Story: `docs/planning/epics/EP-013/stories/ST-1302-shopping-run-endpoints.md`
- Contract: `docs/contracts/http/shopping-marketplaces.openapi.yaml`
- ADR-014: `docs/adr/014-shopping-run-entity-design.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** (ST-1301 entity complete, contract approved)

---

## Goal
Implement REST endpoints for ShoppingRun lifecycle per OpenAPI contract: create, list, get, close, update item.

---

## Scope

### In Scope
- `POST /households/{id}/shopping-runs` — Create run from list
- `GET /households/{id}/shopping-runs` — List runs (status filter)
- `GET /households/{id}/shopping-runs/{runId}` — Get run with items
- `POST /households/{id}/shopping-runs/{runId}/close` — Close run
- `PATCH /households/{id}/shopping-runs/{runId}/items/{itemId}` — Update item
- DTOs: CreateShoppingRunRequest, CloseShoppingRunRequest, UpdateRunItemRequest, ShoppingRunDto, ShoppingRunSummaryDto, ShoppingRunItemDto, ItemCounts
- ShoppingRunService with business logic
- Idempotency support for create
- Household boundary enforcement
- Integration tests

### Out of Scope
- Web UI (ST-1307, ST-1308)
- Run editing (add/remove items)

---

## Files to Create/Modify

| Path | Action | Purpose |
|------|--------|---------|
| `services/backend/src/main/java/com/hometusk/shopping/dto/CreateShoppingRunRequest.java` | CREATE | Request DTO |
| `services/backend/src/main/java/com/hometusk/shopping/dto/CloseShoppingRunRequest.java` | CREATE | Request DTO |
| `services/backend/src/main/java/com/hometusk/shopping/dto/UpdateRunItemRequest.java` | CREATE | Request DTO |
| `services/backend/src/main/java/com/hometusk/shopping/dto/ShoppingRunDto.java` | CREATE | Response DTO |
| `services/backend/src/main/java/com/hometusk/shopping/dto/ShoppingRunSummaryDto.java` | CREATE | Response DTO |
| `services/backend/src/main/java/com/hometusk/shopping/dto/ShoppingRunItemDto.java` | CREATE | Response DTO |
| `services/backend/src/main/java/com/hometusk/shopping/dto/ItemCountsDto.java` | CREATE | Nested DTO |
| `services/backend/src/main/java/com/hometusk/shopping/service/ShoppingRunService.java` | CREATE | Business logic |
| `services/backend/src/main/java/com/hometusk/shopping/api/ShoppingRunController.java` | CREATE | REST controller |
| `services/backend/src/test/java/com/hometusk/integration/shopping/ShoppingRunIntegrationTest.java` | CREATE | Integration tests |

---

## Implementation Plan

### Step 1: Create Request DTOs

**CreateShoppingRunRequest.java:**
```java
public record CreateShoppingRunRequest(
    @NotNull UUID listId
) {}
```

**CloseShoppingRunRequest.java:**
```java
public record CloseShoppingRunRequest(
    @NotNull ShoppingRunStatus status  // COMPLETED or CANCELLED only
) {}
```

**UpdateRunItemRequest.java:**
```java
public record UpdateRunItemRequest(
    @NotNull Boolean purchased,
    Boolean syncToList  // default true
) {}
```

### Step 2: Create Response DTOs

**ItemCountsDto.java:**
```java
public record ItemCountsDto(int total, int purchased, int remaining) {}
```

**ShoppingRunItemDto.java:**
```java
public record ShoppingRunItemDto(
    UUID id,
    UUID originalItemId,
    String name,
    Integer quantity,
    String unit,
    boolean purchased,
    Instant purchasedAt
) {}
```

**ShoppingRunDto.java (full):**
- id, householdId, listId, listName, status
- createdBy (UserSummary), createdAt, closedAt
- items (List<ShoppingRunItemDto>)
- itemCounts (ItemCountsDto)

**ShoppingRunSummaryDto.java (for list):**
- Same as ShoppingRunDto but without items list

### Step 3: Create ShoppingRunService

Methods:
- `createRun(UUID householdId, UUID listId, UUID userId, String idempotencyKey)`
- `listRuns(UUID householdId, ShoppingRunStatus status, UUID listId, int limit)`
- `getRun(UUID householdId, UUID runId)`
- `closeRun(UUID householdId, UUID runId, ShoppingRunStatus status)`
- `updateItem(UUID householdId, UUID runId, UUID itemId, boolean purchased, boolean syncToList)`

Business rules:
- Create: snapshot unpurchased items from list
- Close: only ACTIVE runs can be closed
- Update item: only in ACTIVE runs
- Sync to list: optionally update original ShoppingItem

### Step 4: Create ShoppingRunController

Endpoints per contract:
- POST `/api/v1/households/{householdId}/shopping-runs`
- GET `/api/v1/households/{householdId}/shopping-runs`
- GET `/api/v1/households/{householdId}/shopping-runs/{runId}`
- POST `/api/v1/households/{householdId}/shopping-runs/{runId}/close`
- PATCH `/api/v1/households/{householdId}/shopping-runs/{runId}/items/{itemId}`

Use `@PreAuthorize` or membership check for household boundary.

### Step 5: Integration Tests

Test cases:
- AC-1: Create run from list with items
- AC-2: List runs with status filter
- AC-3: Get run with all items
- AC-4: Close run (COMPLETED/CANCELLED)
- AC-5: Mark item purchased + sync
- AC-6: Household boundary (403/404)
- AC-7: Idempotency (same key = same result)

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/services/backend

# Compile
./gradlew compileJava

# Tests
./gradlew test --tests "*ShoppingRun*"

# All tests
./gradlew test
```

---

## Acceptance Criteria Mapping

| AC | Criteria | Verification |
|----|----------|--------------|
| AC-1 | Create run snapshots items | Integration test |
| AC-2 | List with status filter | Integration test |
| AC-3 | Get run with items | Integration test |
| AC-4 | Close run | Integration test |
| AC-5 | Mark item purchased | Integration test |
| AC-6 | Household boundary | Integration test |
| AC-7 | Idempotency | Integration test |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Large story (8 pts) | Medium | Clear contract, step-by-step |
| Snapshot logic complexity | Low | ADR-014 defines approach |

---

## Rollback

- Delete new files
- No DB migration changes (entity from ST-1301)

---

## References

- Entity: `ShoppingRun.java`, `ShoppingRunItem.java` (ST-1301)
- Repository: `ShoppingRunRepository.java` (ST-1301)
- Patterns: `ShoppingController.java`, `TaskController.java`
