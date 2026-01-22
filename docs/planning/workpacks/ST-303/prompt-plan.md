# Codex PLAN Prompt: ST-303 — Session Management & Error UX

## Role
You are a developer planning the implementation of ST-303. Your task is to produce a detailed implementation plan. **DO NOT write or modify any code in this phase.**

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
| `docs/planning/workpacks/ST-303/workpack.md` | Implementation plan, files to change |
| `docs/planning/epics/EP-004/stories/ST-303-session-management.md` | Story spec, AC |
| `docs/planning/epics/EP-004/epic.md` | Epic context, decisions |
| `clients/web/src/context/AuthContext.tsx` | Current auth implementation |
| `clients/web/src/lib/api.ts` | Current API fetch with 401 handling |
| `clients/web/src/lib/auth/oidc.ts` | OIDC module from ST-301 |
| `clients/web/src/routes/Login.tsx` | Login screen from ST-302 |
| `clients/web/src/routes/Callback.tsx` | OIDC callback from ST-301 |
| `clients/web/src/components/ProtectedRoute.tsx` | Route protection |

---

## Story Context

**Goal:** Implement robust session management (token storage, refresh, logout) and clear error UX.

**Dependencies:** ST-301 (oidc.ts), ST-302 (Login.tsx)

**In Scope:**
- Refactor AuthContext for dual mode (keycloak/dev)
- Silent token refresh (accessTokenExpiring event)
- Logout with Keycloak end-session
- Global 401 handling in api.ts (auto-logout + redirect)
- Error display on Login screen (query param `?error=...`)

**Out of Scope (DO NOT PLAN):**
- Multi-device session management (LATER)
- Remember me / persistent sessions (LATER)
- Session timeout warning modal (LATER)

---

## Current Architecture Analysis

### Current Token Flow (Problem)
1. Callback.tsx saves token to `localStorage['hometusk_auth_token']`
2. AuthContext reads token from localStorage on mount
3. api.ts reads token from localStorage for each request
4. AuthContext.login() saves to localStorage (dev mode)

**Issue:** Token is scattered across localStorage reads. No centralized token management.

### Target Token Flow (Solution)
1. **Keycloak mode:**
   - AuthContext initializes from `userManager.getUser()` on mount
   - Token comes from User.access_token (in-memory via oidc-client-ts)
   - Silent refresh via `userManager.signinSilent()`
   - Logout via `userManager.signoutRedirect()`

2. **Dev mode:**
   - Keep current localStorage-based flow (unchanged)

3. **api.ts:**
   - Get token from exported `getAuthToken()` function
   - On 401, call exported `handleAuthError()` to trigger logout

---

## Key Technical Decisions

### 1. Token Source by Mode
```typescript
// Keycloak mode: token from UserManager
const user = await userManager.getUser();
const token = user?.access_token;

// Dev mode: token from localStorage (current behavior)
const token = localStorage.getItem('hometusk_auth_token');
```

### 2. Token Provider Pattern
Create a token provider that api.ts can use:
```typescript
// lib/auth/tokenProvider.ts
let tokenGetter: (() => string | null) = () => null;

export function setTokenGetter(getter: () => string | null) {
  tokenGetter = getter;
}

export function getAuthToken(): string | null {
  return tokenGetter();
}
```

### 3. Auth Error Callback Pattern
```typescript
// lib/auth/tokenProvider.ts
let authErrorHandler: (() => void) = () => {};

export function setAuthErrorHandler(handler: () => void) {
  authErrorHandler = handler;
}

export function handleAuthError(): void {
  authErrorHandler();
}
```

### 4. Silent Refresh
```typescript
// In oidc.ts, add:
userManager.events.addAccessTokenExpiring(() => {
  userManager.signinSilent().catch(() => {
    // Token refresh failed, redirect to login
    handleAuthError();
  });
});
```

### 5. Logout Flow (Keycloak)
```typescript
// Option A: Full Keycloak logout (redirects to Keycloak)
await userManager.signoutRedirect();

// Option B: Local-only logout (faster, doesn't clear Keycloak session)
await userManager.removeUser();
navigate('/login');
```

