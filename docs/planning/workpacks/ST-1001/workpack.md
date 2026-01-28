# Workpack: ST-1001 — Routine Entity + CRUD Endpoints

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Scope Anchor: `docs/planning/initiatives/INIT-2026Q3-recurring-tasks-scheduling.md`
- Epic: `docs/planning/epics/EP-010/epic.md`
- Story: `docs/planning/epics/EP-010/stories/ST-1001-routine-entity-crud.md`
- Sprint: `docs/planning/pi/2026Q1-PI01/sprints/S10/sprint.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI Contract: `docs/contracts/http/routines.openapi.yaml`
- ADR-013: `docs/adr/013-routine-scheduler-design.md`

---

## Status
**Ready** — Approved for implementation

---

## Goal
Create `Routine` JPA entity and REST CRUD endpoints with DB migration, extending `Task` entity with routine linkage fields, enabling the foundation for recurring task scheduling (EP-010).

---

## Scope

### In Scope
- `Routine` JPA entity with all fields
- `RoutineStatus` enum (ACTIVE, PAUSED, DELETED)
- `AssignmentPolicy` enum (FIXED, ROUND_ROBIN, MANUAL)
- `RecurrenceRule` sealed interface with DAILY, WEEKLY, MONTHLY, EVERY_N_DAYS variants
- `RoutineRepository` with household-scoped queries
- `RoutineService` with CRUD operations
- `RoutineController` REST endpoints (5 endpoints)
- Task entity extension: add `routineId`, `scheduledDate` fields
- DB migration V021 with constraints and indexes
- Request/Response DTOs matching OpenAPI contract
- Household boundary enforcement (403 for non-members)

### Out of Scope
- Recurrence rule parsing/next-date calculation (ST-1002)
- RoutineSchedulerService (ST-1003)
- Assignment logic implementation (ST-1004)
- Pause/resume endpoints (ST-1006)
- UI (ST-1005)

---

## Files to Change/Create

### New Files

| Path | Purpose |
|------|---------|
| `services/backend/src/main/java/com/hometusk/routines/domain/Routine.java` | JPA entity |
| `services/backend/src/main/java/com/hometusk/routines/domain/RoutineStatus.java` | Status enum |
| `services/backend/src/main/java/com/hometusk/routines/domain/AssignmentPolicy.java` | Policy enum |
| `services/backend/src/main/java/com/hometusk/routines/domain/RecurrenceRule.java` | Sealed interface + records |
| `services/backend/src/main/java/com/hometusk/routines/domain/RoundRobinState.java` | State record |
| `services/backend/src/main/java/com/hometusk/routines/repository/RoutineRepository.java` | Spring Data JPA |
| `services/backend/src/main/java/com/hometusk/routines/service/RoutineService.java` | Business logic |
| `services/backend/src/main/java/com/hometusk/routines/api/RoutineController.java` | REST endpoints |
| `services/backend/src/main/java/com/hometusk/routines/dto/CreateRoutineRequest.java` | Request DTO |
| `services/backend/src/main/java/com/hometusk/routines/dto/UpdateRoutineRequest.java` | Request DTO |
| `services/backend/src/main/java/com/hometusk/routines/dto/RoutineDto.java` | Response DTO |
| `services/backend/src/main/java/com/hometusk/routines/dto/UserSummaryDto.java` | Embedded user |
| `services/backend/src/main/java/com/hometusk/routines/dto/ZoneSummaryDto.java` | Embedded zone |
| `services/backend/src/main/resources/db/migration/V021__create_routines.sql` | DB migration |
| `services/backend/src/test/java/com/hometusk/routines/service/RoutineServiceTest.java` | Unit tests |
| `services/backend/src/test/java/com/hometusk/integration/RoutineControllerIntegrationTest.java` | Integration tests |

### Modified Files

| Path | Changes |
|------|---------|
| `services/backend/src/main/java/com/hometusk/tasks/domain/Task.java` | Add `routineId`, `scheduledDate` fields |
| `services/backend/src/main/java/com/hometusk/shared/exception/ErrorCode.java` | Add ROUTINE_NOT_FOUND, INVALID_RECURRENCE_RULE |

---

## Implementation Plan

### Step 1: Create DB Migration V021

**File:** `services/backend/src/main/resources/db/migration/V021__create_routines.sql`

```sql
-- V021: Create routines table for EP-010 Recurring Tasks

