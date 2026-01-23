# PLAN Prompt: ST-404 — Accept Invite Flow

## Role
You are a planning agent. Your task is to **analyze the codebase and produce an implementation plan** for ST-404.

**CRITICAL CONSTRAINTS:**
- **READ-ONLY**: NO file edits, NO file writes, NO code changes
- **NO network access**, NO package installs
- If any required information is missing → STOP and list what is needed

---

## Sources of Truth (MUST read first)
1. `docs/planning/workpacks/ST-404/workpack.md` — implementation spec
2. `docs/planning/epics/EP-005/stories/ST-404-accept-invite.md` — story AC
3. `docs/contracts/http/commands.openapi.yaml` — AcceptInviteRequest, AcceptInviteResponse schemas
4. `docs/_governance/dod.md` — Definition of Done checklist

---

## Current State Summary

Based on codebase analysis:

| Component | Path | State |
|-----------|------|-------|
| API module | `clients/web/src/lib/api.ts` | Has apiFetch, createInvite. **NO acceptInvite.** |
| Types | `clients/web/src/types/api.ts` | Has CreateInviteResponse. **NO AcceptInviteResponse.** |
| Routes | `clients/web/src/routes/index.tsx` | **NO `/invite` route.** |
| ProtectedRoute | `clients/web/src/components/ProtectedRoute.tsx` | Passes `state={{ from: location }}` on redirect. |
| Callback | `clients/web/src/routes/Callback.tsx` | Always redirects to `/households`. **Does NOT check state.from.** |

---

## Goal
Implement accept invite flow:
1. Route `/invite?token=hti_xxx`
2. Call `POST /invites/accept`
3. Handle 200, 404, 410 status codes
4. On success: refresh profile, select household, redirect
5. Preserve token through login redirect

---

## Allowed Commands (read-only only)

```bash
ls -la <path>
cat <path>
rg "<pattern>" <path>
head -n 50 <path>
```

---

## Planning Tasks

### Task 1: Verify API contract
Read `docs/contracts/http/commands.openapi.yaml`:
- Confirm `POST /invites/accept` endpoint
- Confirm AcceptInviteRequest: `{ inviteToken: string }`
- Confirm AcceptInviteResponse: `{ membership, household }`

### Task 2: Analyze types/api.ts
- Plan AcceptInviteResponse type
- Plan InviteMembership type (nested in response)

### Task 3: Analyze api.ts
- Plan acceptInvite(inviteToken: string) function
- Handle 404 and 410 as ApiError

### Task 4: Analyze ProtectedRoute
- Confirm it passes `state={{ from: location }}`
- Understand how to use this for redirect after login

### Task 5: Analyze Callback.tsx
- Current flow: always redirects to /households
- Plan modification to check `location.state?.from` and redirect there instead

### Task 6: Plan AcceptInvite.tsx component
- Token extraction from URL
- Auto-accept on mount (if token present)
- Loading state
- Success: refetchUser, selectHousehold, navigate to household
- Error states: 404, 410 with messages
- "Back to Home" link

### Task 7: Plan route registration
- Add `/invite` under ProtectedRoute
- Understand route structure

---

## Output Format

```markdown
## Implementation Plan: ST-404

### File 1: `clients/web/src/types/api.ts`
**Action:** MODIFY
**Changes:**
1. Add InviteMembership interface
2. Add AcceptInviteResponse interface

### File 2: `clients/web/src/lib/api.ts`
**Action:** MODIFY
**Changes:**
1. Add acceptInvite(inviteToken: string) function

### File 3: `clients/web/src/routes/Callback.tsx`
**Action:** MODIFY
**Changes:**
1. Check location.state?.from after successful login
2. If present, redirect there instead of /households

### File 4: `clients/web/src/routes/AcceptInvite.tsx`
**Action:** CREATE
**Purpose:** Accept invite page
**Structure:**
- Extract token from URL search params
- useEffect to auto-accept on mount
- States: loading, success, error (404/410)
- On success: refetchUser, selectHousehold, navigate
- Error UI with message and "Back to Home" link

### File 5: `clients/web/src/routes/index.tsx`
**Action:** MODIFY
**Changes:**
1. Import AcceptInvite
2. Add `/invite` route under ProtectedRoute

### File 6: `clients/web/src/styles/index.css`
**Action:** MODIFY
**Changes:**
1. Add accept invite page styles

## Commit Plan
1. Add types + API function
2. Modify Callback to handle redirect
3. Create AcceptInvite page + route + CSS

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
- [ ] Invite preview (show household before accepting)
- [ ] Decline functionality
- [ ] Manual token input field
- [ ] Guest accept (without login)
- [ ] Create invite (ST-403)

If you find yourself planning any of the above → STOP, that is out of scope.

---

## STOP Conditions

**STOP and request input if:**
1. AcceptInviteResponse schema differs from expected
2. ProtectedRoute doesn't pass location state
3. Callback modification unclear
4. Token extraction pattern unclear

**DO NOT GUESS. Ask for clarification.**
