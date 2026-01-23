# PLAN Prompt: ST-401 — Household Selector & Empty State

## Role
You are a planning agent. Your task is to **analyze the codebase and produce an implementation plan** for ST-401.

**CRITICAL CONSTRAINTS:**
- **READ-ONLY**: NO file edits, NO file writes, NO code changes
- **NO network access**, NO package installs
- If any required information is missing → STOP and list what is needed

---

## Sources of Truth (MUST read first)
1. `docs/planning/workpacks/ST-401/workpack.md` — implementation spec
2. `docs/planning/epics/EP-005/stories/ST-401-household-selector.md` — story AC
3. `docs/contracts/http/commands.openapi.yaml` — UserProfile.households schema
4. `docs/_governance/dod.md` — Definition of Done checklist

---

## Current State Summary

Based on codebase analysis, the following **already exists**:

| Component | Path | State |
|-----------|------|-------|
| AuthContext | `clients/web/src/context/AuthContext.tsx` | Has `householdId`, `selectHousehold`, `user.households`. Uses **localStorage** (needs sessionStorage). |
| HouseholdSelector (route) | `clients/web/src/routes/HouseholdSelector.tsx` | List view of cards. Empty state exists but **no CTA button**. |
| Header | `clients/web/src/components/Layout/Header.tsx` | Static chip display, **no dropdown**. |
| HouseholdLayout | `clients/web/src/routes/HouseholdLayout.tsx` | Wrapper, OK. |

---

## Goal
Enhance existing code to meet ST-401 acceptance criteria:
1. Header shows **dropdown selector** (not static chip)
2. Empty state has **"Create Household" CTA** navigating to `/households/new`
3. Selection persists in **sessionStorage** (not localStorage)
4. On load: validate stored selection against user's current households

---

## Allowed Commands (read-only only)

```bash
# Directory listing
ls -la <path>

# File search
find <path> -name "*.tsx" -type f

# Read files
cat <path>

# Content search
rg "<pattern>" <path>
grep -r "<pattern>" <path>

# Partial reads
head -n 50 <path>
tail -n 50 <path>
sed -n '10,30p' <path>

# Git inspection (read-only)
git status
git diff --stat
```

---

## Planning Tasks

### Task 1: Verify API contract
Read `docs/contracts/http/commands.openapi.yaml` and confirm:
- `UserProfile` schema has `households: HouseholdSummary[]`
- `HouseholdSummary` has: `id`, `name`, `role`

### Task 2: Analyze AuthContext changes needed
Read `clients/web/src/context/AuthContext.tsx`:
- Find `HOUSEHOLD_ID_KEY` storage key
- Identify where to change `localStorage` → `sessionStorage`
- Check if validation of stored ID against user.households exists

### Task 3: Analyze Header changes needed
Read `clients/web/src/components/Layout/Header.tsx`:
- Current structure
- Where to add dropdown selector
- What props/context it needs

### Task 4: Analyze HouseholdSelector route
Read `clients/web/src/routes/HouseholdSelector.tsx`:
- Current empty state implementation
- Where to add "Create Household" CTA button
- Navigation to `/households/new`

### Task 5: Check routing
Read `clients/web/src/routes/index.tsx`:
- Verify `/households/new` route exists or needs to be added
- How routes are structured

### Task 6: Check existing UI components
Check if reusable dropdown/select exists:
- `clients/web/src/components/ui/Select.tsx`
- Any existing dropdown patterns

---

## Output Format

Produce a structured plan in this format:

```markdown
## Implementation Plan: ST-401

### File 1: `clients/web/src/context/AuthContext.tsx`
**Action:** MODIFY
**Changes:**
1. Change `localStorage` to `sessionStorage` for `HOUSEHOLD_ID_KEY`
2. Add validation: on init, check if stored householdId exists in user.households
3. If invalid → clear stored value, select first household (or null if empty)

### File 2: `clients/web/src/components/Layout/Header.tsx`
**Action:** MODIFY
**Changes:**
1. Import useAuth hook
2. Replace static chip with dropdown selector
3. Show current household name + dropdown arrow
4. List all households with role badges
5. Include "Create Household" link at bottom

### File 3: ...

### New Component: `clients/web/src/components/HouseholdDropdown.tsx`
**Action:** CREATE (if needed)
**Purpose:** ...
**Props:** ...

## Commit Plan
1. Commit 1: ...
2. Commit 2: ...

## Risks & Mitigations
- Risk: ...
- Mitigation: ...

## Verification Steps
1. `npm run lint` passes
2. `npm run build` passes
3. Manual test: ...
```

---

## Anti-Scope-Creep Checklist

You MUST NOT plan for:
- [ ] Create household form (ST-402)
- [ ] Invite button/modal (ST-403)
- [ ] Household settings/edit
- [ ] Leave household
- [ ] Member list
- [ ] Any backend changes

If you find yourself planning any of the above → STOP, that is out of scope.

---

## STOP Conditions

**STOP and request input if:**
1. `UserProfile.households` schema differs from expected
2. Routing structure is unclear
3. No clear pattern for dropdowns in codebase
4. Any ambiguity in AC interpretation

**DO NOT GUESS. Ask for clarification.**
