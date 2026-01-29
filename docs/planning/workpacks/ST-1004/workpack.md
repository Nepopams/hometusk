# Workpack: ST-1004 — Assignment Policies (Fixed/Round-Robin/Manual)

## Sources of Truth
- Story: `docs/planning/epics/EP-010/stories/ST-1004-assignment-policies.md`
- Epic: `docs/planning/epics/EP-010/epic.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- Existing Routine entity: `services/backend/src/main/java/com/hometusk/routines/domain/Routine.java`
- RoutineSchedulerService: `services/backend/src/main/java/com/hometusk/routines/service/RoutineSchedulerService.java`

---

## Goal
Implement assignment policies that scheduler uses when creating task instances:
- FIXED: always assign to specified user
- ROUND_ROBIN: rotate among household members
- MANUAL: no assignee

## Scope: In / Out

### In Scope
- Create `AssignmentPolicyService` with strategy pattern
- FIXED policy: assign to `routine.fixedAssigneeId` (already validated on routine creation)
- ROUND_ROBIN policy: rotate through household members
- RoundRobinState record for JSON serialization/deserialization
- MANUAL policy: leave assignee null (current behavior in ST-1003)
- Integration with `RoutineSchedulerService.createTaskForDate()`
- Round-robin state persistence atomically with task creation
- Concurrent access handling (pessimistic lock on routine row)

### Out of Scope
- Scheduler-level locking (SELECT FOR UPDATE SKIP LOCKED on scheduler_locks table)
  - v0: single scheduler instance, row-level lock per routine sufficient
- Availability-based assignment (calendar)
- Skill/preference-based assignment
- UI for policy selection (ST-1005)

---

## Anchors (non-negotiables)
| Artifact | Path |
|----------|------|
| Story Spec | `docs/planning/epics/EP-010/stories/ST-1004-assignment-policies.md` |
| Epic | `docs/planning/epics/EP-010/epic.md` |
| Routine Entity | `services/backend/src/main/java/com/hometusk/routines/domain/Routine.java` |
| RoutineSchedulerService | `services/backend/src/main/java/com/hometusk/routines/service/RoutineSchedulerService.java` |
| MembershipRepository | `services/backend/src/main/java/com/hometusk/users/repository/MembershipRepository.java` |

---

## Plan Steps

### Step 1: Create RoundRobinState record

**Description:** Create a record class for round-robin state serialization.

**Expected Result:**
```java
public record RoundRobinState(
    UUID lastAssignedUserId,
    List<UUID> memberOrder
) {}
```

**Files touched:**
- CREATE: `services/backend/src/main/java/com/hometusk/routines/domain/RoundRobinState.java`

### Step 2: Create AssignmentPolicyService

**Description:** Service that determines assignee based on policy.

**Expected Result:**
- `User determineAssignee(Routine routine)` method
- Strategy pattern: switch on `routine.getAssignmentPolicy()`
- FIXED: return `routine.getFixedAssignee()`
- MANUAL: return `null`
- ROUND_ROBIN: call `determineRoundRobinAssignee(routine)`

**Files touched:**
- CREATE: `services/backend/src/main/java/com/hometusk/routines/service/AssignmentPolicyService.java`

### Step 3: Implement Round-Robin Logic

**Description:** Round-robin rotation logic with state management.

**Expected Result:**
- Parse `routine.roundRobinStateJson` to `RoundRobinState`
- Get current household members via `membershipRepository.findByHousehold_IdWithUser()`
- If state is null or empty: initialize with current members, start from first
- Find next member after `lastAssignedUserId` in rotation
- Handle wrap-around (after last member -> first member)
- Handle removed members (skip if not in current member list)
- Update state with new `lastAssignedUserId`

**Files touched:**
- MODIFY: `services/backend/src/main/java/com/hometusk/routines/service/AssignmentPolicyService.java`

### Step 4: Add Pessimistic Lock for Routine

**Description:** Add `@Lock` annotation to repository method for concurrent access safety.

**Expected Result:**
- Add `findByIdForUpdate(UUID id)` method with `@Lock(LockModeType.PESSIMISTIC_WRITE)`
- This prevents concurrent scheduler instances from corrupting round-robin state

**Files touched:**
- MODIFY: `services/backend/src/main/java/com/hometusk/routines/repository/RoutineRepository.java`

### Step 5: Integrate with RoutineSchedulerService

**Description:** Call AssignmentPolicyService when creating tasks.

**Expected Result:**
- Inject `AssignmentPolicyService` into `RoutineSchedulerService`
- In `generateTasksForRoutine()`: lock routine with `findByIdForUpdate()`
- In `createTaskForDate()`: call `assignmentPolicyService.determineAssignee(routine)`
- Set `task.setAssignee(assignee)` if not null
- Save routine after round-robin state update (same transaction)

**Files touched:**
- MODIFY: `services/backend/src/main/java/com/hometusk/routines/service/RoutineSchedulerService.java`

### Step 6: Unit Tests for AssignmentPolicyService

**Description:** Unit tests with mocked dependencies.

**Expected Result:**
- `fixed_assignsToConfiguredUser`
- `manual_returnsNull`
- `roundRobin_rotatesCorrectly`
- `roundRobin_wrapsAround`
- `roundRobin_handlesMemberRemoval`
- `roundRobin_initializesEmptyState`

**Files touched:**
- CREATE: `services/backend/src/test/java/com/hometusk/routines/service/AssignmentPolicyServiceTest.java`

### Step 7: Integration Tests

**Description:** Integration tests with real DB.

**Expected Result:**
- `roundRobin_statePersistedCorrectly`
- `roundRobin_concurrentAccess_noCorruption` (optional for v0)
- `scheduler_withFixedPolicy_assignsCorrectly`
- `scheduler_withRoundRobin_rotatesMembers`
- `scheduler_withManual_noAssignee`

**Files touched:**
- MODIFY: `services/backend/src/test/java/com/hometusk/integration/RoutineSchedulerIntegrationTest.java`

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `RoundRobinState.java` | CREATE | State record for JSON serialization |
| `AssignmentPolicyService.java` | CREATE | Assignment strategy logic |
| `RoutineRepository.java` | MODIFY | Add pessimistic lock method |
| `RoutineSchedulerService.java` | MODIFY | Integrate assignment policies |
| `AssignmentPolicyServiceTest.java` | CREATE | Unit tests |
| `RoutineSchedulerIntegrationTest.java` | MODIFY | Add policy integration tests |

---

## Tests & Checks

### Required Test Methods
| Test Class | Method | AC |
|------------|--------|-----|
| AssignmentPolicyServiceTest | `fixed_assignsToConfiguredUser` | AC-1 |
| AssignmentPolicyServiceTest | `manual_returnsNull` | AC-7 |
| AssignmentPolicyServiceTest | `roundRobin_rotatesCorrectly` | AC-3 |
| AssignmentPolicyServiceTest | `roundRobin_wrapsAround` | AC-5 |
| AssignmentPolicyServiceTest | `roundRobin_handlesMemberRemoval` | AC-6 |
| AssignmentPolicyServiceTest | `roundRobin_continuesAcrossRuns` | AC-4 |
| RoutineSchedulerIntegrationTest | `scheduler_withRoundRobin_rotatesMembers` | AC-3, AC-8 |
| RoutineSchedulerIntegrationTest | `scheduler_withFixedPolicy_assignsCorrectly` | AC-1 |

### Commands to Run
```bash
cd services/backend && ./gradlew spotlessApply
cd services/backend && ./gradlew test --tests "com.hometusk.routines.service.AssignmentPolicyServiceTest"
cd services/backend && ./gradlew test --tests "com.hometusk.integration.RoutineSchedulerIntegrationTest"
cd services/backend && ./gradlew build
```

---

## Contract Impact
None — internal scheduler logic, no new API endpoints.

---

## Docs Updates
None required.

---

## Rollout / Rollback

### Rollout
- Assignment policies activated immediately (already part of routine model)
- Round-robin state persists automatically
- No feature flag needed (policies were already selectable on routine creation)

### Rollback Steps
- Revert code changes
- Round-robin state remains in DB but is ignored
- Tasks created with assignments remain (assignee preserved)

---

## Done Criteria

| AC | Criteria | Verification |
|----|----------|--------------|
| AC-1 | FIXED policy assigns to specified user | Unit + Integration test |
| AC-2 | FIXED policy validates membership | Already done in ST-1001 |
| AC-3 | ROUND_ROBIN rotates through members | Unit + Integration test |
| AC-4 | ROUND_ROBIN continues across runs | Unit test |
| AC-5 | ROUND_ROBIN wraps around | Unit test |
| AC-6 | ROUND_ROBIN handles member changes | Unit test |
| AC-7 | MANUAL policy leaves no assignee | Unit test |
| AC-8 | State persisted atomically | Integration test |
| AC-9 | Concurrent access safe | Row-level lock (code review) |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Round-robin state corruption | Wrong assignments | Pessimistic lock on routine row |
| Member removal during rotation | Infinite loop | Filter to current members only |
| Empty household | No assignee | Return null, task has no assignee |
| JSON parsing error | Scheduler fails | Try-catch, reinitialize state |

---

## Prompt Pack
- PLAN: `docs/planning/workpacks/ST-1004/prompt-plan.md`
- APPLY: `docs/planning/workpacks/ST-1004/prompt-apply.md`
