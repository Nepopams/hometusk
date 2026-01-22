# Story: ST-401 — Household Selector & Empty State

## Sources of Truth
- Epic: `docs/planning/epics/EP-005/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Summary
Display user's households list with switcher and handle empty state gracefully.

## User Value
As a user with multiple households, I want to see and switch between them so that I can manage different contexts.
As a new user with no households, I want clear guidance to create one.

---

## In Scope
- Fetch households from `GET /users/me` response (`households: HouseholdSummary[]`)
- Household selector dropdown/menu in header
- Display: household name, role badge (admin/member)
- Store selected household in React context
- Persist selection in sessionStorage (survive refresh)
- Empty state: "No households yet" with "Create Household" CTA

## Out of Scope
- Create household form (ST-402)
- Invite functionality (ST-403)
- Household settings/edit
- Leave household

---

## Acceptance Criteria

```gherkin
Given user is authenticated with 2+ households
When they view the app header
Then household selector shows current household name
And dropdown lists all households with roles

Given user clicks different household in selector
When selection changes
Then app context updates to new household
And tasks/zones/notifications reload for new household
And selection persists across page refresh

Given user is authenticated with 0 households
When they view the app
Then empty state is displayed
And "Create your first household" button is visible
And clicking it navigates to create household form

Given user is authenticated with 1 household
When they view the app
Then that household is auto-selected
And selector shows household name (no dropdown needed, or minimal)
```

---

## UI Specification

**Header with selector:**
```
+------------------------------------------+
| HomeTusk   [Household Name ▼]    [User]  |
+------------------------------------------+
```

**Dropdown (multi-household):**
```
+------------------------+
| My Home ✓ (admin)      |
| Beach House (member)   |
| + Create Household     |
+------------------------+
```

**Empty state:**
```
+-----------------------------+
|                             |
|   Welcome to HomeTusk!      |
|                             |
|   You don't have any        |
|   households yet.           |
|                             |
|   [Create your first]       |
|   [household        ]       |
|                             |
+-----------------------------+
```

---

## API Dependencies

| Endpoint | Method | Response Field |
|----------|--------|----------------|
| `GET /api/v1/users/me` | GET | `households: HouseholdSummary[]` |

**HouseholdSummary schema:**
```typescript
interface HouseholdSummary {
  id: string;       // UUID
  name: string;
  role: 'admin' | 'member';
}
```

---

## Technical Notes

**Files to modify:**
- `clients/web/src/context/HouseholdContext.tsx` — create/update
- `clients/web/src/components/HouseholdSelector.tsx` — create
- `clients/web/src/routes/Households.tsx` — update empty state
- `clients/web/src/components/Layout.tsx` — add selector to header

**State management:**
- Selected household ID in HouseholdContext
- Persist to sessionStorage key: `selectedHouseholdId`
- On load: restore from storage if valid (exists in user's households)

---

## Test Strategy

**Manual tests:**
- Login with 0 households → see empty state
- Login with 1 household → auto-selected
- Login with 2+ households → selector dropdown works
- Switch household → context updates, data reloads
- Refresh page → selection persists

---

## Flags

| Flag | Value |
|------|-------|
| contract_impact | no |
| adr_needed | no |
| security_sensitive | no |

---

## Dependencies
- S03 complete (auth working)
- `GET /users/me` returns households (already does)

## Points
2 (context + UI + persistence)

## Priority
P1 (blocks other stories)

---

## Files Consulted
- `docs/contracts/http/commands.openapi.yaml` (UserProfile, HouseholdSummary schemas)
- `docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md`
