# Story: Implement start_task Command

**ID:** ST-104
**Epic:** EP-002 (MVP Iteration 2)
**Iteration:** 2b
**Points:** 2
**Status:** blocked
**Blocked By:** ST-103 (decision must be "implement")
**Priority:** P2 (conditional)

---

## Title

Implement `start_task` command to transition task to IN_PROGRESS

---

## Description

As a user, I want to mark a task as "in progress" so that my household knows I'm working on it.

**Context:**
This story is **conditional** on ST-103 decision. Only implement if product decides start_task is MVP-required.

---

## Acceptance Criteria

### AC1: Command type exists
```
Given CommandType enum
When I check values
Then START_TASK is present
```

### AC2: Happy path
```
Given a task in OPEN status
When I send POST /commands with type=start_task, payload={taskId}
Then response status is "executed"
And task status becomes IN_PROGRESS
And DecisionLog is created
```

### AC3: Already in progress
```
Given a task in IN_PROGRESS status
When I send start_task command
Then response status is "rejected"
With errorCode TASK_ALREADY_IN_PROGRESS
```

### AC4: Already done
```
Given a task in DONE status
When I send start_task command
Then response status is "rejected"
With errorCode TASK_ALREADY_COMPLETED
```

### AC5: Task not found
```
Given a non-existent taskId
When I send start_task command
Then response is error with TASK_NOT_FOUND
```

### AC6: OpenAPI updated
```
Given implementation complete
When I check commands.openapi.yaml
Then start_task is documented as command type
And StartTaskPayload schema exists
```

---

## Test Strategy

### Unit Tests
- `DecisionEngineTest`: start_task decision
- `BusinessValidatorTest`: state validation

### Integration Tests
- Add to `CommandPipelineTest`:
  - start_task happy path
  - start_task invalid states

---

## Technical Notes

**Files to modify/create:**

| File | Action |
|------|--------|
| `CommandType.java` | Add START_TASK |
| `StartTaskPayload.java` | New DTO |
| `start-task.schema.json` | New JSON Schema |
| `SchemaValidator.java` | Add schema mapping |
| `BusinessValidator.java` | Add validateStartTask() |
| `DecisionEngine.java` | Add decideStartTask() |
| `ActionExecutor.java` | Add executeStartTask() |
| `CommandService.java` | Add case for START_TASK |
| `commands.openapi.yaml` | Update |
| `ManualDecisionProvider.java` | Add START_TASK handling |

**Similar to complete_task implementation.**

---

## Related Artifacts

| Artifact | Path |
|----------|------|
| complete_task impl | Reference pattern |
| Commands Contract | `docs/contracts/http/commands.openapi.yaml` |
| Task domain | `services/backend/src/main/java/.../tasks/domain/Task.java` |

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | **yes** | New command type |
| adr_needed | no | Follows existing pattern |
| diagrams_needed | no | — |

---

## Definition of Ready Checklist

- [x] Title clear
- [x] AC testable
- [x] Test strategy defined
- [ ] **BLOCKED:** Waiting for ST-103 decision
