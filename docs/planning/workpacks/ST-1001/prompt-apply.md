# Codex APPLY Prompt: ST-1001 — Routine Entity + CRUD Endpoints

## Mode
**APPLY** — Implementation mode. File modifications allowed.

## Objective
Implement ST-1001 (Routine Entity + CRUD Endpoints) following the approved implementation plan.

---

## Sources of Truth (MUST READ FIRST)

```
docs/planning/workpacks/ST-1001/workpack.md       # Implementation plan
docs/planning/workpacks/ST-1001/checklist.md      # DoD checklist
docs/planning/epics/EP-010/stories/ST-1001-routine-entity-crud.md  # Story + ACs
docs/contracts/http/routines.openapi.yaml         # API contract
docs/adr/013-routine-scheduler-design.md          # Architecture decisions
```

---

## Approved Implementation Plan Summary

### Technical Decisions (CONFIRMED)

1. **JSONB storage**: Use `@JdbcTypeCode(SqlTypes.JSON)` + String (existing pattern)
   - NO hypersistence-utils
   - Serialize via ObjectMapper in service layer

2. **PATCH semantics**: v0 limitation accepted
   - `null` in DTO = don't change field
   - Cannot explicitly clear fields to null
   - To clear description: send empty string `""`

3. **ErrorCodes**:
   - Add `ROUTINE_NOT_FOUND` for 404
   - Add `INVALID_RECURRENCE_RULE` for rule validation
   - Use `BUSINESS_RULE_VIOLATION` with `violations[].rule` for business errors

---

## Implementation Order

Execute in this exact order:

### Step 1: DB Migration V021

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

CREATE INDEX idx_routines_household_id ON routines(household_id);
CREATE INDEX idx_routines_household_status ON routines(household_id, status);

ALTER TABLE tasks ADD COLUMN routine_id UUID REFERENCES routines(id) ON DELETE SET NULL;
ALTER TABLE tasks ADD COLUMN scheduled_date DATE;

ALTER TABLE tasks ADD CONSTRAINT chk_routine_date_consistency
    CHECK ((routine_id IS NULL) = (scheduled_date IS NULL));

CREATE UNIQUE INDEX idx_task_routine_scheduled_date
    ON tasks (routine_id, scheduled_date)
    WHERE routine_id IS NOT NULL;

CREATE INDEX idx_tasks_routine_id ON tasks(routine_id) WHERE routine_id IS NOT NULL;
```

### Step 2: Domain Enums

**File:** `services/backend/src/main/java/com/hometusk/routines/domain/RoutineStatus.java`
```java
package com.hometusk.routines.domain;

public enum RoutineStatus {
    ACTIVE,
    PAUSED,
    DELETED
}
```

**File:** `services/backend/src/main/java/com/hometusk/routines/domain/AssignmentPolicy.java`
```java
package com.hometusk.routines.domain;

public enum AssignmentPolicy {
    FIXED,
    ROUND_ROBIN,
    MANUAL
}
```

### Step 3: RecurrenceRule (Sealed Interface)

**File:** `services/backend/src/main/java/com/hometusk/routines/domain/RecurrenceRule.java`
```java
package com.hometusk.routines.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.DayOfWeek;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = RecurrenceRule.Daily.class, name = "DAILY"),
    @JsonSubTypes.Type(value = RecurrenceRule.Weekly.class, name = "WEEKLY"),
    @JsonSubTypes.Type(value = RecurrenceRule.Monthly.class, name = "MONTHLY"),
    @JsonSubTypes.Type(value = RecurrenceRule.EveryNDays.class, name = "EVERY_N_DAYS")
})
public sealed interface RecurrenceRule permits
        RecurrenceRule.Daily,
        RecurrenceRule.Weekly,
        RecurrenceRule.Monthly,
        RecurrenceRule.EveryNDays {

    record Daily() implements RecurrenceRule {}

    record Weekly(List<DayOfWeek> daysOfWeek) implements RecurrenceRule {}

    record Monthly(int dayOfMonth) implements RecurrenceRule {}

    record EveryNDays(int interval) implements RecurrenceRule {}
}
```

**File:** `services/backend/src/main/java/com/hometusk/routines/domain/RoundRobinState.java`
```java
package com.hometusk.routines.domain;

import java.util.List;
import java.util.UUID;

