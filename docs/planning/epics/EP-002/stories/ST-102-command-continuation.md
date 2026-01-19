# Story: Implement Command Continuation Endpoint

**ID:** ST-102
**Epic:** EP-002 (MVP Iteration 2)
**Iteration:** 2a
**Points:** 3
**Status:** ready
**Priority:** P0 (CRITICAL)

---

## Title

Implement `POST /commands/{commandId}/continue` to complete NEEDS_INPUT commands

---

## Description

As a user who received a clarification question from the system, I want to provide my answer so that my command completes successfully without starting over.

**Context:**
Currently, commands can return `status: needs_input` with a question. But there's no endpoint to provide the answer and continue. The command "sticks" in NEEDS_INPUT status forever.

Per ADR-004 and ADR-005, the clarification loop is a core part of the AI-driven command flow.

**User Value:**
Complete command lifecycle. Users can answer questions and get results.

---

## Acceptance Criteria

### AC1: Endpoint exists
```
Given a command in NEEDS_INPUT status
When I call POST /api/v1/commands/{commandId}/continue
With body { "additionalInput": { "zoneId": "..." } }
Then the command is processed with the additional input
And response has status "executed" or appropriate status
```

### AC2: Command status transitions
```
Given a command with status=NEEDS_INPUT
When continuation is processed successfully
Then command status becomes EXECUTED
And DecisionLog is updated
```

### AC3: Invalid command state rejected
```
Given a command with status=EXECUTED (not NEEDS_INPUT)
When I call POST /commands/{id}/continue
Then response is 400 or 409 with error "COMMAND_NOT_CONTINUABLE"
```

### AC4: Command not found
```
Given a non-existent commandId
When I call POST /commands/{id}/continue
Then response is 404
```

### AC5: Authorization enforced
```
Given a command belonging to user A
When user B calls POST /commands/{id}/continue
Then response is 403 ACCESS_DENIED
```

### AC6: OpenAPI updated
```
Given the implementation is complete
When I check docs/contracts/http/commands.openapi.yaml
Then POST /commands/{commandId}/continue is documented
With request/response schemas
```

---

## Test Strategy

### Unit Tests
- `CommandServiceTest`: Test continuation logic
- Test state validation (only NEEDS_INPUT can be continued)

### Integration Tests
- `CommandContinuationIntegrationTest`: Full flow
  - Create command → NEEDS_INPUT → continue → EXECUTED
  - Test invalid state, not found, auth errors

### Test Data
- Use guardrails that trigger NEEDS_INPUT (e.g., MaxOpenTasksPerAssigneePolicy)

---

## Technical Notes

**Files to modify/create:**

| File | Action |
|------|--------|
| `CommandController.java` | Add `continueCommand()` method |
| `CommandService.java` | Add continuation logic |
| `ContinueCommandRequest.java` | New DTO |
| `commands.openapi.yaml` | Add endpoint spec |
| `CommandContinuationIntegrationTest.java` | New test |

**Implementation approach:**

```java
@PostMapping("/{commandId}/continue")
public ResponseEntity<Object> continueCommand(
    @PathVariable UUID commandId,
    @RequestBody ContinueCommandRequest request) {

    Command command = commandRepository.findById(commandId)
        .orElseThrow(() -> new NotFoundException(COMMAND_NOT_FOUND));

    // Verify ownership
    if (!command.getInitiator().getId().equals(currentUser.getId())) {
        throw new AccessDeniedException();
    }

    // Verify state
    if (command.getStatus() != CommandStatus.NEEDS_INPUT) {
        throw new BusinessException(COMMAND_NOT_CONTINUABLE);
    }

    // Merge additional input with original payload
    // Re-run decision pipeline
    // ...
}
```

**Idempotency consideration (per ADR-012):**
- Continuation uses commandId, not new Idempotency-Key
- Original command's idempotency key protects the initial request
- Continuation is a new action, could have its own idempotency

---

## Related Artifacts

| Artifact | Path | Relevance |
|----------|------|-----------|
| ADR-004 | `docs/architecture/decisions/004-stage2-ai-platform-integration.md` | Clarify flow |
| ADR-005 | `docs/architecture/decisions/005-stage3-guardrails-pipeline.md` | Guardrails clarification |
| ADR-012 | `docs/architecture/decisions/012-command-reliability-idempotency.md` | Idempotency handling |
| Commands Contract | `docs/contracts/http/commands.openapi.yaml` | To update |
| CommandService | `services/backend/src/main/java/.../CommandService.java` | Core logic |

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | **yes** | New endpoint in OpenAPI |
| adr_needed | no | Follows existing ADRs |
| diagrams_needed | no | No structural change |
| traceability_critical | yes | DecisionLog must be updated |

---

## Definition of Ready Checklist

- [x] Title clear and user-centric
- [x] AC testable (Given/When/Then)
- [x] Happy path and edge cases covered
- [x] Test strategy defined
- [x] Related artifacts identified
- [x] Flags assessed
- [x] Depends on: ST-101 (JDK setup)
