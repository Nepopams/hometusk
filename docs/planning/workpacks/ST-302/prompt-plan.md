# Codex PLAN Prompt: ST-302 — Login & Registration Screens

## Role
You are a developer planning the implementation of ST-302. Your task is to produce a detailed implementation plan. **DO NOT write or modify any code in this phase.**

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
| `docs/planning/workpacks/ST-302/workpack.md` | Implementation plan, files to change |
| `docs/planning/epics/EP-004/stories/ST-302-login-registration-screens.md` | Story spec, AC |
| `docs/planning/epics/EP-004/epic.md` | Epic context, decisions (Fix B) |
| `clients/web/src/routes/Login.tsx` | Current implementation (dev mode only) |
| `clients/web/src/lib/auth/oidc.ts` | OIDC module from ST-301 |
| `clients/web/src/styles/index.css` | Current global styles |

---

## Story Context

**Goal:** Update Login screen to support Keycloak OIDC with dual-mode (keycloak/dev).

**Dependency:** ST-301 complete (oidc.ts exists with `signinRedirect()`)

**In Scope:**
- Update Login.tsx with dual mode support
- "Sign in" button calls `signinRedirect()` (Keycloak mode)
- "Register" link calls same `signinRedirect()` (Fix B)
- Loading state during OIDC redirect
- Preserve dev mode (token paste)

**Out of Scope (DO NOT PLAN):**
- Token refresh logic (ST-303)
- Error handling UI beyond basic (ST-303)
- Forgot password link (NEXT)
- Social login buttons (LATER)

---

## Key Technical Decisions (Already Made)

### Registration Approach (Fix B)
**DO NOT use hardcoded `/registrations` URL** — it's version-dependent and may be unavailable.

**Correct approach:**
- Both "Sign in" and "Register" call `signinRedirect()`
- "Register" link includes UX guidance: "Don't have an account? Sign in to register"
- Keycloak login page shows "Register" option when realm has user registration enabled

```typescript
// Both buttons call the same function
const handleSignIn = () => signinRedirect();
const handleRegister = () => signinRedirect(); // same flow, different UX text
```

### Keycloak Realm Prerequisite
- "User registration" must be enabled in realm settings (Login tab)
- This is Keycloak admin configuration, not code

---

## Current Login.tsx Analysis

Current implementation:
- Only supports `VITE_AUTH_PROVIDER=dev`
- Shows "Authentication Not Available" for any other provider
- Token paste form with loading state
- Uses `useAuth()` hook for `login(token)` function

What needs to change:
- Add `keycloak` mode that shows "Sign in" button + "Register" link
- "Sign in" calls `signinRedirect()` from oidc.ts
- "Register" calls same `signinRedirect()` with different UX text
- Loading state during redirect
- Dev mode remains unchanged

---

## Expected Deliverables (Files to Modify)

| File | Action | Purpose |
|------|--------|---------|
| `clients/web/src/routes/Login.tsx` | MODIFY | Add Keycloak mode with dual UI |

**Note:** Only one file needs modification. No new files required.

---

## UI Specification

**Keycloak Mode:**
```
+-----------------------------+
|      HomeTusk               |
|                             |
|   Welcome back!             |
|                             |
|   +---------------------+   |
|   |     Sign in         |   |
|   +---------------------+   |
|                             |
|   Don't have an account?    |
|   Sign in to register →     |
|                             |
+-----------------------------+
```

**Loading State (during redirect):**
```
+-----------------------------+
|      HomeTusk               |
|                             |
|   Redirecting to login...   |
|                             |
+-----------------------------+
```

**Dev Mode (unchanged):**
```
+-----------------------------+
|      Login (Dev Mode)       |
|   Paste your JWT token...   |
|   [textarea]                |
|   [Login button]            |
+-----------------------------+
```

---

## Acceptance Criteria (for verification)

```gherkin
Given VITE_AUTH_PROVIDER=keycloak
When user visits /login
Then "Sign in" button is displayed
And "Register" link is displayed
And token paste form is NOT displayed

Given user clicks "Sign in"
Then loading indicator appears
And browser redirects to Keycloak login page

Given user clicks "Register"
Then browser redirects to Keycloak login page (same as "Sign in")

Given VITE_AUTH_PROVIDER=dev
When user visits /login
Then token paste form is displayed (existing behavior)
```

---

## Your Task: Produce Implementation Plan

1. **Read** all Sources of Truth files listed above
2. **Analyze** current Login.tsx structure and styling
3. **Produce** detailed implementation plan with:
   - Component structure (Keycloak mode vs Dev mode)
   - Event handlers (handleSignIn, handleRegister)
   - Loading state management
   - Exact code snippets to add/modify
   - Import statements
4. **Verify** plan against AC and Out of Scope constraints
5. **Output** structured plan in Markdown format

---

## Output Format

```markdown
# Implementation Plan: ST-302

## Pre-flight Checks
- [ ] Verified ST-301 complete (oidc.ts exists)
- [ ] Verified Login.tsx current structure
- [ ] Verified signinRedirect export from oidc.ts

## Step 1: Update Login.tsx imports
**Change:** Add import for signinRedirect
```typescript
import { signinRedirect } from '../lib/auth/oidc';
```

## Step 2: Refactor component structure
**Change:** Add Keycloak mode rendering
```typescript
// Keycloak mode UI code here
```

## Step 3: Add event handlers
**Change:** Add handleSignIn and handleRegister
```typescript
// Handler code here
```

## Step 4: Add loading state for OIDC
**Change:** Add isRedirecting state
```typescript
// Loading state code here
```

## Verification Checklist
- [ ] AC1: Keycloak mode shows Sign in + Register
- [ ] AC2: Sign in redirects to Keycloak
- [ ] AC3: Register redirects to Keycloak (same URL)
- [ ] AC4: Dev mode unchanged

## Anti-Scope-Creep Verification
- [ ] No token refresh logic (ST-303)
- [ ] No error UI components (ST-303)
- [ ] No forgot password link (NEXT)
```

---

## Remember
- **Read first, plan second**
- **No code execution** — this is planning only
- **Reference sources of truth** in your plan
- **STOP if blocked** — request clarification
- **Fix B is non-negotiable** — no hardcoded /registrations URL