public record RoundRobinState(UUID lastAssignedUserId, List<UUID> memberOrder) {}
```

### Step 4: Routine Entity

**File:** `services/backend/src/main/java/com/hometusk/routines/domain/Routine.java`

Key points:
- UUID primary key with `@GeneratedValue(strategy = GenerationType.UUID)`
- `@ManyToOne(fetch = FetchType.LAZY)` for household, zone, fixedAssignee, createdBy
- `@JdbcTypeCode(SqlTypes.JSON)` for recurrenceRuleJson and roundRobinStateJson (String fields)
- `@Enumerated(EnumType.STRING)` for status and assignmentPolicy
- `softDelete()` method sets status = DELETED and updates updatedAt
- Constructor with required fields (household, title, recurrenceRuleJson, assignmentPolicy, createdBy)

Follow pattern from `Task.java` for entity structure.

### Step 5: Extend Task Entity

**File:** `services/backend/src/main/java/com/hometusk/tasks/domain/Task.java`

Add:
```java
import com.hometusk.routines.domain.Routine;
import java.time.LocalDate;

// ... existing fields ...

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "routine_id")
private Routine routine;

@Column(name = "scheduled_date")
private LocalDate scheduledDate;

// Getters
public Routine getRoutine() {
    return routine;
}

public UUID getRoutineId() {
    return routine != null ? routine.getId() : null;
}

public LocalDate getScheduledDate() {
    return scheduledDate;
}

// Setters (for scheduler use in ST-1003)
public void setRoutine(Routine routine) {
    this.routine = routine;
}

public void setScheduledDate(LocalDate scheduledDate) {
    this.scheduledDate = scheduledDate;
}
```

### Step 6: ErrorCode

**File:** `services/backend/src/main/java/com/hometusk/shared/exception/ErrorCode.java`

Add to enum:
```java
// Routines (EP-010)
ROUTINE_NOT_FOUND("Routine not found"),
INVALID_RECURRENCE_RULE("Invalid recurrence rule"),
```

### Step 7: Repository

**File:** `services/backend/src/main/java/com/hometusk/routines/repository/RoutineRepository.java`
```java
package com.hometusk.routines.repository;

