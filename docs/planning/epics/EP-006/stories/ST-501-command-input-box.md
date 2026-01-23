# Story: ST-501 — Command Input Box

## Sources of Truth
- Epic: `docs/planning/epics/EP-006/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-command-ux.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Summary
Create a text input component for entering natural language commands with API integration.

## User Value
As a household member, I want to type a natural language command and submit it, so that I can create tasks or complete them without navigating complex forms.

---

## In Scope
- Command input text field (single line or textarea)
- Submit button + Enter key shortcut
- `POST /api/v1/commands` integration
- Generate `Idempotency-Key` header (UUID v4)
- Pass `X-Correlation-ID` header (UUID v4)
- Loading state during API call
- Pass `householdId` from current HouseholdContext
- Pass `source: 'web'` in request body
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
Then I see a text input with placeholder "Type a command..."
And a Submit button is visible

Given I type "Clean the kitchen tomorrow"
When I click Submit (or press Enter)
Then a POST request is made to /api/v1/commands
And request body contains:
  - householdId: (current household ID)
  - input: "Clean the kitchen tomorrow"
  - source: "web"
And headers include:
  - Idempotency-Key: (UUID v4)
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

**Command input area:**
```
+----------------------------------------------------------+
| +------------------------------------------------------+ |
| | Type a command...                                    | |
| +------------------------------------------------------+ |
|                                         [Submit]         |
+----------------------------------------------------------+
```

**Loading state:**
```
+----------------------------------------------------------+
| +------------------------------------------------------+ |
| | Clean the kitchen tomorrow                           | |
| +------------------------------------------------------+ |
|                                      [Submitting...]     |
+----------------------------------------------------------+
```

---

## API Request Example

```typescript
// POST /api/v1/commands
{
  "householdId": "123e4567-e89b-12d3-a456-426614174000",
  "input": "Clean the kitchen tomorrow",
  "source": "web"
}

// Headers:
// Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
// X-Correlation-ID: 7c9e6679-7425-40de-944b-e07fc1f90ae7
// Authorization: Bearer <token>
```

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
interface CommandRequest {
  householdId: string;
  input: string;
  source: 'api' | 'web' | 'mobile';
}

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