CREATE TABLE routines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    zone_id UUID REFERENCES zones(id) ON DELETE SET NULL,
    recurrence_rule JSONB NOT NULL,
    assignment_policy VARCHAR(20) NOT NULL,
    fixed_assignee_id UUID REFERENCES users(id) ON DELETE SET NULL,
    round_robin_state JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    generation_window_days INTEGER NOT NULL DEFAULT 7,
    created_by_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    paused_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT routines_status_check CHECK (status IN ('ACTIVE', 'PAUSED', 'DELETED')),
    CONSTRAINT routines_assignment_policy_check CHECK (assignment_policy IN ('FIXED', 'ROUND_ROBIN', 'MANUAL')),
    CONSTRAINT routines_generation_window_check CHECK (generation_window_days BETWEEN 1 AND 30)
);

-- Indexes
CREATE INDEX idx_routines_household_id ON routines(household_id);
CREATE INDEX idx_routines_household_status ON routines(household_id, status);

-- Extend tasks table for routine linkage
ALTER TABLE tasks ADD COLUMN routine_id UUID REFERENCES routines(id) ON DELETE SET NULL;
ALTER TABLE tasks ADD COLUMN scheduled_date DATE;

-- Constraint: both fields set or both null (ADR-013)
ALTER TABLE tasks ADD CONSTRAINT chk_routine_date_consistency
    CHECK ((routine_id IS NULL) = (scheduled_date IS NULL));

-- Partial unique index: one task per routine per scheduled date (ADR-013)
CREATE UNIQUE INDEX idx_task_routine_scheduled_date
    ON tasks (routine_id, scheduled_date)
    WHERE routine_id IS NOT NULL;

-- Index for finding routine tasks
CREATE INDEX idx_tasks_routine_id ON tasks(routine_id) WHERE routine_id IS NOT NULL;
```

### Step 2: Create Domain Enums

**RoutineStatus.java:**
```java
public enum RoutineStatus { ACTIVE, PAUSED, DELETED }
```

**AssignmentPolicy.java:**
```java
public enum AssignmentPolicy { FIXED, ROUND_ROBIN, MANUAL }
```

### Step 3: Create RecurrenceRule (Sealed Interface)

```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = RecurrenceRule.Daily.class, name = "DAILY"),
    @JsonSubTypes.Type(value = RecurrenceRule.Weekly.class, name = "WEEKLY"),
    @JsonSubTypes.Type(value = RecurrenceRule.Monthly.class, name = "MONTHLY"),
    @JsonSubTypes.Type(value = RecurrenceRule.EveryNDays.class, name = "EVERY_N_DAYS")
})
public sealed interface RecurrenceRule permits
    RecurrenceRule.Daily, RecurrenceRule.Weekly,
    RecurrenceRule.Monthly, RecurrenceRule.EveryNDays {

    record Daily() implements RecurrenceRule {}
    record Weekly(List<DayOfWeek> daysOfWeek) implements RecurrenceRule {}
    record Monthly(int dayOfMonth) implements RecurrenceRule {}
    record EveryNDays(int interval) implements RecurrenceRule {}
}
```

### Step 4: Create Routine Entity

Follow patterns from `Task.java`:
- UUID primary key
- ManyToOne to Household, Zone, User (createdBy, fixedAssignee)
- JSONB for recurrenceRule and roundRobinState using `@Type(JsonType.class)`
- Enum fields with `@Enumerated(EnumType.STRING)`
- `softDelete()` method for status change
- Timestamps (createdAt, updatedAt, pausedAt)

### Step 5: Extend Task Entity

Add to `Task.java`:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "routine_id")
private Routine routine;

@Column(name = "scheduled_date")
private LocalDate scheduledDate;
```

