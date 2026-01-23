# PLAN Prompt: ST-403 — Create Invite & Share

## Role
You are a planning agent. Your task is to **analyze the codebase and produce an implementation plan** for ST-403.

**CRITICAL CONSTRAINTS:**
- **READ-ONLY**: NO file edits, NO file writes, NO code changes
- **NO network access**, NO package installs
- If any required information is missing → STOP and list what is needed

---

## Sources of Truth (MUST read first)
1. `docs/planning/workpacks/ST-403/workpack.md` — implementation spec
2. `docs/planning/epics/EP-005/stories/ST-403-create-invite.md` — story AC
3. `docs/contracts/http/commands.openapi.yaml` — CreateInviteResponse schema
4. `docs/_governance/dod.md` — Definition of Done checklist

---

## Current State Summary

Based on codebase analysis:

| Component | Path | State |
|-----------|------|-------|
| API module | `clients/web/src/lib/api.ts` | Has apiFetch pattern. **NO createInvite.** |
| Types | `clients/web/src/types/api.ts` | **NO CreateInviteResponse type.** |
| Sidebar | `clients/web/src/components/Layout/Sidebar.tsx` | Has Navigation section. **NO invite button.** |
| Modal components | `clients/web/src/components/` | **NO existing modal pattern.** |

---

## Goal
Add "Invite Member" functionality:
1. Button in Sidebar to trigger invite creation
2. Modal displaying invite link with copy-to-clipboard
3. `POST /households/{householdId}/invites` integration

---

## Allowed Commands (read-only only)

```bash
ls -la <path>
find <path> -name "*.tsx" -type f
cat <path>
rg "<pattern>" <path>
head -n 50 <path>
tail -n 50 <path>
sed -n '10,30p' <path>
git status
git diff --stat
```

---

## Planning Tasks

### Task 1: Verify API contract
Read `docs/contracts/http/commands.openapi.yaml`:
- Confirm `POST /households/{householdId}/invites` endpoint
- Confirm `CreateInviteResponse` schema: inviteToken, expiresAt, status, inviteLink

### Task 2: Analyze types/api.ts
- Plan where to add CreateInviteResponse type
- Check for InviteStatus type if needed

### Task 3: Analyze api.ts
- Understand apiFetch pattern for POST without body
- Plan createInvite(householdId) function

### Task 4: Analyze Sidebar.tsx
- Current structure
- Where to add "Invite Member" button
- How to get householdId (already uses useParams)

### Task 5: Check for existing modal patterns
- Look for any modal/dialog components in codebase
- If none, plan simple modal from scratch

### Task 6: Analyze styles/index.css
- Check for any overlay/modal styles
- Plan modal CSS (overlay, box, content)

### Task 7: Check clipboard API usage
- Plan navigator.clipboard.writeText usage
- Consider fallback for unsupported browsers

---

## Output Format

```markdown
## Implementation Plan: ST-403

### File 1: `clients/web/src/types/api.ts`
**Action:** MODIFY
**Changes:**
1. Add InviteStatus type
2. Add CreateInviteResponse interface

### File 2: `clients/web/src/lib/api.ts`
**Action:** MODIFY
**Changes:**
1. Add createInvite(householdId: string) function

### File 3: `clients/web/src/components/InviteModal.tsx`
**Action:** CREATE
**Purpose:** Modal displaying invite link with copy functionality
**Structure:**
- Props: householdId, isOpen, onClose
- State: loading, invite, error, copied
- On open: call createInvite API
- Display: link input (readonly), copy button, expiry, done button

### File 4: `clients/web/src/components/Layout/Sidebar.tsx`
**Action:** MODIFY
**Changes:**
1. Add state for modal open/close
2. Add "Invite Member" button in new "Actions" section
3. Render InviteModal conditionally

### File 5: `clients/web/src/styles/index.css`
**Action:** MODIFY
**Changes:**
1. Add modal overlay styles
2. Add modal box styles
3. Add invite-specific styles

## Commit Plan
1. Add types + API function
2. Create InviteModal component
3. Add Sidebar button + modal integration + CSS

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
- [ ] Invite list/history
- [ ] Revoke invite functionality
- [ ] Email/SMS delivery
- [ ] QR code generation
- [ ] Members page (ST-405)
- [ ] Accept invite flow (ST-404)

If you find yourself planning any of the above → STOP, that is out of scope.

---

## STOP Conditions

**STOP and request input if:**
1. `POST /households/{householdId}/invites` endpoint missing
2. CreateInviteResponse schema differs from expected
3. Unclear where to place invite button
4. Clipboard API compatibility concerns

**DO NOT GUESS. Ask for clarification.**
