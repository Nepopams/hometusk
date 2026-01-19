# Workpack: ST-102 — Implement Command Continuation Endpoint

**Story:** [ST-102-command-continuation.md](../../epics/EP-002/stories/ST-102-command-continuation.md)
**Type:** Implementation (code changes)
**Priority:** P0 (CRITICAL)
**Depends on:** ST-101 (JDK setup)

---

## Sources of Truth

| Artifact | Path | Relevance |
|----------|------|-----------|
| Story spec | `docs/planning/epics/EP-002/stories/ST-102-command-continuation.md` | AC definition |
| ADR-004 | `docs/architecture/decisions/004-stage2-ai-platform-integration.md` | Clarify flow design |
| ADR-005 | `docs/architecture/decisions/005-stage3-guardrails-pipeline.md` | Guardrails clarification |
| ADR-012 | `docs/architecture/decisions/012-command-reliability-idempotency.md` | Idempotency handling |
| Commands Contract | `docs/contracts/http/commands.openapi.yaml` | API specification |
| CommandService | `services/backend/src/main/java/.../commands/service/CommandService.java` | Core pipeline |
| CommandController | `services/backend/src/main/java/.../commands/api/CommandController.java` | HTTP layer |
| CommandStatus | `services/backend/src/main/java/.../commands/domain/CommandStatus.java` | NEEDS_INPUT status |
| DecisionLog | `services/backend/src/main/java/.../commands/domain/DecisionLog.java` | Traceability |

---

## Design Summary

Per ADR-004 and ADR-005:
- Commands can return `status: needs_input` with a question
- User must be able to provide answer and continue the command
- DecisionLog must be updated when command completes

**Key design decisions:**
1. Continuation uses original `commandId` (not new Idempotency-Key)
2. Only commands in `NEEDS_INPUT` status can be continued
3. Ownership verified (initiator only)
4. Additional input merged with original payload
5. Re-run decision pipeline with additional context

---

## Files to Create

| File | Purpose |
|------|---------|
| `ContinueCommandRequest.java` | DTO for continuation request body |
| `CommandContinuationIntegrationTest.java` | Integration test for full flow |

---

## Files to Modify

| File | Changes |
|------|---------|
| `CommandController.java` | Add `POST /{commandId}/continue` endpoint |
| `CommandService.java` | Add `continueCommand()` method |
| `commands.openapi.yaml` | Document new endpoint + schemas |
| `ErrorCode.java` | Add `COMMAND_NOT_CONTINUABLE` if not exists |

---

## Implementation Plan

### Commit 1: Add DTO and error code

**Files:**
- `services/backend/src/main/java/com/hometusk/commands/dto/ContinueCommandRequest.java` (NEW)
- `services/backend/src/main/java/com/hometusk/shared/ErrorCode.java` (MODIFY)

**Steps:**
1. Create `ContinueCommandRequest` DTO:
```java
public record ContinueCommandRequest(
    @NotNull Map<String, Object> additionalInput
) {}
```

2. Add `COMMAND_NOT_CONTINUABLE` to ErrorCode enum (if not exists)

### Commit 2: Add CommandService.continueCommand()

**File:** `services/backend/src/main/java/com/hometusk/commands/service/CommandService.java`

**Logic:**
1. Find command by ID (throw 404 if not found)
2. Verify command.initiator == currentUser (throw 403 if not)
3. Verify command.status == NEEDS_INPUT (throw 400 with COMMAND_NOT_CONTINUABLE if not)
4. Merge additionalInput into original payload
5. Re-run decision pipeline (DecisionProviderSelector)
6. Apply guardrails
7. Execute action
8. Update DecisionLog
9. Return CommandResponse

**Key method signature:**
```java
public CommandResponseBase continueCommand(
    UUID commandId,
    ContinueCommandRequest request,
    User currentUser,
    UUID correlationId
);
```

### Commit 3: Add CommandController endpoint

