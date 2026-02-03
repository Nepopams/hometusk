# DoD Checklist: ST-1301 — ShoppingRun Entity + Repository

## Code Quality
- [ ] Code follows project conventions (Java 21, Spring Boot idioms)
- [ ] Spotless formatting applied (`./gradlew spotlessApply`)
- [ ] No compiler warnings introduced
- [ ] Package structure correct

## Database Migration
- [ ] V025 migration created and runs successfully
- [ ] `shopping_runs` table with all columns:
  - [ ] id (UUID PRIMARY KEY)
  - [ ] household_id (UUID NOT NULL, FK)
  - [ ] source_list_id (UUID NOT NULL, FK)
  - [ ] list_name (VARCHAR(255) NOT NULL)
  - [ ] status (VARCHAR(20) NOT NULL, CHECK)
  - [ ] created_by_id (UUID NOT NULL, FK)
  - [ ] created_at (TIMESTAMP WITH TIME ZONE NOT NULL)
  - [ ] closed_at (TIMESTAMP WITH TIME ZONE, nullable)
- [ ] `shopping_run_items` table with all columns:
  - [ ] id (UUID PRIMARY KEY)
  - [ ] run_id (UUID NOT NULL, FK)
  - [ ] original_item_id (UUID, nullable, FK)
  - [ ] name (VARCHAR(255) NOT NULL)
  - [ ] quantity (INTEGER NOT NULL DEFAULT 1)
  - [ ] unit (VARCHAR(50), nullable)
  - [ ] purchased (BOOLEAN NOT NULL DEFAULT FALSE)
  - [ ] purchased_at (TIMESTAMP WITH TIME ZONE, nullable)
- [ ] FK constraints correct (CASCADE/RESTRICT/SET NULL per ADR)
- [ ] Indexes created
- [ ] IF NOT EXISTS guards (AC-5)

## Entities
- [ ] `ShoppingRunStatus` enum: ACTIVE, COMPLETED, CANCELLED
- [ ] `ShoppingRun` entity maps to DB correctly
  - [ ] @ManyToOne to Household, ShoppingList, User (LAZY)
  - [ ] @OneToMany to ShoppingRunItem (cascade ALL, orphanRemoval)
  - [ ] Constructor sets status=ACTIVE, createdAt=now (AC-1)
  - [ ] close(status) validates ACTIVE, sets closedAt (AC-3)
  - [ ] getItemCounts() returns correct values
- [ ] `ShoppingRunItem` entity maps to DB correctly
  - [ ] Factory: fromShoppingItem(run, item) creates snapshot (AC-2)
  - [ ] markPurchased() / unmarkPurchased() methods
- [ ] `ItemCounts` record created

## Repositories
- [ ] `ShoppingRunRepository` with household-scoped queries
  - [ ] findByIdAndHousehold_Id
  - [ ] findByHousehold_IdOrderByCreatedAtDesc
  - [ ] findByHousehold_IdAndStatusOrderByCreatedAtDesc
- [ ] `ShoppingRunItemRepository` created
  - [ ] findByRun_Id
  - [ ] countByRun_IdAndPurchasedTrue

## Tests
- [ ] Unit tests pass (`ShoppingRunTest`)
  - [ ] newRun_hasActiveStatus_andCreatedAt
  - [ ] snapshotItem_copiesDataFromShoppingItem
  - [ ] closeAsCompleted_setsStatusAndClosedAt
  - [ ] markItemPurchased_setsTimestamp
  - [ ] getItemCounts_returnsCorrectValues
- [ ] Integration tests pass (`ShoppingRunRepositoryIntegrationTest`)
  - [ ] createAndFindRun_works
  - [ ] findByWrongHousehold_returnsEmpty (AC-4)
  - [ ] runWithItems_cascadesProperly

## Security
- [ ] All repository queries include householdId (IDOR prevention) (AC-4)
- [ ] No cross-household data access
- [ ] No PII in logs

## Build
- [ ] `./gradlew build` passes
- [ ] `./gradlew spotlessCheck` passes
- [ ] All tests pass

## Acceptance Criteria
- [ ] AC-1: Entity creation with status=ACTIVE, createdAt=now
- [ ] AC-2: Item snapshot from list
- [ ] AC-3: Run completion sets status + closedAt
- [ ] AC-4: Household boundary enforced
- [ ] AC-5: Migration idempotent

## Final
- [ ] PR reviewed
- [ ] All checklist items complete
- [ ] Ready to merge
