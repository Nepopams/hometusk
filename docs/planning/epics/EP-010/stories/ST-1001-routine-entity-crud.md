# Story: ST-1001 — Routine Entity + CRUD Endpoints

## Sources of Truth
- Epic: `docs/planning/epics/EP-010/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q3-recurring-tasks-scheduling.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Draft** — Awaiting Human Gate approval

## User Value
> "Хочу создать рутину 'Мыть посуду' и указать зону кухня — это шаблон для будущих повторяющихся задач."

---

## Description
Implement Routine entity and REST CRUD endpoints:
- Domain entity with validation
- Repository with household scoping
- REST controller with membership check
- DB migration for `routines` table
- Extend Task entity with `routineId` and `scheduledDate` fields

---

## In Scope
- `Routine` JPA entity with all fields from epic Data Model
- `RoutineRepository` with household-scoped queries
- `RoutineService` with CRUD operations
- `RoutineController` REST endpoints:
  - `GET /households/{id}/routines` (list)
  - `POST /households/{id}/routines` (create)
  - `GET /households/{id}/routines/{routineId}` (get)
  - `PUT /households/{id}/routines/{routineId}` (update)
  - `DELETE /households/{id}/routines/{routineId}` (soft delete)
- Task entity extension: add `routineId`, `scheduledDate` fields
- DB migration for new tables/columns
- OpenAPI contract update

## Out of Scope
- Recurrence rule parsing (ST-1002)
- Scheduler service (ST-1003)
- Assignment logic (ST-1004)
- Pause/resume endpoints (ST-1006)
- UI (ST-1005)

---

## Acceptance Criteria

### AC-1: Routine entity created
```
Given admin user in household
When POST /households/{householdId}/routines with:
  {
    "title": "Мыть посуду",
    "zoneId": "<kitchen-zone-id>",
    "recurrenceRule": { "type": "DAILY" },
    "assignmentPolicy": "ROUND_ROBIN"
  }
Then response 201 with created Routine
And routine.id is UUID
And routine.status = ACTIVE
And routine.createdBy = current user
```

### AC-2: List routines (household scoped)
```
Given household with 3 routines (2 active, 1 deleted)
When GET /households/{householdId}/routines
Then response 200 with array of 2 routines (active only by default)
And each routine belongs to householdId
```

### AC-3: Get routine by ID
```
Given existing routine in household
When GET /households/{householdId}/routines/{routineId}
Then response 200 with full Routine object
And includes all fields (title, zone, rule, policy, status)
```

### AC-4: Update routine
```
Given existing active routine
When PUT /households/{householdId}/routines/{routineId} with:
  { "title": "Помыть посуду тщательно" }
Then response 200 with updated Routine
And routine.title = "Помыть посуду тщательно"
And routine.updatedAt > routine.createdAt
```

### AC-5: Soft delete routine
```
Given existing active routine
When DELETE /households/{householdId}/routines/{routineId}
Then response 204
And routine.status = DELETED (not physically removed)
And routine not in list response
```

### AC-6: Task entity extended
```
Given Task entity
Then new nullable fields exist:
  - routineId: UUID (FK to routines.id)
  - scheduledDate: LocalDate
And unique constraint on (routineId, scheduledDate) where both not null
```

### AC-7: Validation - title required
```
Given POST request without title
When creating routine
Then response 400 with validation error
```

### AC-8: Validation - recurrenceRule required
```
Given POST request without recurrenceRule
When creating routine
Then response 400 with validation error
```

### AC-9: Household boundary enforced
```
Given user NOT member of householdId
When any routine endpoint called
Then response 403 Forbidden
```

### AC-10: Zone must exist in household
```
Given zoneId that does not exist in household
When creating routine with that zoneId
Then response 400 with error "Zone not found in household"
```

---

## Test Strategy

### Unit Tests
- `RoutineService.create()` — validation, defaults
- `RoutineService.update()` — partial update
- `RoutineService.delete()` — soft delete status change
- RecurrenceRule JSON serialization/deserialization

### Integration Tests
- `RoutineControllerIntegrationTest`:
  - `createRoutine_asMember_succeeds`
  - `createRoutine_notMember_returns403`
  - `listRoutines_filtersDeletedByDefault`
  - `updateRoutine_partialUpdate_works`
  - `deleteRoutine_softDeletes`
  - `createRoutine_invalidZone_returns400`
  - `createRoutine_missingTitle_returns400`

---

## Points
**5 points**

## Dependencies
- None (foundation story)

## Flags
- contract_impact: yes
- adr_needed: no
- diagrams_needed: no
- security_sensitive: yes
