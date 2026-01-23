# PLAN Prompt: ST-405 — Members List View

## Role
You are a planning agent. Your task is to **analyze the codebase and produce an implementation plan** for ST-405.

**CRITICAL CONSTRAINTS:**
- **READ-ONLY**: NO file edits, NO file writes, NO code changes
- **NO network access**, NO package installs
- If any required information is missing → STOP and list what is needed

---

## Sources of Truth (MUST read first)
1. `docs/planning/workpacks/ST-405/workpack.md` — implementation spec
2. `docs/planning/epics/EP-005/stories/ST-405-members-list.md` — story AC
3. `docs/contracts/http/commands.openapi.yaml` — HouseholdMember schema
4. `docs/_governance/dod.md` — Definition of Done checklist

---

## Current State Summary (IMPORTANT - reuse existing!)

| Component | Path | State |
|-----------|------|-------|
| API function | `clients/web/src/lib/api.ts` | **getMembers() EXISTS** |
| Hook | `clients/web/src/hooks/useMembers.ts` | **useMembers() EXISTS** |
| Type | `clients/web/src/types/api.ts` | **HouseholdMember EXISTS** |
| InviteModal | `clients/web/src/components/InviteModal.tsx` | **EXISTS** (from ST-403) |
| Sidebar | `clients/web/src/components/Layout/Sidebar.tsx` | Has nav links, needs "Members" |

**NO NEW API or types needed!**

---

## Goal
Create Members page that:
1. Shows list of household members (name, email, role, joined date)
2. Has "Invite Member" button (opens InviteModal)
3. Handles loading/error states
4. Is accessible via "Members" nav link in Sidebar

---

## Allowed Commands (read-only only)

```bash
cat <path>
rg "<pattern>" <path>
```

---

## Planning Tasks

### Task 1: Analyze useMembers hook
Read `clients/web/src/hooks/useMembers.ts`:
- Understand return values (members, isLoading, error)
- Confirm it handles householdId undefined case

### Task 2: Analyze HouseholdMember type
Read `clients/web/src/types/api.ts`:
- Confirm fields: userId, displayName, email, role, joinedAt

### Task 3: Analyze Sidebar for nav link pattern
Read `clients/web/src/components/Layout/Sidebar.tsx`:
- Understand NavLink pattern
- Plan where to add "Members" link

### Task 4: Analyze InviteModal usage
Read `clients/web/src/components/InviteModal.tsx`:
- Understand props: householdId, isOpen, onClose
- Plan reuse in Members page

### Task 5: Analyze routes/index.tsx
- Understand HouseholdLayout children structure
- Plan where to add members route

### Task 6: Check existing page patterns
Read `clients/web/src/routes/TasksList.tsx` or similar:
- Understand loading/error pattern
- Use as template for Members page

---

## Output Format

```markdown
## Implementation Plan: ST-405

### File 1: `clients/web/src/routes/Members.tsx`
**Action:** CREATE
**Purpose:** Members list page
**Structure:**
- Use useParams for householdId
- Use useMembers hook
- State for InviteModal open/close
- Render: header with count + invite button, table/list of members
- Format joinedAt as readable date

### File 2: `clients/web/src/routes/index.tsx`
**Action:** MODIFY
**Changes:**
1. Import Members
2. Add { path: 'members', element: <Members /> } to HouseholdLayout children

### File 3: `clients/web/src/components/Layout/Sidebar.tsx`
**Action:** MODIFY
**Changes:**
1. Add NavLink to members route

### File 4: `clients/web/src/styles/index.css`
**Action:** MODIFY
**Changes:**
1. Add members list/table styles

## Commit Plan
1. Create Members page
2. Add route + Sidebar link + CSS

## Verification Steps
1. `npm run lint` passes
2. `npm run build` passes
3. Manual test: navigate to members, see list, invite button works
```

---

## Anti-Scope-Creep Checklist

You MUST NOT plan for:
- [ ] Edit/remove member functionality
- [ ] Leave household
- [ ] Member avatars/profiles
- [ ] New API functions (use existing getMembers)
- [ ] New hooks (use existing useMembers)
- [ ] Separate MembersList component

If you find yourself planning any of the above → STOP, that is out of scope.

---

## STOP Conditions

**STOP and request input if:**
1. useMembers hook doesn't exist or differs from expected
2. HouseholdMember type missing fields
3. InviteModal pattern unclear

**DO NOT GUESS. Ask for clarification.**