### Step 6: Create Repository

```java
@Repository
public interface RoutineRepository extends JpaRepository<Routine, UUID> {
    List<Routine> findByHousehold_IdAndStatusInOrderByCreatedAtDesc(UUID householdId, List<RoutineStatus> statuses);
    Optional<Routine> findByIdAndHousehold_Id(UUID id, UUID householdId);
}
```

### Step 7: Create Service

RoutineService with:
- `listRoutines(householdId, status)` — filter by status, exclude DELETED by default
- `getRoutine(routineId, householdId)` — 404 if not found
- `createRoutine(householdId, request, createdBy)` — validate zone, fixedAssignee, rule
- `updateRoutine(routineId, householdId, request)` — partial update
- `deleteRoutine(routineId, householdId)` — soft delete

Validation:
- WEEKLY requires daysOfWeek non-empty
- MONTHLY dayOfMonth 1-31
- EVERY_N_DAYS interval 2-365
- FIXED requires fixedAssigneeId + member check

### Step 8: Create Controller

5 endpoints matching OpenAPI:
- `GET /households/{householdId}/routines`
- `POST /households/{householdId}/routines`
- `GET /households/{householdId}/routines/{routineId}`
- `PATCH /households/{householdId}/routines/{routineId}`
- `DELETE /households/{householdId}/routines/{routineId}`

All endpoints call `membershipService.requireMembership()` first.

### Step 9: Create DTOs

Match OpenAPI schemas:
- `CreateRoutineRequest` — @Valid with @NotBlank title, @NotNull recurrenceRule
- `UpdateRoutineRequest` — all fields nullable for partial update
- `RoutineDto` — with `from(Routine)` mapper
- `UserSummaryDto`, `ZoneSummaryDto`

### Step 10: Write Tests

**Unit tests (RoutineServiceTest):**
- create/update/delete validation
- recurrence rule validation for each type
- partial update logic

**Integration tests (RoutineControllerIntegrationTest):**
- All 10 ACs covered

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/services/backend

# Build
./gradlew build

# Unit tests
./gradlew test --tests "*RoutineServiceTest*"

# Integration tests
./gradlew test --tests "*RoutineControllerIntegrationTest*"

# All routine tests
./gradlew test --tests "*Routine*"

# All tests
./gradlew test

# Spotless
./gradlew spotlessCheck
./gradlew spotlessApply
```

---

## Tests Required

| Test Class | Scenarios |
|------------|-----------|
| `RoutineServiceTest` | create validation, update partial, delete soft, list filtering |
| `RecurrenceRuleSerializationTest` | JSON for all 4 rule types |
| `RoutineControllerIntegrationTest` | All 10 ACs |

---

## Rollout / Rollback

### Rollout
- Deploy migration V021 (backward compatible: new columns nullable)
- Deploy code
- No feature flag needed

### Rollback
- Revert code deployment
- Migration can remain
- If needed: `DROP TABLE routines; ALTER TABLE tasks DROP COLUMN routine_id, scheduled_date;`

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| RecurrenceRule JSON issues | HIGH | Unit tests for all types |
| hibernate-types dependency | MEDIUM | Check build.gradle |
| Task entity breaks existing tests | MEDIUM | Nullable fields |

---

## Done Criteria

| AC | Criteria | Verification |
|----|----------|--------------|
| AC-1 | Create routine -> 201, UUID id | Integration test |
| AC-2 | List excludes DELETED | Integration test |
| AC-3 | Get returns full object | Integration test |
| AC-4 | PATCH updates only provided | Integration test |
| AC-5 | DELETE soft-deletes | Integration test |
| AC-6 | Task extended + constraints | Migration |
| AC-7 | Missing title -> 400 | Integration test |
| AC-8 | Missing rule -> 400 | Integration test |
| AC-9 | Non-member -> 403 | Integration test |
| AC-10 | Invalid zone -> 400 | Integration test |
