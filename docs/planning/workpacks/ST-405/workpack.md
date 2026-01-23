# Workpack: ST-405 — Members List View

## Sources of Truth
- Story: `docs/planning/epics/EP-005/stories/ST-405-members-list.md`
- Epic: `docs/planning/epics/EP-005/epic.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Goal
Display read-only list of household members.

## User Value
Users can see who's in their household.

---

## In Scope
- Members page at `/households/{id}/members`
- Display: name, email, role badge, joined date
- Loading and error states
- "Invite Member" button (reuse InviteModal from ST-403)
- "Members" link in Sidebar navigation

## Out of Scope
- Edit/remove members
- Leave household
- Member avatars

---

## Current State (reuse existing)

| Component | Path | State |
|-----------|------|-------|
| API function | `clients/web/src/lib/api.ts` | **getMembers() EXISTS** |
| Hook | `clients/web/src/hooks/useMembers.ts` | **useMembers() EXISTS** |
| Type | `clients/web/src/types/api.ts` | **HouseholdMember EXISTS** |
| InviteModal | `clients/web/src/components/InviteModal.tsx` | **EXISTS** (from ST-403) |

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `clients/web/src/routes/Members.tsx` | CREATE | Members list page |
| `clients/web/src/routes/index.tsx` | MODIFY | Add `/households/:householdId/members` route |
| `clients/web/src/components/Layout/Sidebar.tsx` | MODIFY | Add "Members" nav link |
| `clients/web/src/styles/index.css` | MODIFY | Members list styles |

---

## Verification Commands

```bash
cd clients/web && npm run lint
cd clients/web && npm run build
```

---

## Anti-Scope-Creep

DO NOT:
- Add edit/remove member functionality
- Add leave household
- Add member avatars/profiles
- Create separate MembersList component (keep simple in route)
