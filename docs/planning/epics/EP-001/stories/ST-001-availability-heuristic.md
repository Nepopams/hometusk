# Story: Implement Availability-Based Assignee Selection

**ID:** ST-001
**Epic:** EP-001 (MVP Closure)
**Sprint:** S01
**Points:** 3
**Status:** ready

---

## Title

Implement simple availability-based assignee selection heuristic

---

## Description

As a household member submitting a task without specifying an assignee, I want the system to automatically assign the task to the household member with the fewest open tasks, so that workload is distributed fairly.

**Context:**
Currently, when no `assigneeId` is provided in `create_task` command, the system defaults to the command initiator. This story adds a simple heuristic: query open tasks per household member and assign to the one with the minimum count.

**User Value:**
Fair workload distribution without manual coordination.

---

## Acceptance Criteria

### AC1: Happy Path - Auto-assign to least loaded member
```
Given household has members: Alice (2 open tasks), Bob (5 open tasks), Carol (1 open task)
When user submits create_task command without assigneeId
Then task is assigned to Carol (member with fewest open tasks)
And DecisionLog records the assignment decision with reason "availability_heuristic"
```

### AC2: Tie-breaker - Multiple members with same count
```
Given household has members: Alice (2 open tasks), Bob (2 open tasks)
When user submits create_task command without assigneeId
Then task is assigned to one of them deterministically (e.g., alphabetical by name, or by member join date)
And assignment is consistent across retries
```

### AC3: Single member household
```
Given household has only one member (the initiator)
When user submits create_task command without assigneeId
Then task is assigned to the initiator
```

### AC4: Explicit assignee still works
```
Given user provides explicit assigneeId in create_task command
When command is processed
Then task is assigned to the specified assignee (heuristic not applied)
```

### AC5: Backward compatibility
```
Given existing integration tests for create_task
When tests are run after this change
Then all existing tests pass
```

---

## Test Strategy

### Unit Tests
- `DecisionEngineTest`: Test heuristic logic with mocked task counts
- Edge cases: empty household, single member, tie-breaker scenarios

### Integration Tests
- `CommandPipelineTest`: Add test for auto-assignment behavior
- Verify DecisionLog contains availability reason

### Test Data
- Household with 3+ members
- Varying open task counts per member

---

## Technical Notes

**Files to modify:**
- `services/backend/src/main/java/com/hometusk/commands/pipeline/DecisionEngine.java`
- `services/backend/src/main/java/com/hometusk/tasks/repository/TaskRepository.java` (add query if needed)

**Approach:**
1. Add method to TaskRepository: `countOpenTasksByAssigneeAndHousehold(householdId)`
2. In DecisionEngine.decideCreateTask(), when assigneeId is null:
   - Query open task counts for all household members
   - Select member with minimum count
   - Use tie-breaker (e.g., earliest joinedAt) if counts equal

**No contract changes required** - response format unchanged.

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | No API changes |
| adr_needed | no | Simple heuristic, not architectural |
| diagrams_needed | no | No structural changes |
| security_sensitive | no | No auth/authz changes |
| traceability_critical | yes | Must log decision reason |

---

## Definition of Ready Checklist

- [x] Title is clear and user-centric
- [x] Description includes context and user value
- [x] Acceptance criteria are testable (Given/When/Then)
- [x] Happy path and edge cases specified
- [x] Test strategy defined
- [x] Flags assessed
- [x] No blocking dependencies
