# Story: ST-405 — Members List View

## Sources of Truth
- Epic: `docs/planning/epics/EP-005/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Summary
Display list of household members (read-only).

## User Value
As a household member, I want to see who's in my household so that I know my team.

---

## In Scope
- Members page/section: `/households/{id}/members` or section in settings
- `GET /households/{householdId}/members` integration
- Display: name, email, role, joined date
- Loading and error states
- Link to invite (ST-403)

## Out of Scope
- Edit member (change role)
- Remove member
- Leave household
- Member profiles/avatars

---

## Acceptance Criteria

```gherkin
Given user is a member of a household
When they navigate to Members page
Then list of members is displayed
And each member shows: name, email, role badge, joined date

Given household has 1 member (just user)
When members list loads
Then user sees themselves
And "Invite Member" CTA is prominent

Given API call fails
When members list cannot load
Then error message is displayed
And retry option is available

Given user is not a member (403)
When they try to access members
Then access denied message is shown
```

---

## UI Specification

**Members list:**
```
+------------------------------------------+
| Members (3)              [Invite Member] |
+------------------------------------------+
| Name           | Role   | Joined         |
|----------------|--------|----------------|
| John Doe       | admin  | Jan 15, 2026   |
| jane@email.com | member | Jan 18, 2026   |
| Bob Smith      | member | Jan 20, 2026   |
+------------------------------------------+
```

---

## API Dependencies

| Endpoint | Method | Response |
|----------|--------|----------|
| `GET /api/v1/households/{householdId}/members` | GET | `HouseholdMember[]` |

**HouseholdMember schema:**
```typescript
interface HouseholdMember {
  userId: string;
  displayName: string;
  email: string;
  role: 'admin' | 'member';
  joinedAt: string;
}
```

**Status codes:**
- 200: Success
- 401: Not authenticated
- 403: Not a member

---

## Technical Notes

**Files to create/modify:**
- `clients/web/src/routes/Members.tsx` — create
- `clients/web/src/routes/index.tsx` — add route
- `clients/web/src/lib/api/households.ts` — add getMembers function
- `clients/web/src/components/MembersList.tsx` — create

**Navigation:**
- Add "Members" link to sidebar/nav within household context

---

## Test Strategy

**Manual tests:**
- View members list → all members shown with correct data
- Single member household → shows "Invite" CTA prominently
- 403 error → access denied message

---

## Flags

| Flag | Value |
|------|-------|
| contract_impact | no |
| adr_needed | no |
| security_sensitive | no |

---

## Dependencies
- ST-401 (household context)
- ST-403 (invite button integration)

## Points
2 (API + list view)

## Priority
P2

---

## Files Consulted
- `docs/contracts/http/commands.openapi.yaml` (HouseholdMember schema)
- `docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md`