**Recommendation:** Start with Option B (local-only) for MVP simplicity. Can add full Keycloak logout later.

### 6. Error Display on Login
```typescript
// Login.tsx reads query param
const [searchParams] = useSearchParams();
const errorParam = searchParams.get('error');

// Redirect with error
navigate('/login?error=session_expired');
```

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `clients/web/src/lib/auth/tokenProvider.ts` | CREATE | Centralized token access for api.ts |
| `clients/web/src/lib/auth/oidc.ts` | MODIFY | Add getUser, signout, events |
| `clients/web/src/context/AuthContext.tsx` | MODIFY | Dual mode, token provider, refresh |
| `clients/web/src/lib/api.ts` | MODIFY | Use tokenProvider, call handleAuthError on 401 |
| `clients/web/src/routes/Login.tsx` | MODIFY | Show error from query param |
| `clients/web/src/routes/Callback.tsx` | MODIFY | Remove localStorage write (keycloak mode handles via UserManager) |

---

## Acceptance Criteria (for verification)

```gherkin
Given user is authenticated (keycloak mode)
When access token expires
Then silent refresh is attempted
And if successful, user continues without interruption
And if failed, user is redirected to /login?error=session_expired

Given user is authenticated
When user clicks Logout
Then local tokens are cleared
And user is redirected to /login

Given user makes API call
When backend returns 401
Then user is logged out
And redirected to /login?error=session_expired
And message "Session expired, please login again" is shown

Given VITE_AUTH_PROVIDER=dev
When user logs in with token paste
Then existing behavior unchanged
```

---

## Your Task: Produce Implementation Plan

1. **Read** all Sources of Truth files listed above
2. **Analyze** current AuthContext, api.ts, oidc.ts structure
3. **Produce** detailed implementation plan with:
   - Step-by-step changes for each file
   - Token provider pattern implementation
   - Event subscriptions for silent refresh
   - Error handling and display
   - Exact code snippets
4. **Verify** plan against AC and Out of Scope constraints
5. **Output** structured plan in Markdown format

---

## Output Format

```markdown
# Implementation Plan: ST-303

## Pre-flight Checks
- [ ] Verified ST-301 complete (oidc.ts exists)
- [ ] Verified ST-302 complete (Login.tsx with keycloak mode)
- [ ] Verified current AuthContext structure
- [ ] Verified current api.ts 401 handling

## Step 1: Create tokenProvider.ts
**File:** src/lib/auth/tokenProvider.ts
**Action:** CREATE
```typescript
// Token provider code
```

## Step 2: Extend oidc.ts
**File:** src/lib/auth/oidc.ts
**Action:** MODIFY
```typescript
// Add getUser, signout, setupEvents
```

## Step 3: Refactor AuthContext.tsx
**File:** src/context/AuthContext.tsx
**Action:** MODIFY
```typescript
// Dual mode support, token provider registration
```

## Step 4: Update api.ts
**File:** src/lib/api.ts
**Action:** MODIFY
```typescript
// Use tokenProvider, call handleAuthError
```

## Step 5: Update Login.tsx for errors
**File:** src/routes/Login.tsx
**Action:** MODIFY
```typescript
// Read error query param, show message
```

## Step 6: Update Callback.tsx
**File:** src/routes/Callback.tsx
**Action:** MODIFY
```typescript
// Remove localStorage bridge for keycloak mode
```

## Verification Checklist
- [ ] AC1: Silent refresh on token expiring
- [ ] AC2: Logout clears tokens and redirects
- [ ] AC3: 401 triggers auto-logout with error message
- [ ] AC4: Dev mode unchanged

## Anti-Scope-Creep Verification
- [ ] No "Remember me" (LATER)
- [ ] No session timeout warning modal (LATER)
- [ ] No multi-tab session sync (LATER)
```

---

## Remember
- **Read first, plan second**
- **No code execution** — this is planning only
- **Reference sources of truth** in your plan
- **STOP if blocked** — request clarification
- **Dual mode is required** — keycloak AND dev must work
