# Codex PLAN Prompt: ST-301 — Keycloak OIDC Integration

## Role
You are a developer planning the implementation of ST-301. Your task is to produce a detailed implementation plan. **DO NOT write or modify any code in this phase.**

---

## CRITICAL CONSTRAINTS (MUST FOLLOW)

### Forbidden Actions (PLAN phase)
- **NO file edits/writes/moves/deletes**
- **NO npm install / package modifications**
- **NO git commit/push**
- **NO network requests**
- **NO code execution beyond inspection**

### Allowed Actions (read-only inspection)
- `ls`, `find` — list files
- `cat`, `head`, `tail` — read file contents
- `rg`, `grep` — search patterns
- `git status`, `git diff` — inspect repo state

### STOP-THE-LINE Rule
If you discover missing information, ambiguity, or blockers → **STOP and request clarification**. Do not invent details.

---

## Sources of Truth (MUST READ)

Before planning, read and reference these files:

| File | Purpose |
|------|---------|
| `docs/planning/workpacks/ST-301/workpack.md` | Implementation plan, files to change |
| `docs/planning/epics/EP-004/stories/ST-301-oidc-integration.md` | Story spec, AC |
| `docs/planning/epics/EP-004/epic.md` | Epic context, decisions, Keycloak config |
| `clients/web/package.json` | Current dependencies |
| `clients/web/src/routes/index.tsx` | Current routing |
| `clients/web/src/context/AuthContext.tsx` | Current auth implementation |
| `clients/web/.env.example` | Current env vars |
| `clients/web/src/vite-env.d.ts` | Current type definitions |

---

## Story Context

**Goal:** Integrate Keycloak OIDC into web client using `oidc-client-ts` with PKCE flow.

**In Scope:**
- Add `oidc-client-ts` npm dependency
- Create `src/lib/auth/oidc.ts` with UserManager configuration
- Create `src/routes/Callback.tsx` to handle OIDC redirect
- Add `/callback` route to router
- Add OIDC environment variables to `.env.example`
- Add type definitions to `vite-env.d.ts`

**Out of Scope (DO NOT PLAN):**
- Login.tsx UI changes (ST-302)
- Token refresh logic (ST-303)
- Error UX components (ST-303)
- Documentation updates (ST-304)

---

## Key Technical Decisions (Already Made)

### Session Storage (Fix A)
Use `sessionStorage` via `WebStorageStateStore`:
```typescript
userStore: new WebStorageStateStore({ store: window.sessionStorage })
```
Rationale: session persists across page refresh, clears on tab close.

### PKCE Flow
Use authorization code flow with PKCE (S256). Required config:
```typescript
response_type: 'code',
// oidc-client-ts enables PKCE by default for 'code' flow
```

### Environment Variables
```
VITE_AUTH_PROVIDER=keycloak
VITE_OIDC_AUTHORITY=http://localhost:8080/realms/hometusk
VITE_OIDC_CLIENT_ID=hometusk-web
VITE_OIDC_REDIRECT_URI=http://localhost:5173/callback
```

---

## Expected Deliverables (Files to Create/Modify)

| File | Action | Purpose |
|------|--------|---------|
| `clients/web/package.json` | MODIFY | Add `oidc-client-ts` dependency |
| `clients/web/src/lib/auth/oidc.ts` | CREATE | UserManager wrapper with signinRedirect, signinCallback |
| `clients/web/src/routes/Callback.tsx` | CREATE | Handle OIDC callback, redirect to /households |
| `clients/web/src/routes/index.tsx` | MODIFY | Add /callback route |
| `clients/web/.env.example` | MODIFY | Add VITE_OIDC_* variables |
| `clients/web/src/vite-env.d.ts` | MODIFY | Add type definitions for VITE_OIDC_* |

---

## Acceptance Criteria (for verification)

```gherkin
Given the web application is loaded
And VITE_AUTH_PROVIDER=keycloak is set
When user initiates login
Then browser redirects to Keycloak authorization endpoint
And redirect includes PKCE code_challenge

Given user completes Keycloak login
When Keycloak redirects to /callback
Then application exchanges code for tokens via PKCE
And access_token is obtained
And user is redirected to /households

Given OIDC config is invalid or Keycloak unavailable
When login is initiated
Then clear error is logged (no crash)
And user sees error state (handled by ST-303)
```

---

## Your Task: Produce Implementation Plan

1. **Read** all Sources of Truth files listed above
2. **Analyze** current codebase structure (routing, auth context, types)
3. **Produce** detailed implementation plan with:
   - Step-by-step changes for each file
   - Exact code snippets to add/modify
   - Import statements
   - Type definitions
   - Error handling approach
4. **Verify** plan against AC and Out of Scope constraints
5. **Output** structured plan in Markdown format

---

## Output Format

```markdown
# Implementation Plan: ST-301

## Pre-flight Checks
- [ ] Verified oidc-client-ts is not already installed
- [ ] Verified /callback route doesn't exist
- [ ] Verified lib/auth/oidc.ts doesn't exist

## Step 1: Add oidc-client-ts dependency
**File:** package.json
**Change:** Add to dependencies
```json
"oidc-client-ts": "^3.0.0"
```

## Step 2: Create OIDC module
**File:** src/lib/auth/oidc.ts
**Action:** CREATE
```typescript
// Full code here
```

## Step 3: Create Callback route
**File:** src/routes/Callback.tsx
**Action:** CREATE
```typescript
// Full code here
```

## Step 4: Add route to router
**File:** src/routes/index.tsx
**Change:** Add import and route
```typescript
// Diff-style changes
```

## Step 5: Update env.example
**File:** .env.example
**Change:** Add OIDC variables

## Step 6: Update type definitions
**File:** src/vite-env.d.ts
**Change:** Add VITE_OIDC_* types

## Verification Checklist
- [ ] AC1: signinRedirect includes PKCE code_challenge
- [ ] AC2: Callback exchanges code for tokens
- [ ] AC3: Redirect to /households after success
- [ ] AC4: Errors logged, no crash

## Anti-Scope-Creep Verification
- [ ] No Login.tsx changes (ST-302)
- [ ] No token refresh logic (ST-303)
- [ ] No error UI components (ST-303)
```

---

## Remember
- **Read first, plan second**
- **No code execution** — this is planning only
- **Reference sources of truth** in your plan
- **STOP if blocked** — request clarification
