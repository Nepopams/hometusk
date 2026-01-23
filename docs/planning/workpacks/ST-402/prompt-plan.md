# PLAN Prompt: ST-402 — Create Household Form

## Role
You are a planning agent. Your task is to **analyze the codebase and produce an implementation plan** for ST-402.

**CRITICAL CONSTRAINTS:**
- **READ-ONLY**: NO file edits, NO file writes, NO code changes
- **NO network access**, NO package installs
- If any required information is missing → STOP and list what is needed

---

## Sources of Truth (MUST read first)
1. `docs/planning/workpacks/ST-402/workpack.md` — implementation spec
2. `docs/planning/epics/EP-005/stories/ST-402-create-household.md` — story AC
3. `docs/contracts/http/commands.openapi.yaml` — CreateHouseholdRequest, Household schemas
4. `docs/_governance/dod.md` — Definition of Done checklist

---

## Current State Summary

Based on codebase analysis, the following **already exists**:

| Component | Path | State |
|-----------|------|-------|
| AuthContext | `clients/web/src/context/AuthContext.tsx` | Has `user`, `selectHousehold`, reconciliation. **NO refetchUser method yet.** |
| API module | `clients/web/src/lib/api.ts` | Has `getMe`, `apiFetch`. **NO createHousehold.** |
| Types | `clients/web/src/types/api.ts` | Has `HouseholdSummary`. **NO Household type.** |
| Routes | `clients/web/src/routes/index.tsx` | Has `/households` selector. **NO `/households/new`.** |
| HouseholdSelector | `clients/web/src/routes/HouseholdSelector.tsx` | Empty state CTA navigates to `/households/new`. |

---

## Goal
Create a form page at `/households/new` that:
1. Accepts household name input (1-80 chars)
2. Calls `POST /api/v1/households`
3. On success: refreshes user profile, selects new household, navigates to tasks

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
- `POST /households` endpoint exists
- Request: `CreateHouseholdRequest { name: string }`
- Response 201: `Household { id, name, createdAt }`
- Response 400: validation error

### Task 2: Analyze types/api.ts
Read `clients/web/src/types/api.ts`:
- Confirm `Household` type is missing
- Plan where to add it

### Task 3: Analyze api.ts
Read `clients/web/src/lib/api.ts`:
- Confirm `createHousehold` function is missing
- Understand `apiFetch` pattern for POST requests

### Task 4: Analyze AuthContext for refetch capability
Read `clients/web/src/context/AuthContext.tsx`:
- Check if there's a way to refetch user profile
- If not, plan how to add `refetchUser` or modify existing flow

### Task 5: Analyze routes/index.tsx
Read `clients/web/src/routes/index.tsx`:
- Understand current route structure
- Plan where to add `/households/new` (must be BEFORE `:householdId` to avoid conflict)

### Task 6: Check existing form patterns
Look for any existing form components for reference:
- Input styling patterns
- Validation patterns
- Error display patterns

### Task 7: Check styles for form CSS
Read `clients/web/src/styles/index.css`:
- Check for existing form styles
- Plan what CSS to add

---

## Output Format

Produce a structured plan in this format:

```markdown
## Implementation Plan: ST-402

### File 1: `clients/web/src/types/api.ts`
**Action:** MODIFY
**Changes:**
1. Add Household interface

### File 2: `clients/web/src/lib/api.ts`
**Action:** MODIFY
**Changes:**
1. Add createHousehold function

### File 3: `clients/web/src/context/AuthContext.tsx`
**Action:** MODIFY (if needed)
**Changes:**
1. Add refetchUser capability (or explain how to refresh)

### File 4: `clients/web/src/routes/CreateHousehold.tsx`
**Action:** CREATE
**Purpose:** Form page for creating household
**Structure:**
- State: name, error, loading
- Validation: required, max 80 chars
- Submit: POST /households → getMe() → selectHousehold() → navigate()

### File 5: `clients/web/src/routes/index.tsx`
**Action:** MODIFY
**Changes:**
1. Import CreateHousehold
2. Add route `/households/new` BEFORE `:householdId`

### File 6: `clients/web/src/styles/index.css`
**Action:** MODIFY
**Changes:**
1. Add form styles (input, error, etc.)

## Commit Plan
1. Commit 1: Add Household type + createHousehold API
2. Commit 2: Add refetchUser to AuthContext (if needed)
3. Commit 3: Create CreateHousehold page + route + styles

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
- [ ] Household settings/edit
- [ ] Household kind (temporary/permanent)
- [ ] Household description field
- [ ] Delete household
- [ ] Invite button on this page (ST-403)

If you find yourself planning any of the above → STOP, that is out of scope.

---

## STOP Conditions

**STOP and request input if:**
1. `POST /households` endpoint missing from OpenAPI
2. AuthContext cannot be extended for refetch
3. Route structure unclear
4. Form validation pattern unclear

**DO NOT GUESS. Ask for clarification.**
