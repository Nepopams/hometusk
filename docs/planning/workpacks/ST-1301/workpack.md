# Workpack: ST-1301 — ShoppingRun Entity + Repository

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Scope Anchor: `docs/planning/releases/MVP.md`
- Epic: `docs/planning/epics/EP-013/epic.md`
- Story: `docs/planning/epics/EP-013/stories/ST-1301-shopping-run-entity.md`
- Contract: `docs/contracts/http/shopping-marketplaces.openapi.yaml`
- ADR: `docs/adr/014-shopping-run-entity-design.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — Contract and ADR approved 2026-02-03

---

## Goal
Create `ShoppingRun` and `ShoppingRunItem` JPA entities with repository and DB migration V025, enabling the foundation for "shopping trip" tracking functionality (EP-013).

---

## Scope

### In Scope
- `ShoppingRun` JPA entity with all fields per ADR-014
- `ShoppingRunItem` JPA entity with snapshot fields
- `ShoppingRunStatus` enum (ACTIVE, COMPLETED, CANCELLED)
- `ShoppingRunRepository` with household-scoped queries
- `ShoppingRunItemRepository` for item queries
- DB migration V025 with constraints and indexes
- Unit tests for entity methods
- Integration tests for repository CRUD

### Out of Scope
- REST endpoints (ST-1302)
- Service layer business logic (ST-1302)
- Web UI (ST-1307, ST-1308)
- Multi-store runs
- Run templates/presets

---

## Files to Create

| Path | Purpose |
|------|---------|
| `services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingRun.java` | JPA entity for shopping runs |
| `services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingRunItem.java` | JPA entity for run item snapshots |
| `services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingRunStatus.java` | Status enum |
| `services/backend/src/main/java/com/hometusk/shopping/repository/ShoppingRunRepository.java` | Spring Data JPA repository |
| `services/backend/src/main/java/com/hometusk/shopping/repository/ShoppingRunItemRepository.java` | Spring Data JPA repository |
| `services/backend/src/main/resources/db/migration/V025__create_shopping_runs.sql` | DB migration |
| `services/backend/src/test/java/com/hometusk/shopping/domain/ShoppingRunTest.java` | Unit tests |
| `services/backend/src/test/java/com/hometusk/integration/shopping/ShoppingRunRepositoryIntegrationTest.java` | Integration tests |

---

## Implementation Plan

### Step 1: Create DB Migration V025

**File:** `services/backend/src/main/resources/db/migration/V025__create_shopping_runs.sql`

**Key elements:**
- shopping_runs table with household_id, source_list_id, list_name, status, created_by_id, created_at, closed_at
- shopping_run_items table with run_id, original_item_id, name, quantity, unit, purchased, purchased_at
- CHECK constraint: status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')
- FK to households with ON DELETE CASCADE
- FK to shopping_lists with ON DELETE RESTRICT
- FK to shopping_items with ON DELETE SET NULL
- Indexes for household_id, source_list_id, run_id, original_item_id
- IF NOT EXISTS guards for idempotency

### Step 2: Create ShoppingRunStatus Enum

**File:** `services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingRunStatus.java`

Simple enum: ACTIVE, COMPLETED, CANCELLED

### Step 3: Create ShoppingRun Entity

**File:** `services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingRun.java`

**Key elements:**
- UUID primary key with @GeneratedValue(strategy = GenerationType.UUID)
- @ManyToOne to Household (fetch LAZY)
- @ManyToOne to ShoppingList (sourceList, fetch LAZY)
- @ManyToOne to User (createdBy, fetch LAZY)
- listName String field (snapshot at creation)
- status @Enumerated(EnumType.STRING)
- createdAt Instant (not null)
- closedAt Instant (nullable)
- @OneToMany to ShoppingRunItem (mappedBy="run", cascade ALL, orphanRemoval true)

**Methods:**
- Constructor(Household, ShoppingList, User) - sets status=ACTIVE, createdAt=now
- close(ShoppingRunStatus) - validates ACTIVE state, sets closedAt
- isActive() - returns status == ACTIVE
- getItemCounts() - returns ItemCounts record

### Step 4: Create ShoppingRunItem Entity

**File:** `services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingRunItem.java`

**Key elements:**
- UUID primary key
- @ManyToOne to ShoppingRun (fetch LAZY)
- originalItemId UUID (nullable)
- name, quantity, unit (snapshot fields)
- purchased boolean, purchasedAt Instant

**Factory method:**
- static fromShoppingItem(ShoppingRun run, ShoppingItem item)

**Methods:**
- markPurchased() / unmarkPurchased()

### Step 5: Create ItemCounts Record

```java
public record ItemCounts(int total, int purchased, int remaining) {}
```

### Step 6: Create ShoppingRunRepository

**File:** `services/backend/src/main/java/com/hometusk/shopping/repository/ShoppingRunRepository.java`

**Methods:**
- findByIdAndHousehold_Id(UUID, UUID)
- findByHousehold_IdOrderByCreatedAtDesc(UUID)
- findByHousehold_IdAndStatusOrderByCreatedAtDesc(UUID, ShoppingRunStatus)
- existsByIdAndHousehold_Id(UUID, UUID)

### Step 7: Create ShoppingRunItemRepository

**File:** `services/backend/src/main/java/com/hometusk/shopping/repository/ShoppingRunItemRepository.java`

**Methods:**
- findByRun_Id(UUID)
- countByRun_IdAndPurchasedTrue(UUID)

### Step 8: Write Unit Tests

**File:** `services/backend/src/test/java/com/hometusk/shopping/domain/ShoppingRunTest.java`

**Test cases:**
- newRun_hasActiveStatus_andCreatedAt (AC-1)
- snapshotItem_copiesDataFromShoppingItem (AC-2)
- closeAsCompleted_setsStatusAndClosedAt (AC-3)
- closeAlreadyClosed_sameStatus_isIdempotent
- closeAlreadyClosed_differentStatus_throwsException
- markItemPurchased_setsTimestamp
- getItemCounts_returnsCorrectValues

### Step 9: Write Integration Tests

**File:** `services/backend/src/test/java/com/hometusk/integration/shopping/ShoppingRunRepositoryIntegrationTest.java`

**Test cases:**
- createAndFindRun_works (AC-1)
- findByHouseholdId_returnsOnlyHouseholdRuns (AC-4)
- runWithItems_cascadesProperly (AC-2)
- findByWrongHousehold_returnsEmpty (AC-4 - IDOR prevention)

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/services/backend

# Build
./gradlew build

# Spotless
./gradlew spotlessCheck
./gradlew spotlessApply

# Unit tests
./gradlew test --tests "*ShoppingRunTest*"

# Integration tests
./gradlew test --tests "*ShoppingRunRepositoryIntegrationTest*"

# All tests
./gradlew test
```

---

## Acceptance Criteria Mapping

| AC | Criteria | Verification |
|----|----------|--------------|
| AC-1 | Entity created with status=ACTIVE, createdAt=now | Unit test |
| AC-2 | Item snapshot copies name/qty/unit from ShoppingItem | Unit test |
| AC-3 | close() sets status + closedAt | Unit test |
| AC-4 | Household boundary in all repository queries | Integration test |
| AC-5 | Migration uses IF NOT EXISTS | Migration file |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Migration V025 conflicts | HIGH | Check existing migrations first |
| FK RESTRICT blocks list deletion | MEDIUM | Intentional per ADR-014 |
| Cascade delete items | LOW | Expected, covered by tests |

---

## Rollback

- Revert code deployment
- Migration can remain (orphaned tables harmless)
- Full rollback: `DROP TABLE shopping_run_items; DROP TABLE shopping_runs;`

---

## References

- ADR-014: Entity structure, snapshot strategy
- Contract: ShoppingRunDto, ShoppingRunItemDto schemas
- Patterns: ShoppingList.java, ShoppingItem.java
