# Checklist: ST-1008 — Security Boundaries + Integration Tests

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-1008/workpack.md`
- Story: `docs/planning/epics/EP-010/stories/ST-1008-security-boundaries.md`
- Contract: `docs/contracts/http/routines.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Definition of Ready (DoR)

- [x] Story has clear title and description
- [x] Acceptance criteria defined (Given/When/Then) — 11 ACs (9 in scope, 2 deferred)
- [x] In scope / out of scope explicit
- [x] Technical approach identified (membership enforcement + integration tests)
- [x] Dependencies identified (ST-1001 RoutineEntity, ST-1002 RecurrenceRuleParser)
- [x] Files to change listed (5 files)
- [x] Verification commands defined
- [x] Test strategy defined (integration tests for security boundaries)

**DoR Status: READY**

---

## Definition of Done (DoD)

### Code Quality
- [ ] Pause endpoint added to RoutineController
- [ ] Resume endpoint added to RoutineController
- [ ] Upcoming endpoint added to RoutineController
- [ ] pauseRoutine method added to RoutineService
- [ ] resumeRoutine method added to RoutineService
- [ ] getUpcomingInstances method added to RoutineService
- [ ] UpcomingInstanceDto created
- [ ] All endpoints have membership enforcement
- [ ] No compiler warnings introduced
- [ ] Spotless formatting applied: `./gradlew spotlessApply`

### Functionality
- [ ] Pause returns 200 with updated RoutineDto
- [ ] Pause is idempotent (pausing already-paused returns 200)
- [ ] Pause returns 400 if routine is DELETED
- [ ] Resume returns 200 with updated RoutineDto
- [ ] Resume is idempotent (resuming already-active returns 200)
- [ ] Resume returns 400 if routine is DELETED
- [ ] Upcoming returns list of dates using RecurrenceRuleParser
- [ ] All endpoints return 403 for non-members
- [ ] Wrong household routine ID returns 404 (not 403)

### Tests
- [ ] Integration test class `RoutineSecurityIntegrationTest` created
- [ ] Test: `listRoutines_notMember_returns403` (AC-1)
- [ ] Test: `createRoutine_notMember_returns403` (AC-2)
- [ ] Test: `getRoutine_notMember_returns403` (AC-3)
- [ ] Test: `updateRoutine_notMember_returns403` (AC-4)
- [ ] Test: `deleteRoutine_notMember_returns403` (AC-5)
- [ ] Test: `pauseRoutine_notMember_returns403` (AC-6)
- [ ] Test: `resumeRoutine_notMember_returns403` (AC-6)
- [ ] Test: `upcoming_notMember_returns403` (AC-7)
- [ ] Test: `listRoutines_crossHousehold_noLeaks` (AC-8)
- [ ] Test: `getRoutine_wrongHousehold_returns404` (AC-9)
- [ ] All tests pass: `./gradlew test`

### Documentation
- [ ] N/A — Contract already exists in routines.openapi.yaml

### Security
- [ ] Membership enforcement verified on all 8 endpoints
- [ ] No cross-household data leaks verified by tests
- [ ] Wrong household returns 404 (no existence leak)

---

## Acceptance Criteria Verification

| AC | Description | Status | Evidence |
|----|-------------|--------|----------|
| AC-1 | List routines 403 for non-member | [ ] | Integration test |
| AC-2 | Create routine 403 for non-member | [ ] | Integration test |
| AC-3 | Get routine 403 for non-member | [ ] | Integration test |
| AC-4 | Update routine 403 for non-member | [ ] | Integration test |
| AC-5 | Delete routine 403 for non-member | [ ] | Integration test |
| AC-6 | Pause/resume 403 for non-member | [ ] | Integration test |
| AC-7 | Upcoming 403 for non-member | [ ] | Integration test |
| AC-8 | No cross-household listing | [ ] | Integration test |
| AC-9 | Wrong household → 404 | [ ] | Integration test |
| AC-10 | Scheduler isolation | DEFERRED | ST-1003 |
| AC-11 | Generated tasks inherit boundary | DEFERRED | ST-1003 |

---

## Verification Commands

```bash
# Run security integration tests
cd services/backend && ./gradlew test --tests "com.hometusk.integration.RoutineSecurityIntegrationTest"

# Run all routine controller tests
cd services/backend && ./gradlew test --tests "com.hometusk.integration.RoutineControllerIntegrationTest"

# Check formatting
cd services/backend && ./gradlew spotlessCheck

# Apply formatting
cd services/backend && ./gradlew spotlessApply

# Full build
cd services/backend && ./gradlew build
```

---

## Files Created/Modified

| File | Status |
|------|--------|
| `services/backend/src/main/java/com/hometusk/routines/api/RoutineController.java` | [ ] Modified |
| `services/backend/src/main/java/com/hometusk/routines/service/RoutineService.java` | [ ] Modified |
| `services/backend/src/main/java/com/hometusk/routines/domain/Routine.java` | [ ] Modified (if pausedAt missing) |
| `services/backend/src/main/java/com/hometusk/routines/dto/UpcomingInstanceDto.java` | [ ] Created |
| `services/backend/src/test/java/com/hometusk/integration/RoutineSecurityIntegrationTest.java` | [ ] Created |

---

## Scope Compliance (Anti-Scope-Creep)

- [ ] NO scheduler integration added (ST-1003)
- [ ] NO role-based permissions added
- [ ] NO audit logging added
- [ ] NO routine-level sharing added
- [ ] Only membership enforcement, no complex authorization

---

## Rollback Checklist

If rollback needed:
- [ ] Revert RoutineController changes (remove pause/resume/upcoming)
- [ ] Revert RoutineService changes
- [ ] Revert Routine.java changes (if pausedAt was added)
- [ ] Delete UpcomingInstanceDto.java
- [ ] Delete RoutineSecurityIntegrationTest.java
- [ ] Verify build still passes: `./gradlew build`
