# Workpack: ST-1008 — Security Boundaries + Integration Tests

## Sources of Truth
- Story: `docs/planning/epics/EP-010/stories/ST-1008-security-boundaries.md`
- Epic: `docs/planning/epics/EP-010/epic.md`
- Contract: `docs/contracts/http/routines.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Goal
Implement and verify security boundaries for routines feature:
- Add missing endpoints (pause, resume, upcoming) with membership checks
- Integration tests proving 403 for non-members on all endpoints
- Integration tests proving no cross-household data leaks

## Scope: In / Out

### In Scope
- Add 3 missing controller endpoints: pause, resume, upcoming
- Membership enforcement on all 8 routine endpoints
- Integration tests for AC-1 through AC-9
- 403 response for non-members
- 404 response for routine ID in wrong household (AC-9 - no existence leak)

### Out of Scope
- Scheduler tests (AC-10, AC-11) - deferred to ST-1003 when scheduler exists
- Role-based permissions (admin vs member)
- Audit logging
- Routine-level sharing

---

## Anchors (non-negotiables)
| Artifact | Path |
|----------|------|
| Story Spec | `docs/planning/epics/EP-010/stories/ST-1008-security-boundaries.md` |
| Contract | `docs/contracts/http/routines.openapi.yaml` |
| Existing Controller | `services/backend/src/main/java/com/hometusk/routines/api/RoutineController.java` |
| Integration Test Base | `services/backend/src/test/java/com/hometusk/integration/IntegrationTestBase.java` |

---

## Plan Steps

### Step 1: Add pause endpoint to RoutineController
**Description:** Add `POST /households/{householdId}/routines/{routineId}/pause` with membership check.

**Expected Result:**
- Returns 200 with updated RoutineDto
- Idempotent: pausing already-paused routine returns 200
- Returns 400 if routine is DELETED
- Returns 403 if not member

**Files touched:**
- MODIFY: `services/backend/src/main/java/com/hometusk/routines/api/RoutineController.java`

### Step 2: Add resume endpoint to RoutineController
**Description:** Add `POST /households/{householdId}/routines/{routineId}/resume` with membership check.

**Expected Result:**
- Returns 200 with updated RoutineDto
- Idempotent: resuming already-active routine returns 200
- Returns 400 if routine is DELETED
- Returns 403 if not member

**Files touched:**
- MODIFY: `services/backend/src/main/java/com/hometusk/routines/api/RoutineController.java`

### Step 3: Add pause/resume methods to RoutineService
**Description:** Implement `pauseRoutine` and `resumeRoutine` business logic.

**Expected Result:**
- `pauseRoutine`: sets status=PAUSED, sets pausedAt timestamp
- `resumeRoutine`: sets status=ACTIVE, clears pausedAt
- Both throw BusinessException if routine is DELETED

**Files touched:**
- MODIFY: `services/backend/src/main/java/com/hometusk/routines/service/RoutineService.java`
- MODIFY: `services/backend/src/main/java/com/hometusk/routines/domain/Routine.java` (add pausedAt field if missing)

### Step 4: Add upcoming endpoint to RoutineController
**Description:** Add `GET /households/{householdId}/routines/{routineId}/upcoming` with membership check.

**Expected Result:**
- Returns list of upcoming dates using RecurrenceRuleParser
- Uses configurable count (default 5, query param)
- Returns 403 if not member

**Files touched:**
- MODIFY: `services/backend/src/main/java/com/hometusk/routines/api/RoutineController.java`
- CREATE: `services/backend/src/main/java/com/hometusk/routines/dto/UpcomingInstanceDto.java`

### Step 5: Add upcoming method to RoutineService
**Description:** Implement `getUpcomingInstances` using RecurrenceRuleParser.

**Expected Result:**
- Uses RecurrenceRuleParser.getOccurrencesInRange with fromDate=today
- Returns list of UpcomingInstanceDto with date and projected assignee

**Files touched:**
- MODIFY: `services/backend/src/main/java/com/hometusk/routines/service/RoutineService.java`

### Step 6: Create RoutineSecurityIntegrationTest
**Description:** Integration tests for all security scenarios AC-1 through AC-9.

**Expected Result:**
- All 9 ACs covered with tests
- Tests use separate households and users
- Proper setup/teardown

**Files touched:**
- CREATE: `services/backend/src/test/java/com/hometusk/integration/RoutineSecurityIntegrationTest.java`

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `RoutineController.java` | MODIFY | Add pause, resume, upcoming endpoints |
| `RoutineService.java` | MODIFY | Add pause, resume, upcoming business logic |
| `Routine.java` | MODIFY | Add pausedAt field if missing |
| `UpcomingInstanceDto.java` | CREATE | DTO for upcoming endpoint response |
| `RoutineSecurityIntegrationTest.java` | CREATE | Security integration tests |

---

## Tests & Checks

### Required Test Methods in RoutineSecurityIntegrationTest
| Test Method | AC | Description |
|-------------|-----|-------------|
| `listRoutines_notMember_returns403` | AC-1 | Non-member GET /routines → 403 |
| `createRoutine_notMember_returns403` | AC-2 | Non-member POST /routines → 403 |
| `getRoutine_notMember_returns403` | AC-3 | Non-member GET /routines/{id} → 403 |
| `updateRoutine_notMember_returns403` | AC-4 | Non-member PATCH /routines/{id} → 403 |
| `deleteRoutine_notMember_returns403` | AC-5 | Non-member DELETE /routines/{id} → 403 |
| `pauseRoutine_notMember_returns403` | AC-6 | Non-member POST /routines/{id}/pause → 403 |
| `resumeRoutine_notMember_returns403` | AC-6 | Non-member POST /routines/{id}/resume → 403 |
| `upcoming_notMember_returns403` | AC-7 | Non-member GET /routines/{id}/upcoming → 403 |
| `listRoutines_crossHousehold_noLeaks` | AC-8 | Only own household routines returned |
| `getRoutine_wrongHousehold_returns404` | AC-9 | Routine ID in wrong household → 404 (not 403) |

### Commands to Run
```bash
cd services/backend && ./gradlew test --tests "com.hometusk.integration.RoutineSecurityIntegrationTest"
cd services/backend && ./gradlew test --tests "com.hometusk.integration.RoutineControllerIntegrationTest"
cd services/backend && ./gradlew spotlessApply
cd services/backend && ./gradlew build
```

---

## Contract Impact
Endpoints already defined in `routines.openapi.yaml` - implementation only.

---

## Docs Updates
None required - contract already exists.

---

## Rollout / Rollback

### Rollout
- No feature flags needed
- No database migration (pausedAt field may need adding)
- Backward compatible - new endpoints only

### Rollback Steps
- Revert controller changes
- Revert service changes
- Delete new test file

---

## Done Criteria

| AC | Criteria | Verification |
|----|----------|--------------|
| AC-1 | List routines 403 for non-member | Integration test |
| AC-2 | Create routine 403 for non-member | Integration test |
| AC-3 | Get routine 403 for non-member | Integration test |
| AC-4 | Update routine 403 for non-member | Integration test |
| AC-5 | Delete routine 403 for non-member | Integration test |
| AC-6 | Pause/resume 403 for non-member | Integration test |
| AC-7 | Upcoming 403 for non-member | Integration test |
| AC-8 | No cross-household listing | Integration test |
| AC-9 | Wrong household → 404 | Integration test |
| AC-10 | Scheduler isolation | Deferred to ST-1003 |
| AC-11 | Generated tasks inherit boundary | Deferred to ST-1003 |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| pausedAt field missing | Pause won't persist timestamp | Check entity, add if needed |
| RecurrenceRuleParser throws on invalid rule | upcoming endpoint fails | Handle exception, return empty list |
| Test isolation | Flaky tests | Use @Transactional, fresh data per test |

---

## Prompt Pack
- PLAN: `docs/planning/workpacks/ST-1008/prompt-plan.md`
- APPLY: `docs/planning/workpacks/ST-1008/prompt-apply.md`