import com.hometusk.routines.domain.AssignmentPolicy;
import com.hometusk.routines.domain.Routine;
import com.hometusk.routines.domain.RoutineStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoutineRepository extends JpaRepository<Routine, UUID> {

    List<Routine> findByHousehold_IdAndStatusInOrderByCreatedAtDesc(
            UUID householdId, List<RoutineStatus> statuses);

    List<Routine> findByHousehold_IdAndStatusOrderByCreatedAtDesc(
            UUID householdId, RoutineStatus status);

    List<Routine> findByHousehold_IdAndStatusInAndAssignmentPolicyOrderByCreatedAtDesc(
            UUID householdId, List<RoutineStatus> statuses, AssignmentPolicy assignmentPolicy);

    Optional<Routine> findByIdAndHousehold_Id(UUID id, UUID householdId);

    boolean existsByIdAndHousehold_Id(UUID id, UUID householdId);
}
```

### Step 8: DTOs

Create in `services/backend/src/main/java/com/hometusk/routines/dto/`:

- `CreateRoutineRequest.java` - @NotBlank title, @NotNull recurrenceRule, @NotNull assignmentPolicy
- `UpdateRoutineRequest.java` - all nullable fields for partial update
- `RoutineDto.java` - response DTO with `from(Routine, ObjectMapper)` mapper
- `UserSummaryDto.java` - id + displayName
- `ZoneSummaryDto.java` - id + name

Match OpenAPI schemas exactly.

### Step 9: Service

**File:** `services/backend/src/main/java/com/hometusk/routines/service/RoutineService.java`

Dependencies:
- RoutineRepository
- HouseholdService (or HouseholdRepository)
- ZoneRepository
- UserService
- MembershipService
- ObjectMapper

Methods:
- `listRoutines(householdId, status, assignmentPolicy)` - exclude DELETED by default
- `getRoutine(routineId, householdId)` - throw NotFoundException if not found
- `createRoutine(householdId, request, createdBy)` - validate + serialize JSON + save
- `updateRoutine(routineId, householdId, request)` - partial update (non-null fields only)
- `deleteRoutine(routineId, householdId)` - soft delete

Validation:
- WEEKLY: daysOfWeek must be non-empty
- MONTHLY: dayOfMonth must be 1-31
- EVERY_N_DAYS: interval must be 2-365
- FIXED policy: fixedAssigneeId required + must be household member
- zoneId: must exist in household

### Step 10: Controller

**File:** `services/backend/src/main/java/com/hometusk/routines/api/RoutineController.java`

5 endpoints:
- `GET /api/v1/households/{householdId}/routines` → 200 List<RoutineDto>
- `POST /api/v1/households/{householdId}/routines` → 201 RoutineDto
- `GET /api/v1/households/{householdId}/routines/{routineId}` → 200 RoutineDto
- `PATCH /api/v1/households/{householdId}/routines/{routineId}` → 200 RoutineDto
- `DELETE /api/v1/households/{householdId}/routines/{routineId}` → 204

Every endpoint MUST call:
```java
CurrentUser currentUser = userResolver.resolveCurrentUser();
membershipService.requireMembership(currentUser.id(), householdId);
```

Follow ShoppingController pattern for structure.

### Step 11: Unit Tests

**File:** `services/backend/src/test/java/com/hometusk/routines/service/RoutineServiceTest.java`

Test scenarios:
- `createRoutine_withValidData_succeeds`
- `createRoutine_missingTitle_throwsValidation`
- `createRoutine_missingRecurrenceRule_throwsValidation`
- `createRoutine_weeklyWithoutDays_throwsBusinessViolation`
- `createRoutine_monthlyInvalidDay_throwsBusinessViolation`
- `createRoutine_everyNDaysInvalidInterval_throwsBusinessViolation`
- `createRoutine_fixedWithoutAssignee_throwsBusinessViolation`
- `createRoutine_fixedNonMemberAssignee_throwsBusinessViolation`
- `createRoutine_invalidZone_throwsBusinessViolation`
- `updateRoutine_partialUpdate_updatesOnlyNonNull`
- `deleteRoutine_setsStatusDeleted`
- `listRoutines_excludesDeletedByDefault`

### Step 12: Integration Tests

**File:** `services/backend/src/test/java/com/hometusk/integration/RoutineControllerIntegrationTest.java`

Test all 10 ACs:
- AC-1: `createRoutine_asMember_returns201_withUuidAndActiveStatus`
- AC-2: `listRoutines_excludesDeleted_byDefault`
- AC-3: `getRoutine_returnsFullObject`
- AC-4: `updateRoutine_partialUpdate_changesOnlyProvidedFields`
- AC-5: `deleteRoutine_softDeletes_returns204`
- AC-6: (covered by migration - Task has routineId/scheduledDate)
- AC-7: `createRoutine_missingTitle_returns400`
- AC-8: `createRoutine_missingRecurrenceRule_returns400`
- AC-9: `createRoutine_asNonMember_returns403`
- AC-10: `createRoutine_invalidZone_returns400`

Follow existing integration test patterns (TestContainers, @WithMockUser, etc.)

---

## Verification Commands

After implementation, run:

```bash
cd /home/vad/Документы/hometusk/services/backend

# Format code
./gradlew spotlessApply

# Build (includes compilation)
./gradlew build

# Run all tests
./gradlew test

# Run only routine tests
./gradlew test --tests "*Routine*"

# Check spotless
./gradlew spotlessCheck
```

All tests MUST pass before completing.

---

## Critical Constraints (MUST FOLLOW)

1. **Package structure**: `com.hometusk.routines.domain`, `.repository`, `.service`, `.api`, `.dto`
2. **JSONB pattern**: Use `@JdbcTypeCode(SqlTypes.JSON)` + String, serialize via ObjectMapper
3. **Membership enforcement**: Call `membershipService.requireMembership()` in ALL endpoints
4. **Soft delete**: DELETE sets status=DELETED, does not remove row
5. **PATCH semantics**: null = don't change, empty string for description = clear
6. **No new dependencies**: Do not add hypersistence-utils or jackson-databind-nullable

---

## STOP-THE-LINE

If you encounter:
- Compilation errors that can't be resolved
- Test failures indicating design issues
- Missing dependencies or services
- Conflicting patterns in existing code

STOP and report the issue. Do NOT proceed with workarounds.

---

## Deliverable

All files implemented, formatted, and tests passing. Ready for review gate.
