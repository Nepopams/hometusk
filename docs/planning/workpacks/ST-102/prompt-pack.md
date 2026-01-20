# ST-102 Prompt Pack — Command Continuation Endpoint

**Story:** ST-102
**Workpack:** [workpack.md](./workpack.md)
**Generated:** 2026-01-19

---

## PLAN Prompt

```markdown
# Codex PLAN Prompt — ST-102: Implement Command Continuation Endpoint

You are Codex CLI acting as an executor-analyst. TASK: produce a plan ONLY.

## HARD RULES (non-negotiable)

- DO NOT edit any files.
- DO NOT run any shell commands.
- DO NOT propose extra features outside scope.
- If you need more info, ask targeted questions instead of guessing.

## Anchor Block — Read These First

Before proceeding, you MUST read and acknowledge:
1. `AGENTS.md` (project instructions) — list which instruction files you loaded and summarize key constraints
2. `docs/planning/workpacks/ST-102/workpack.md` — the primary workpack
3. `docs/planning/epics/EP-002/stories/ST-102-command-continuation.md` — story spec with AC
4. `docs/contracts/http/commands.openapi.yaml` — current API contract (to update)
5. `docs/_governance/dod.md` — Definition of Done requirements

If instructions seem truncated or missing, say so explicitly.

## Context / Source of Truth

| Artifact | Path | Purpose |
|----------|------|---------|
| Workpack | `docs/planning/workpacks/ST-102/workpack.md` | Implementation plan |
| Story Spec | `docs/planning/epics/EP-002/stories/ST-102-command-continuation.md` | Acceptance Criteria |
| ADR-004 | `docs/architecture/decisions/004-stage2-ai-platform-integration.md` | Clarify flow design |
| ADR-005 | `docs/architecture/decisions/005-stage3-guardrails-pipeline.md` | Guardrails clarification |
| ADR-012 | `docs/architecture/decisions/012-command-reliability-idempotency.md` | Idempotency handling |
| Commands Contract | `docs/contracts/http/commands.openapi.yaml` | API spec to update |
| CommandService | `services/backend/src/main/java/com/hometusk/commands/service/CommandService.java` | Core logic |
| CommandController | `services/backend/src/main/java/com/hometusk/commands/api/CommandController.java` | HTTP layer |

## Scope

**Goal:** Implement `POST /commands/{commandId}/continue` endpoint to complete NEEDS_INPUT commands

**In-scope:**
- Create `ContinueCommandRequest` DTO
- Add `continueCommand()` method to CommandService
- Add POST endpoint to CommandController
- Update OpenAPI contract
- Write integration tests covering AC1-AC5
- Add COMMAND_NOT_CONTINUABLE error code if not exists

**Out-of-scope:**
- Changing existing command processing flow
- Modifying AI Platform integration
- Refactoring guardrails
- Any UI/frontend changes

**Must-not-change (invariants):**
- Existing endpoint signatures in CommandController
- CommandResponse schema structure
- DecisionLog structure
- Existing test behavior

## Acceptance Criteria Summary

| AC | Description | Test |
|----|-------------|------|
| AC1 | Endpoint exists and accepts body | Integration test |
| AC2 | NEEDS_INPUT → continue → EXECUTED | Happy path test |
| AC3 | Non-NEEDS_INPUT → 400 COMMAND_NOT_CONTINUABLE | State validation test |
| AC4 | Non-existent commandId → 404 | Not found test |
| AC5 | Different user → 403 ACCESS_DENIED | Auth test |
| AC6 | OpenAPI updated | Manual verification |

## Deliverable Format (strict)

1) **Understanding** (3-6 bullets)
   - What the story achieves
   - How it fits into existing architecture
   - Key design decisions from ADRs

2) **Proposed file-level changes:**
   - File: `<path>` — change summary

3) **Step-by-step plan** (max 10 steps, each step testable)
   - Step 1: ...
   - Step 2: ...

4) **Verification:**
   - Commands to run:
     - `./gradlew compileJava`
     - `./gradlew test --tests "*CommandContinuationIntegrationTest*"`
     - `./gradlew test`
     - `./gradlew spotlessCheck`
   - Expected outcomes: All pass without errors

5) **Risks / edge cases** (max 8 bullets)

6) **Questions** (only if truly blocking)

Now read the repository and produce the plan.
```

---

## APPLY Prompt

