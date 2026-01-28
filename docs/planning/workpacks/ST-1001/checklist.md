# DoD Checklist: ST-1001 — Routine Entity + CRUD Endpoints

## Code Quality
- [ ] Code follows project conventions (Java 21, Spring Boot idioms)
- [ ] Spotless formatting applied (`./gradlew spotlessApply`)
- [ ] No compiler warnings introduced
- [ ] Package structure: `com.hometusk.routines.*`

## Tests
- [ ] Unit tests pass
  - [ ] RoutineServiceTest: create, update, delete, list, validation
  - [ ] RecurrenceRule JSON serialization/deserialization
- [ ] Integration tests pass
  - [ ] `createRoutine_asMember_succeeds` (AC-1)
  - [ ] `listRoutines_filtersDeletedByDefault` (AC-2)
  - [ ] `getRoutine_returnsFullObject` (AC-3)
  - [ ] `updateRoutine_partialUpdate_works` (AC-4)
  - [ ] `deleteRoutine_softDeletes` (AC-5)
  - [ ] `createRoutine_missingTitle_returns400` (AC-7)
  - [ ] `createRoutine_missingRecurrenceRule_returns400` (AC-8)
  - [ ] `createRoutine_notMember_returns403` (AC-9)
  - [ ] `createRoutine_invalidZone_returns400` (AC-10)
- [ ] All tests pass: `./gradlew test`
- [ ] Existing tests still pass (Task entity extension)

## Database Migration
- [ ] V021 migration runs successfully
- [ ] `routines` table created with all columns
- [ ] `tasks.routine_id` and `tasks.scheduled_date` columns added
- [ ] CHECK constraint: `(routine_id IS NULL) = (scheduled_date IS NULL)`
- [ ] Partial unique index: `idx_task_routine_scheduled_date`
- [ ] Indexes created for queries

## Entity / Domain
- [ ] `Routine` entity maps to DB correctly
- [ ] `RoutineStatus` enum: ACTIVE, PAUSED, DELETED
- [ ] `AssignmentPolicy` enum: FIXED, ROUND_ROBIN, MANUAL
- [ ] `RecurrenceRule` sealed interface with 4 variants
- [ ] JSON polymorphic serialization works for RecurrenceRule
- [ ] `RoundRobinState` record defined
- [ ] `Task` entity extended with routineId, scheduledDate

## Contract Compliance
- [ ] Endpoints match OpenAPI: `docs/contracts/http/routines.openapi.yaml`
  - [ ] `GET /households/{id}/routines` -> 200 array
  - [ ] `POST /households/{id}/routines` -> 201 RoutineDto
  - [ ] `GET /households/{id}/routines/{routineId}` -> 200 RoutineDto
  - [ ] `PATCH /households/{id}/routines/{routineId}` -> 200 RoutineDto
  - [ ] `DELETE /households/{id}/routines/{routineId}` -> 204
- [ ] Response DTOs match schemas
- [ ] Error codes: 400, 401, 403, 404

## Security
- [ ] `membershipService.requireMembership()` called in all endpoints
- [ ] No cross-household data in queries
- [ ] No PII in logs (only UUIDs)
- [ ] IDOR not possible (403 on invalid household)
- [ ] Zone validation: zone must belong to household
- [ ] fixedAssignee validation: must be household member

## Validation
- [ ] Title required (NotBlank)
- [ ] RecurrenceRule required (NotNull)
- [ ] WEEKLY requires daysOfWeek (non-empty)
- [ ] MONTHLY dayOfMonth between 1-31
- [ ] EVERY_N_DAYS interval between 2-365
- [ ] FIXED policy requires fixedAssigneeId
- [ ] fixedAssignee must be household member

## Acceptance Criteria Verification
- [ ] AC-1: Create routine -> 201, UUID id, status=ACTIVE
- [ ] AC-2: List excludes DELETED by default
- [ ] AC-3: Get returns full object with all fields
- [ ] AC-4: PATCH updates only provided fields, updatedAt changes
- [ ] AC-5: DELETE sets status=DELETED (soft delete)
- [ ] AC-6: Task entity has routineId, scheduledDate with constraints
- [ ] AC-7: Missing title -> 400
- [ ] AC-8: Missing recurrenceRule -> 400
- [ ] AC-9: Non-member -> 403
- [ ] AC-10: Invalid zone -> 400

## Build
- [ ] `./gradlew build` passes
- [ ] `./gradlew spotlessCheck` passes
- [ ] All tests pass in CI

## Final Sign-off
- [ ] PR reviewed
- [ ] All checklist items complete
- [ ] Ready to merge
