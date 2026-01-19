# Work Package: ST-001 — Availability-Based Assignee Selection

**Story:** ST-001
**Epic:** EP-001 (MVP Closure)
**Sprint:** S01
**Target:** Codex implementation

---

## Summary

Implement simple availability heuristic: when no assigneeId is provided in create_task command, assign to household member with fewest open tasks.

---

## Anchors (Source of Truth)

| Anchor | Path | Relevance |
|--------|------|-----------|
| MVP Scope | `docs/planning/mvp.md` | Line 12: "Automatic assignee selection based on availability" |
| Story Spec | `docs/planning/epics/EP-001/stories/ST-001-availability-heuristic.md` | Full AC and test strategy |
| DecisionEngine | `services/backend/src/main/java/com/hometusk/commands/pipeline/DecisionEngine.java` | File to modify |
| TaskRepository | `services/backend/src/main/java/com/hometusk/tasks/repository/TaskRepository.java` | May need new query |

---

## Implementation Steps

### Step 1: Add query to TaskRepository

**File:** `services/backend/src/main/java/com/hometusk/tasks/repository/TaskRepository.java`

**Action:** Add method to count open tasks by assignee within household.

```java
@Query("SELECT t.assignee.id, COUNT(t) FROM Task t " +
       "WHERE t.household.id = :householdId " +
       "AND t.status IN ('OPEN', 'IN_PROGRESS') " +
       "GROUP BY t.assignee.id")
List<Object[]> countOpenTasksByAssigneeInHousehold(@Param("householdId") UUID householdId);
```

**Verification:** Query compiles without errors.

---

### Step 2: Add service method for task counts

**File:** `services/backend/src/main/java/com/hometusk/tasks/service/TaskService.java`

**Action:** Add method that returns Map<UUID, Long> of assigneeId -> openTaskCount.

```java
public Map<UUID, Long> getOpenTaskCountsByAssignee(UUID householdId) {
    return taskRepository.countOpenTasksByAssigneeInHousehold(householdId)
        .stream()
        .collect(Collectors.toMap(
            row -> (UUID) row[0],
            row -> (Long) row[1]
        ));
}
```

**Verification:** Method returns correct counts in unit test.

---

### Step 3: Inject TaskService into DecisionEngine

**File:** `services/backend/src/main/java/com/hometusk/commands/pipeline/DecisionEngine.java`

**Action:** Add constructor parameter for TaskService.

```java
private final TaskService taskService;
private final MembershipService membershipService;

public DecisionEngine(TaskService taskService, MembershipService membershipService) {
    this.taskService = taskService;
    this.membershipService = membershipService;
}
```

**Verification:** Application context loads without circular dependency errors.

---

### Step 4: Implement availability heuristic in DecisionEngine

**File:** `services/backend/src/main/java/com/hometusk/commands/pipeline/DecisionEngine.java`

**Method:** `decideCreateTask`

**Action:** Replace simple "default to initiator" logic with availability-based selection.

```java
public CreateTaskDecision decideCreateTask(CreateTaskPayload payload, UUID initiatorId, UUID householdId) {
    UUID assigneeId = payload.assigneeId();

    // If explicit assignee provided, use it
    if (assigneeId != null) {
        return buildDecision(payload, assigneeId, DecisionSource.RULE, "explicit_assignee");
    }

    // Get household members
    List<UUID> memberIds = membershipService.getMemberIds(householdId);

    if (memberIds.size() == 1) {
        // Single member household - assign to initiator
        return buildDecision(payload, initiatorId, DecisionSource.RULE, "single_member");
    }

    // Get open task counts per member
    Map<UUID, Long> taskCounts = taskService.getOpenTaskCountsByAssignee(householdId);

    // Find member with minimum open tasks
    // Tie-breaker: first in memberIds list (stable ordering by join date)
    UUID selectedAssignee = memberIds.stream()
        .min(Comparator.comparingLong(id -> taskCounts.getOrDefault(id, 0L)))
        .orElse(initiatorId);

    log.debug("Availability heuristic: selected {} with {} open tasks",
        selectedAssignee, taskCounts.getOrDefault(selectedAssignee, 0L));

    return buildDecision(payload, selectedAssignee, DecisionSource.RULE, "availability_heuristic");
}
```

**Note:** Method signature changes to include `householdId`. Update caller accordingly.

**Verification:** Unit test with mocked task counts selects correct member.

---

### Step 5: Update DecisionEngine caller (if needed)

**File:** `services/backend/src/main/java/com/hometusk/commands/service/CommandService.java` or wherever DecisionEngine is called.

**Action:** Pass householdId to decideCreateTask method.

**Verification:** Integration test passes.

---

### Step 6: Add unit tests

**File:** `services/backend/src/test/java/com/hometusk/commands/pipeline/DecisionEngineTest.java`

**Tests to add:**
1. `whenNoAssignee_selectsMemberWithFewestOpenTasks`
2. `whenTie_selectsFirstMemberInList`
3. `whenSingleMember_selectsInitiator`
4. `whenExplicitAssignee_usesProvidedAssignee`

**Verification:** All unit tests pass.

---

### Step 7: Add integration test

**File:** `services/backend/src/test/java/com/hometusk/integration/CommandPipelineTest.java`

**Test to add:**
```java
@Test
void createTask_withoutAssignee_assignsToLeastLoadedMember() {
    // Setup: Create household with 2 members
    // Create 3 tasks assigned to member A
    // Submit create_task without assigneeId
    // Assert: Task assigned to member B (fewer tasks)
}
```

**Verification:** Integration test passes.

---

### Step 8: Run full test suite

**Command:** `./scripts/test.sh`

**Verification:** All existing tests pass (no regression).

---

## Files Modified/Created

| File | Action |
|------|--------|
| `TaskRepository.java` | Add query method |
| `TaskService.java` | Add getOpenTaskCountsByAssignee method |
| `DecisionEngine.java` | Add availability heuristic logic |
| `CommandService.java` | Pass householdId to DecisionEngine (if needed) |
| `DecisionEngineTest.java` | Add unit tests |
| `CommandPipelineTest.java` | Add integration test |

---

## Forbidden Changes

- DO NOT modify API contract (commands.openapi.yaml)
- DO NOT change response format
- DO NOT add new endpoints
- DO NOT modify existing test assertions (unless fixing actual bugs)

---

## Invariants to Preserve

1. Existing create_task with explicit assigneeId must work unchanged
2. DecisionLog must be created for every command
3. All existing integration tests must pass
4. No circular dependencies introduced

---

## Test Commands

```bash
# Run unit tests
./gradlew :services:backend:test --tests "*DecisionEngineTest*"

# Run integration tests
./gradlew :services:backend:test --tests "*CommandPipelineTest*"

# Run all tests
./scripts/test.sh
```

**Expected outcome:** All tests pass.

---

## Rollback

If this change causes issues:
1. Revert DecisionEngine to previous "default to initiator" logic
2. Remove new query and service method
3. No data migration needed (no schema changes)

---

## Done Criteria

- [ ] Availability heuristic implemented
- [ ] Unit tests added and passing
- [ ] Integration test added and passing
- [ ] All existing tests pass
- [ ] Code follows project conventions (Spotless)
- [ ] DecisionLog records "availability_heuristic" reason