```markdown
# Codex APPLY Prompt — ST-102: Implement Command Continuation Endpoint

You are Codex CLI acting as a software engineer. Execute the approved plan with minimal diff.

## APPROVED PLAN SUMMARY (from PLAN phase)

Key decisions from approved plan:
- Continuation uses original commandId (not new Idempotency-Key)
- Only NEEDS_INPUT status is continuable
- additionalInput is merged ADDITIVELY with original payload (putAll, not replace)
- Ownership verified: only command initiator can continue
- Re-run decision pipeline with merged context
- DecisionLog captures continuation with correlationId

## STOP-THE-LINE RULE

If execution requires deviating from the plan or changing scope, STOP and ask for human decision.
Do not proceed if:
- You need to modify files not in "Allowed to edit" list
- Tests require changes to unrelated code
- Architecture decisions need reconsideration
- Merge strategy differs from "additive merge"

## Anchor Block — Read These First

1. `AGENTS.MD` (project instructions) — note: file is named AGENTS.MD
2. `docs/planning/workpacks/ST-102/workpack.md` — authoritative workpack
3. `docs/contracts/http/commands.openapi.yaml` — to update
4. `docs/_governance/dod.md` — Definition of Done
5. Existing patterns in `CommandService.java` and `CommandController.java`

## Source of Truth

| Artifact | Path |
|----------|------|
| Workpack | `docs/planning/workpacks/ST-102/workpack.md` |
| Story | `docs/planning/epics/EP-002/stories/ST-102-command-continuation.md` |
| Commands Contract | `docs/contracts/http/commands.openapi.yaml` |
| ErrorCode enum | `services/backend/src/main/java/com/hometusk/shared/exception/ErrorCode.java` |
| CommandStatus enum | `services/backend/src/main/java/com/hometusk/commands/domain/CommandStatus.java` |

## Scope Controls

### Allowed to edit:
- `services/backend/src/main/java/com/hometusk/commands/api/CommandController.java`
- `services/backend/src/main/java/com/hometusk/commands/service/CommandService.java`
- `services/backend/src/main/java/com/hometusk/commands/dto/ContinueCommandRequest.java` (NEW)
- `services/backend/src/main/java/com/hometusk/shared/exception/ErrorCode.java` (add COMMAND_NOT_CONTINUABLE)
- `services/backend/src/test/java/com/hometusk/integration/commands/CommandContinuationIntegrationTest.java` (NEW)
- `docs/contracts/http/commands.openapi.yaml`

### Forbidden to edit:
- `services/backend/src/main/java/com/hometusk/commands/pipeline/**` (guardrails pipeline)
- `services/backend/src/main/java/com/hometusk/commands/domain/Command.java` (domain model)
- `services/backend/src/main/java/com/hometusk/commands/domain/CommandStatus.java`
- `services/backend/src/main/java/com/hometusk/tasks/**` (task domain)
- `services/backend/src/main/java/com/hometusk/shopping/**` (shopping domain)
- `docs/architecture/service-catalog.md` (not needed for this change)
- Any existing test files

### Invariants / must-hold:
- Existing `POST /api/v1/commands` endpoint unchanged
- `CommandResponse` schema backward compatible
- `DecisionLog` structure unchanged
- correlationId propagation pattern maintained
- Ownership check: only command.getInitiator() can continue

## Implementation Constraints

- **Minimal diff.** No opportunistic refactors.
- Keep naming and style consistent with repo (Java 21, Spring Boot idioms).
- Update docs/tests only as required by AC.
- Follow existing patterns in CommandController and CommandService.
- **Unit tests optional** — focus on integration tests for AC coverage.

## CRITICAL: Implementation Details

### ErrorCode Location
**IMPORTANT:** ErrorCode is at `com.hometusk.shared.exception.ErrorCode` (not `shared.ErrorCode`)

Add to ErrorCode enum (after GUARDRAILS_REJECTED):
```java
// Command continuation
COMMAND_NOT_CONTINUABLE("Command cannot be continued in current state"),
```

### Merge Strategy for additionalInput
**IMPORTANT:** Use ADDITIVE merge, not replace:
```java
Map<String, Object> mergedPayload = new HashMap<>(originalPayload);
mergedPayload.putAll(request.additionalInput()); // additionalInput overrides/adds, doesn't clear
```

### HTTP Status Codes
- COMMAND_NOT_CONTINUABLE → **400 Bad Request** (not 409)
- ACCESS_DENIED → 403 Forbidden
- COMMAND_NOT_FOUND → 404 Not Found

### continueCommand() signature
```java
@Transactional
public CommandResponseBase continueCommand(
    UUID commandId,
    ContinueCommandRequest request,
    User requester,
    UUID correlationId
)
```

## Execution Steps (follow exactly)

### Step 1: Add COMMAND_NOT_CONTINUABLE to ErrorCode

File: `services/backend/src/main/java/com/hometusk/shared/exception/ErrorCode.java`

Add after `GUARDRAILS_REJECTED`:
```java
// Command continuation
COMMAND_NOT_CONTINUABLE("Command cannot be continued in current state"),
```

### Step 2: Create ContinueCommandRequest DTO

File: `services/backend/src/main/java/com/hometusk/commands/dto/ContinueCommandRequest.java`

```java
package com.hometusk.commands.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record ContinueCommandRequest(
    @NotNull Map<String, Object> additionalInput
) {}
```

### Step 3: Add continueCommand() to CommandService

File: `services/backend/src/main/java/com/hometusk/commands/service/CommandService.java`

Logic:
1. Find command by ID → throw NotFoundException(COMMAND_NOT_FOUND) if not found
2. Verify command.getInitiator().getId().equals(requester.getId()) → throw AccessDeniedException if not
3. Verify command.getStatus() == CommandStatus.NEEDS_INPUT → throw BusinessException(COMMAND_NOT_CONTINUABLE) if not
4. Parse original payload from command.getPayloadJson()
5. Merge additionalInput ADDITIVELY (putAll)
6. Rebuild DecisionContext with merged payload
7. Re-run decisionProviderSelector.decide(context)
8. handleDecisionResult() — reuse existing method
9. Return CommandResponseBase

### Step 4: Add endpoint to CommandController

File: `services/backend/src/main/java/com/hometusk/commands/api/CommandController.java`

```java
@PostMapping("/{commandId}/continue")
public ResponseEntity<CommandResponseBase> continueCommand(
    @PathVariable UUID commandId,
    @RequestBody @Valid ContinueCommandRequest request,
    @RequestHeader(value = "X-Correlation-ID", required = false) UUID correlationId
) {
    User currentUser = getCurrentUser();
    UUID corrId = correlationId != null ? correlationId : UUID.randomUUID();
    CommandResponseBase response = commandService.continueCommand(
        commandId, request, currentUser, corrId
    );
    return ResponseEntity.ok(response);
}
```

### Step 5: Update OpenAPI contract

File: `docs/contracts/http/commands.openapi.yaml`

Add path `/commands/{commandId}/continue`:
- Method: POST
- Parameters: commandId (path, UUID), X-Correlation-ID (header, optional)
- Request body: ContinueCommandRequest schema
- Responses:
  - 200: CommandResponse (reuse existing)
  - 400: ErrorResponse with code COMMAND_NOT_CONTINUABLE
  - 403: ErrorResponse with code ACCESS_DENIED
  - 404: ErrorResponse with code COMMAND_NOT_FOUND

Add schema ContinueCommandRequest:
```yaml
ContinueCommandRequest:
  type: object
  required:
    - additionalInput
  properties:
    additionalInput:
      type: object
      additionalProperties: true
      description: Additional input to merge with original command payload
```

### Step 6: Create integration test

File: `services/backend/src/test/java/com/hometusk/integration/commands/CommandContinuationIntegrationTest.java`

Test cases (use existing test patterns from CommandPipelineTest):
1. `testContinueCommand_HappyPath`: NEEDS_INPUT → continue → EXECUTED
2. `testContinueCommand_InvalidState_Returns400`: EXECUTED command → 400
3. `testContinueCommand_NotFound_Returns404`: non-existent commandId → 404
4. `testContinueCommand_DifferentUser_Returns403`: other user → 403
5. `testContinueCommand_DecisionLogUpdated`: verify DecisionLog entry created

Setup: Use guardrail that triggers NEEDS_INPUT (e.g., MaxOpenTasksPerAssigneePolicy)

## Verification Commands

After implementation, run:

```bash
# Compile check
./gradlew compileJava

# Run specific test
./gradlew test --tests "*CommandContinuationIntegrationTest*"

# Run all tests (no regression)
./gradlew test

# Format check
./gradlew spotlessCheck

# Apply formatting if needed
./gradlew spotlessApply
```

**Expected:** All commands pass without errors.

## DoD Must-Haves

- [ ] Code compiles without warnings
- [ ] Spotless formatting applied
- [ ] AC1: Endpoint exists and accepts body
- [ ] AC2: NEEDS_INPUT → continue → EXECUTED works
- [ ] AC3: Non-NEEDS_INPUT → 400 COMMAND_NOT_CONTINUABLE
- [ ] AC4: Non-existent commandId → 404
- [ ] AC5: Different user → 403 ACCESS_DENIED
- [ ] AC6: OpenAPI contract updated
- [ ] No regression in existing tests
- [ ] correlationId propagated
- [ ] DecisionLog updated on continuation

## After Changes

1. Show concise diff summary
2. Instruct user to review via `/diff`
3. List files touched
4. Confirm verification commands passed
5. List any remaining risks (expect none if plan followed)
```

---

## REVIEW Prompt

```markdown
# Codex REVIEW Prompt — ST-102: Command Continuation Endpoint

You are Codex CLI acting as a code reviewer. Produce an exit evidence checklist.

## HARD RULES

- DO NOT edit any files.
- DO NOT run commands (assume they were already run).
- Analyze the diff and produce a structured review report.

## Anchor Block — Read These First

1. `AGENTS.md` (project instructions)
2. `docs/planning/workpacks/ST-102/workpack.md` — workpack
3. `docs/planning/workpacks/ST-102/checklist.md` — verification checklist
4. `docs/_governance/dod.md` — Definition of Done
5. `docs/planning/epics/EP-002/stories/ST-102-command-continuation.md` — AC definitions

## Review Scope

Verify implementation against:

### Acceptance Criteria

| AC | Criterion | Evidence Required |
|----|-----------|-------------------|
| AC1 | Endpoint exists | `CommandController.java` has `@PostMapping("/{commandId}/continue")` |
| AC2 | Happy path works | Test `testContinueCommand_HappyPath` passes |
| AC3 | Invalid state rejected | Test `testContinueCommand_InvalidState_Returns400` passes |
| AC4 | Not found handled | Test `testContinueCommand_NotFound_Returns404` passes |
| AC5 | Auth enforced | Test `testContinueCommand_DifferentUser_Returns403` passes |
| AC6 | OpenAPI updated | `commands.openapi.yaml` includes `/commands/{commandId}/continue` |

### DoD Checklist

| Item | Evidence |
|------|----------|
| Code follows conventions | Spotless check passes |
| No compiler warnings | `./gradlew compileJava` clean |
| Integration tests exist | `CommandContinuationIntegrationTest.java` present |
| All tests pass | `./gradlew test` passes |
| OpenAPI updated | Contract includes new endpoint + schemas |
| correlationId propagated | Code inspection |
| DecisionLog updated | Code inspection |
| Ownership verified | Code includes initiator check |

### Security Check

- [ ] Only command initiator can continue (no cross-user access)
- [ ] No cross-household data leak possible
- [ ] Input validation on additionalInput

## Deliverable Format

```
## ST-102 Exit Review

### Summary
[1-2 sentences: what was implemented]

### AC Verification

| AC | Status | Evidence |
|----|--------|----------|
| AC1 | PASS/FAIL | [link/description] |
| AC2 | PASS/FAIL | [link/description] |
| AC3 | PASS/FAIL | [link/description] |
| AC4 | PASS/FAIL | [link/description] |
| AC5 | PASS/FAIL | [link/description] |
| AC6 | PASS/FAIL | [link/description] |

### DoD Verification

| Item | Status | Evidence |
|------|--------|----------|
| Code quality | PASS/FAIL | ... |
| Tests | PASS/FAIL | ... |
| Documentation | PASS/FAIL | ... |
| Observability | PASS/FAIL | ... |
| Security | PASS/FAIL | ... |

### Must-Fix Issues
[List any blocking issues]

### Should-Fix Issues
[List any non-blocking improvements]

### GO / NO-GO Recommendation

**Recommendation:** GO / NO-GO
**Rationale:** [1-2 sentences]
```

Produce the exit review report based on the current state of the repository.
```

---

## Usage Instructions

### Phase 1: Planning
```bash
# In Codex CLI, set read-only mode first
/approvals

# Then run PLAN prompt
# Paste the PLAN prompt above
```

**Human Gate:** Review and approve the plan before proceeding.

### Phase 2: Implementation
```bash
# In Codex CLI, ensure write mode
/approvals

# Run APPLY prompt
# Paste the APPLY prompt above

# After completion, review changes
/diff
```

**Human Gate:** Review diff and test results.

### Phase 3: Review
```bash
# Run REVIEW prompt
# Paste the REVIEW prompt above
```

**Human Gate:** GO/NO-GO decision based on review report.

---

## FIX Prompt (Should-Fix Issues)

```markdown
# Codex FIX Prompt — ST-102: Should-Fix Issues

You are Codex CLI. Apply minimal fixes for should-fix issues identified in code review.

## HARD RULES

- Only fix the specific issues listed below.
- DO NOT refactor or change anything else.
- Minimal diff only.

## Issues to Fix

### Issue 1: Add logging at continueCommand() start

**File:** `services/backend/src/main/java/com/hometusk/commands/service/CommandService.java`

**Location:** Beginning of `continueCommand()` method (after line 211)

**Fix:** Add logging statement similar to execute() method:

```java
log.info(
        "Continuing command: commandId={}, correlationId={}",
        commandId,
        correlationId);
```

Insert after `long startTime = System.currentTimeMillis();` (line 211).

## Verification

After fix, run:

```bash
./gradlew compileJava
./gradlew spotlessApply
```

Expected: Compiles without errors.

## After Changes

1. Show diff summary
2. Confirm fix applied
```

---

## Notes

- All prompts repeat critical constraints because Codex may truncate AGENTS.md
- Verification commands are explicit in each prompt
- STOP-THE-LINE rule prevents scope creep
- Forbidden paths list prevents unintended changes