**File:** `services/backend/src/main/java/com/hometusk/commands/api/CommandController.java`

**Endpoint:**
```java
@PostMapping("/{commandId}/continue")
public ResponseEntity<CommandResponseBase> continueCommand(
    @PathVariable UUID commandId,
    @RequestBody @Valid ContinueCommandRequest request,
    @RequestHeader(value = "X-Correlation-ID", required = false) UUID correlationId
) {
    User currentUser = getCurrentUser();
    CommandResponseBase response = commandService.continueCommand(
        commandId, request, currentUser,
        correlationId != null ? correlationId : UUID.randomUUID()
    );
    return ResponseEntity.ok(response);
}
```

### Commit 4: Update OpenAPI contract

**File:** `docs/contracts/http/commands.openapi.yaml`

**Add:**
1. New path: `/commands/{commandId}/continue`
2. New schema: `ContinueCommandRequest`
3. Response references existing `CommandResponse` schema
4. Error responses: 400 (not continuable), 403 (access denied), 404 (not found)

### Commit 5: Add integration test

**File:** `services/backend/src/test/java/com/hometusk/integration/commands/CommandContinuationIntegrationTest.java` (NEW)

**Test scenarios:**
1. Happy path: NEEDS_INPUT → continue → EXECUTED
2. Invalid state: EXECUTED command → 400 COMMAND_NOT_CONTINUABLE
3. Not found: non-existent commandId → 404
4. Auth error: different user → 403 ACCESS_DENIED
5. DecisionLog updated after continuation

---

## Verification Commands

| # | Command | Expected Result |
|---|---------|-----------------|
| 1 | `./gradlew compileJava` | Compiles without errors |
| 2 | `./gradlew test --tests "*CommandContinuationIntegrationTest*"` | All tests pass |
| 3 | `./gradlew test` | All tests pass (no regression) |
| 4 | Manual: OpenAPI linting | Schema valid |

---

## DoD Checklist

### Code Quality
- [ ] Code follows project conventions (Java 21, Spring Boot idioms)
- [ ] Spotless formatting applied
- [ ] No compiler warnings introduced

### Tests
- [ ] Integration test covers happy path
- [ ] Integration test covers invalid state (not NEEDS_INPUT)
- [ ] Integration test covers not found
- [ ] Integration test covers auth error
- [ ] All existing tests still pass

### Documentation
- [ ] OpenAPI contract updated with new endpoint
- [ ] Request/response schemas documented

### Observability
- [ ] correlationId propagated to DecisionLog
- [ ] DecisionLog updated on continuation

### Security
- [ ] Ownership verified (initiator only)
- [ ] No cross-household data leak possible

---

## Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Merge conflict in CommandService | Medium | Low | Small, focused changes |
| Pipeline re-run edge cases | Low | Medium | Comprehensive test coverage |
| Idempotency interaction | Low | Low | Per ADR-012: continuation uses commandId, not new key |

---

## Rollback

If issues found post-merge:
1. Revert the merge commit
2. No database migration required (no schema changes)
3. OpenAPI contract revert: restore previous version

---

## Contract Impact

**YES** — New endpoint in OpenAPI contract.

**Changes to `docs/contracts/http/commands.openapi.yaml`:**
- New path: `POST /commands/{commandId}/continue`
- New schema: `ContinueCommandRequest`
- Error codes documented

**Review required:** Contract changes should be reviewed before implementation per CLAUDE.md pipeline rules.

---

## Docs Updates

| File | Action |
|------|--------|
| `docs/contracts/http/commands.openapi.yaml` | Add endpoint + schemas |
| `docs/_indexes/contracts-index.md` | Verify commands contract listed |

---

## Notes

- This story completes the clarification loop per ADR-004 and ADR-005
- After this, commands with `needs_input` status will have a complete lifecycle
- Critical for MVP exit criteria #4 (Command traceability) and #6 (Idempotency)
