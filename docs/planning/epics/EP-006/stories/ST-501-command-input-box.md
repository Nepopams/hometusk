# Story: ST-501 — Command Input Box

## Sources of Truth
- Epic: `docs/planning/epics/EP-006/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-command-ux.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Summary
Create a command input component for submitting structured commands via API.

**Contract note:** Backend is Stage 1 (structured commands with `type` + `payload`).
NL-parsing is planned for Stage 2+ per `commands.openapi.yaml` description.

## User Value
As a household member, I want to quickly create tasks or mark them complete through a command interface, so that I can manage household tasks efficiently.

---

## In Scope
- Command form UI (quick task creation or completion)
- Submit button + Enter key shortcut
- `POST /api/v1/commands` integration (Stage 1 structured commands)
- Generate `Idempotency-Key` header (UUID, unique per user for 24h)
- Generate/pass `X-Correlation-ID` header (UUID v4)
- Loading state during API call
- Pass `householdId` from current HouseholdContext
- Pass `type`, `payload`, `source: 'web'` in request body
- Basic feedback on submission (success toast or inline)
- Error handling for 400/401/403/409 responses

## Out of Scope
- Full status display (ST-502)
- Command history (ST-503)
- needs_input form continuation (NEXT)
- Rich suggestions/autocomplete
- Voice input

---

## Acceptance Criteria

```gherkin
Given I am authenticated and a household is selected
When I navigate to the command entry area
Then I see a quick command form (create_task or complete_task)
And a Submit button is visible

Given I fill in the command form (type: create_task, title: "Clean the kitchen")
When I click Submit (or press Enter)
Then a POST request is made to /api/v1/commands
And request body matches CommandRequest schema (per OpenAPI):
  - householdId: (current household ID)
  - type: "create_task" (enum: create_task | complete_task)
  - payload: { title: "Clean the kitchen", ... } (type-specific payload)
  - source: "web"
And headers include:
  - Idempotency-Key: (UUID, unique per user for 24h)
  - X-Correlation-ID: (UUID v4)
  - Authorization: Bearer (token)

Given command is being processed
When API call is in-flight
Then Submit button shows loading state
And input is disabled

Given API returns 200 with any status
When response is received
Then loading state clears
And response is passed to status display component

Given API returns 400 (validation error)
When response is received
Then error message is displayed inline
And input retains text for correction

Given API returns 403 (not a member)
When response is received
Then "Access denied" error is shown

Given API returns 409 (idempotency conflict)
When response is received
Then "Command already submitted" message is shown
And input is cleared

Given API returns 401 (auth error)
When response is received
Then user is redirected to login
```

---

## UI Specification

**Quick Command Form (Stage 1 structured input):**
```
+----------------------------------------------------------+
| Quick Task                                    [x]         |
|----------------------------------------------------------+
| Title: [Clean the kitchen_________________]               |
| Zone:  [Kitchen (optional)      v]                       |
| Due:   [Today 6pm (optional)____]                        |
|                                                          |
|                        [Cancel] [Create Task]            |
+----------------------------------------------------------+
```

**Complete Task (alternative mode):**
```
+----------------------------------------------------------+
| Complete Task                                             |
|----------------------------------------------------------+
| Task: [Select task to complete...  v]                    |
|                                                          |
|                               [Mark Complete]            |
+----------------------------------------------------------+
```

**Loading state:**
```
+----------------------------------------------------------+
| Quick Task                                                |
|----------------------------------------------------------+
| Title: [Clean the kitchen_________________]               |
|                                                          |
|                                  [Creating...]           |
+----------------------------------------------------------+
```

**Note:** NL-first text input is planned for Stage 2 (backend NL parsing).

---

## API Request Example

Per `commands.openapi.yaml` — Stage 1 structured commands:

```typescript
// POST /api/v1/commands
// Example: create_task
{
  "householdId": "123e4567-e89b-12d3-a456-426614174000",
  "type": "create_task",
  "payload": {
    "title": "Clean the kitchen",
    "zoneId": "123e4567-e89b-12d3-a456-426614174001",  // optional
    "deadline": "2026-01-23T18:00:00Z"                  // optional
  },
  "source": "web"
}

// Example: complete_task
{
  "householdId": "123e4567-e89b-12d3-a456-426614174000",
  "type": "complete_task",
  "payload": {
    "taskId": "123e4567-e89b-12d3-a456-426614174002"
  },
  "source": "web"
}

// Headers:
// Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000 (unique per user for 24h)
// X-Correlation-ID: 7c9e6679-7425-40de-944b-e07fc1f90ae7
// Authorization: Bearer <token>
```

**Idempotency-Key rules (from OpenAPI):**
- Unique per user for 24 hours
- Same key + same payload → replay safe (returns cached response)
- Same key + different payload → 409 IDEMPOTENCY_CONFLICT

---

## Technical Notes

**Files to create/modify:**
- `clients/web/src/components/commands/CommandInput.tsx` — create
- `clients/web/src/hooks/useCommand.ts` — create (mutation hook)
- `clients/web/src/lib/api.ts` — add `executeCommand()` function
- `clients/web/src/types/api.ts` — add Command types (from OpenAPI)
- `clients/web/src/routes/HouseholdLayout.tsx` — add CommandInput to layout

**Type definitions needed (from OpenAPI):**
```typescript
// CommandRequest — Stage 1 structured commands
interface CommandRequest {
  householdId: string;
  type: 'create_task' | 'complete_task';
  payload: CreateTaskPayload | CompleteTaskPayload;
  source: 'api' | 'web' | 'mobile';
  clientTimestamp?: string; // optional, ISO 8601
}

interface CreateTaskPayload {
  title: string;            // required, 1-500 chars
  description?: string;     // optional, max 2000 chars
  zoneId?: string;          // optional, must exist in household
  assigneeId?: string;      // optional, must be household member
  deadline?: string;        // optional, ISO 8601, must be future
}

interface CompleteTaskPayload {
  taskId: string;           // required, must exist in household
}

// CommandResponse — discriminated union by status
type CommandResponse =
  | CommandExecutedResponse
  | CommandNeedsInputResponse
  | CommandRejectedResponse
  | CommandDegradedResponse;
```

**Helper functions:**
```typescript
function generateIdempotencyKey(): string {
  return crypto.randomUUID();
}

function generateCorrelationId(): string {
  return crypto.randomUUID();
}
```

---

## Test Strategy

**Manual tests:**
- Type command and submit → verify API call in DevTools
- Verify headers (Idempotency-Key, X-Correlation-ID, Authorization)
- Verify request body structure
- Test Enter key submission
- Test loading state visibility
- Test error responses (use backend test mode or mock)

**Unit tests:**
- CommandInput component renders
- Submit triggers API call with correct payload
- Loading state shows during submission
- Error state renders on API error
- Input clears on success

---

## Flags

| Flag | Value |
|------|-------|
| contract_impact | no |
| adr_needed | no |
| security_sensitive | yes (command execution) |

---

## Dependencies
- HouseholdContext (from EP-005) — provides householdId
- Auth token provider (from EP-004) — provides JWT

## Points
3 (API integration + UI + state management)

## Priority
P1 (core functionality)
