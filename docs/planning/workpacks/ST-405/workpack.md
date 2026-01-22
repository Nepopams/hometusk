# Workpack: ST-405 — Members List View

## Sources of Truth
- Story: `docs/planning/epics/EP-005/stories/ST-405-members-list.md`
- Epic: `docs/planning/epics/EP-005/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Goal
Display household members in a read-only list.

## User Value
Users can see who's in their household.

---

## In Scope
- Members page/route
- `GET /households/{id}/members` API call
- Display: name, email, role, joined date
- Loading and error states
- "Invite Member" button (links to ST-403)

## Out of Scope
- Edit member role
- Remove member
- Leave household
- Member avatars/profiles

---

## UI Surfaces / Flows

### Flow: View Members
1. User navigates to Members page (from nav)
2. Loading state while fetching
3. `GET /members` called with householdId
4. Display list in table/cards
5. "Invite Member" button at top

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `clients/web/src/routes/Members.tsx` | CREATE | Members page |
| `clients/web/src/routes/index.tsx` | MODIFY | Add route |
| `clients/web/src/lib/api/households.ts` | MODIFY | Add getMembers function |
| `clients/web/src/components/MembersList.tsx` | CREATE | List component |
| `clients/web/src/components/Layout.tsx` or nav | MODIFY | Add "Members" nav link |

---

## API Dependencies

| Endpoint | Method | Response |
|----------|--------|----------|
| `GET /api/v1/households/{householdId}/members` | GET | `HouseholdMember[]` |

**Response (200):**
```typescript
interface HouseholdMember {
  userId: string;
  displayName: string;
  email: string;
  role: 'admin' | 'member';
  joinedAt: string;  // ISO date
}
```

**Status codes:**
- 200: Success
- 401: Not authenticated
- 403: Not a member

---

## Data Contract Assumptions

- Response is array (may be empty, but unlikely)
- Each member has displayName (may fallback to email)
- Role is `admin` or `member`

---

## Implementation Plan

### Commit 1: Create API function
- `getMembers(householdId: string)` in `lib/api/households.ts`

### Commit 2: Create MembersList component
- Table/card layout
- Columns: Name, Role, Joined
- Role badge (admin/member)
- Format joinedAt date

### Commit 3: Create Members page
- Fetch members on mount
- Loading state
- Error state with retry
- "Invite Member" button (opens InviteModal from ST-403)

### Commit 4: Add navigation
- Add "Members" link to sidebar/nav
- Route `/households/:id/members` or use context

---

## Verification Commands

```bash
cd clients/web && npm run build
cd clients/web && npm run lint
```

---

## Demo Scenario (Manual)

1. Login and select household
2. Navigate to Members
3. See list of members with roles
4. See "Invite Member" button
5. Click invite → modal opens (ST-403)

---

## Risks

| Risk | Mitigation |
|------|------------|
| Empty list (1 member only) | Show "You're the only member" + invite CTA |
| 403 error | Should not happen with correct context, show error |

---

## Rollback
- Remove Members.tsx
- Remove route
- Remove nav link
- Remove API function

---

## Anti-Scope-Creep

DO NOT:
- Add edit role functionality
- Add remove member
- Add leave household
- Add member profiles/avatars
