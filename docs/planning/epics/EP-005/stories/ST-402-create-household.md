# Story: ST-402 — Create Household Form

## Sources of Truth
- Epic: `docs/planning/epics/EP-005/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Summary
Allow user to create a new household with a name.

## User Value
As a new user, I want to create my household so that I can start managing tasks with my family.

---

## In Scope
- Create household page/modal (`/households/new` or modal)
- Name input with validation (required, 1-80 chars)
- `POST /households` integration
- Success: refresh user profile, auto-select new household
- Error handling: validation errors, network errors
- Loading state during submission

## Out of Scope
- Household settings/edit
- Household deletion
- Temporary household flag (NEXT)
- Invite flow (ST-403)

---

## Acceptance Criteria

```gherkin
Given user is on households page or clicks "Create Household"
When create form is displayed
Then name input is focused
And submit button is disabled until name entered

Given user enters valid name (1-80 chars)
When they submit the form
Then loading indicator appears
And POST /households is called
And on success, user profile is refreshed
And new household is auto-selected
And user sees household dashboard

Given user enters invalid name (empty or >80 chars)
When they try to submit
Then validation error is shown
And form is not submitted

Given backend returns 400 (validation error)
When form submission fails
Then error message is displayed
And user can correct and retry

Given backend returns 401
When form submission fails
Then user is redirected to login
```

---

## UI Specification

**Create form (page or modal):**
```
+-----------------------------+
| Create Household            |
|                             |
| Name:                       |
| [________________________]  |
|  Must be 1-80 characters    |
|                             |
| [Cancel]  [Create]          |
+-----------------------------+
```

---

## API Dependencies

| Endpoint | Method | Request | Response |
|----------|--------|---------|----------|
| `POST /api/v1/households` | POST | `{ name: string }` | `Household` |

**CreateHouseholdRequest schema:**
```typescript
interface CreateHouseholdRequest {
  name: string; // minLength: 1, maxLength: 80
}
```

**Response (201):**
```typescript
interface Household {
  id: string;
  name: string;
  createdAt: string;
}
```

**Error responses:**
- 400: Invalid request (name validation)
- 401: Not authenticated

---

## Technical Notes

**Files to create/modify:**
- `clients/web/src/routes/CreateHousehold.tsx` — create
- `clients/web/src/routes/index.tsx` — add route `/households/new`
- `clients/web/src/lib/api/households.ts` — add createHousehold function

**After success:**
1. Refetch `GET /users/me` to update households list
2. Auto-select new household in context
3. Navigate to household dashboard

---

## Test Strategy

**Manual tests:**
- Submit with valid name → household created, selected
- Submit with empty name → validation error
- Submit with >80 char name → validation error
- Network error → error message, can retry

---

## Flags

| Flag | Value |
|------|-------|
| contract_impact | no |
| adr_needed | no |
| security_sensitive | no |

---

## Dependencies
- ST-401 (HouseholdContext exists)

## Points
2 (form + API + refresh)

## Priority
P1

---

## Files Consulted
- `docs/contracts/http/commands.openapi.yaml` (CreateHouseholdRequest, Household schemas)
- `docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md`
