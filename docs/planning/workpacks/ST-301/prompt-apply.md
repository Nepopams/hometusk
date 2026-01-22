# Codex APPLY Prompt: ST-301 — Keycloak OIDC Integration

## Role
You are a developer implementing ST-301. Execute the approved implementation plan below. **DO NOT deviate from the plan without explicit approval.**

---

## CRITICAL CONSTRAINTS (MUST FOLLOW)

### Allowed Actions
- Create/modify files listed in "Files to Change"
- Run `npm install` in clients/web
- Run `npm run build`, `npm run lint` for verification

### Forbidden Actions
- **NO modifications to AuthContext.tsx** (ST-303 scope)
- **NO modifications to Login.tsx** (ST-302 scope)
- **NO error UI components** (ST-303 scope)
- **NO token refresh logic** (ST-303 scope)
- **NO git commit/push** (human gate)
- **NO other files** beyond listed scope

### STOP-THE-LINE Rule
If you encounter blockers, missing dependencies, or need to deviate from plan → **STOP and request clarification**. Do not improvise.

---

## Sources of Truth

| File | Purpose |
|------|---------|
| `docs/planning/workpacks/ST-301/workpack.md` | Implementation plan |
| `docs/planning/epics/EP-004/stories/ST-301-oidc-integration.md` | Story spec, AC |
| `docs/planning/epics/EP-004/epic.md` | Epic context, decisions |

---

## Approved Implementation Plan

### Step 1: Add oidc-client-ts dependency

**Command:**
```bash
cd clients/web && npm install oidc-client-ts@^3.0.0
```

**Expected Result:**
- `package.json` dependencies include `"oidc-client-ts": "^3.x.x"`
- `package-lock.json` updated
- No npm errors

---

### Step 2: Create OIDC module

**File:** `clients/web/src/lib/auth/oidc.ts`
**Action:** CREATE

```typescript
import { UserManager, WebStorageStateStore, type User } from 'oidc-client-ts';

const authority = import.meta.env.VITE_OIDC_AUTHORITY;
const clientId = import.meta.env.VITE_OIDC_CLIENT_ID;
const redirectUri = import.meta.env.VITE_OIDC_REDIRECT_URI;

// Validate required env vars
function validateConfig(): boolean {
  if (!authority || !clientId || !redirectUri) {
    console.error('[OIDC] Missing required environment variables:', {
      VITE_OIDC_AUTHORITY: !!authority,
      VITE_OIDC_CLIENT_ID: !!clientId,
      VITE_OIDC_REDIRECT_URI: !!redirectUri,
    });
    return false;
  }
  return true;
}

// Create UserManager only if config is valid
let userManager: UserManager | null = null;

function getUserManager(): UserManager | null {
  if (userManager) return userManager;

  if (!validateConfig()) return null;

  userManager = new UserManager({
    authority,
    client_id: clientId,
    redirect_uri: redirectUri,
    response_type: 'code', // PKCE enabled by default for 'code' flow
    scope: 'openid profile',
    userStore: new WebStorageStateStore({ store: window.sessionStorage }),
    // Post-logout redirect (optional, can be added later)
    // post_logout_redirect_uri: window.location.origin,
  });

  return userManager;
}

/**
 * Initiates OIDC sign-in redirect to Keycloak.
 * Throws if OIDC config is invalid.
 */
export async function signinRedirect(): Promise<void> {
  const um = getUserManager();
  if (!um) {
    throw new Error('OIDC configuration is invalid. Check environment variables.');
  }
  await um.signinRedirect();
}

/**
 * Handles OIDC callback after Keycloak redirect.
 * Returns User object with tokens on success.
 * Throws on error.
 */
export async function signinCallback(): Promise<User> {
  const um = getUserManager();
  if (!um) {
    throw new Error('OIDC configuration is invalid. Check environment variables.');
  }
  return um.signinRedirectCallback();
}

/**
 * Check if OIDC is configured and available.
 */
export function isOidcConfigured(): boolean {
  return validateConfig();
}
```

**Notes:**
- Uses `sessionStorage` for OIDC state (Fix A decision)
- PKCE is enabled by default for `response_type: 'code'`
- Lazy initialization of UserManager
- Graceful handling of missing env vars

---

### Step 3: Create Callback route

**File:** `clients/web/src/routes/Callback.tsx`
**Action:** CREATE

```typescript
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { signinCallback } from '../lib/auth/oidc';

// Use same key as AuthContext for token bridging
const AUTH_TOKEN_KEY = 'hometusk_auth_token';

type CallbackStatus = 'processing' | 'success' | 'error';

export default function Callback() {
  const navigate = useNavigate();
  const [status, setStatus] = useState<CallbackStatus>('processing');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    async function handleCallback() {
      try {
        const user = await signinCallback();

        if (!isMounted) return;

        if (user.access_token) {
          // Bridge to existing AuthContext: save token to localStorage
          // AuthContext reads from this key on mount
          localStorage.setItem(AUTH_TOKEN_KEY, user.access_token);
          setStatus('success');
          // Redirect to households - AuthContext will pick up the token
          navigate('/households', { replace: true });
        } else {
          throw new Error('No access token received');
        }
      } catch (err) {
        if (!isMounted) return;

        const message = err instanceof Error ? err.message : 'Unknown error';
        console.error('[Callback] OIDC callback failed:', message);
        setError(message);
        setStatus('error');
      }
    }

    handleCallback();

    return () => {
      isMounted = false;
    };
  }, [navigate]);

  // Minimal UI - error UX is ST-303 scope
  if (status === 'processing') {
    return (
      <div style={{ padding: '2rem', textAlign: 'center' }}>
        <p>Processing login...</p>
      </div>
    );
  }

  if (status === 'error') {
    return (
      <div style={{ padding: '2rem', textAlign: 'center' }}>
        <p>Login failed: {error}</p>
        <p>
          <a href="/login">Return to login</a>
        </p>
      </div>
    );
  }

  // Success state - should redirect immediately
  return null;
}
```

**Notes:**
- Uses `AUTH_TOKEN_KEY = 'hometusk_auth_token'` — same key AuthContext reads
- This is NOT AuthContext modification, it's using the existing localStorage contract
- Minimal error UI (detailed error UX is ST-303)
- Cleanup on unmount to prevent memory leaks

---

### Step 4: Add /callback route

**File:** `clients/web/src/routes/index.tsx`
**Action:** MODIFY

**Add import at top (after existing imports):**
```typescript
import Callback from './Callback';
```

**Add route (after `/login` route, before `/households`):**
```typescript
{ path: '/callback', element: <Callback /> },
```

**Full modified file:**
```typescript
import { Navigate, createBrowserRouter } from 'react-router-dom';
import { ProtectedRoute } from '../components/ProtectedRoute';
import Callback from './Callback';
import HouseholdLayout from './HouseholdLayout';
import HouseholdSelector from './HouseholdSelector';
import Login from './Login';
import Notifications from './Notifications';
import NotFound from './NotFound';
import TaskDetail from './TaskDetail';
import TasksList from './TasksList';
import ZonesList from './ZonesList';

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/login" replace /> },
  { path: '/login', element: <Login /> },
  { path: '/callback', element: <Callback /> },
  {
    path: '/households',
    element: <ProtectedRoute />,
    children: [{ index: true, element: <HouseholdSelector /> }],
  },
  {
    path: '/households/:householdId',
    element: <ProtectedRoute requireHousehold />,
    children: [
      {
        element: <HouseholdLayout />,
        children: [
          { index: true, element: <Navigate to="tasks" replace /> },
          { path: 'tasks', element: <TasksList /> },
          { path: 'tasks/:taskId', element: <TaskDetail /> },
          { path: 'zones', element: <ZonesList /> },
          { path: 'notifications', element: <Notifications /> },
        ],
      },
    ],
  },
  { path: '*', element: <NotFound /> },
]);
```

---

### Step 5: Update .env.example

**File:** `clients/web/.env.example`
**Action:** MODIFY

**Add these lines:**
```bash
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_AUTH_PROVIDER=dev

# OIDC Configuration (required when VITE_AUTH_PROVIDER=keycloak)
VITE_OIDC_AUTHORITY=http://localhost:8080/realms/hometusk
VITE_OIDC_CLIENT_ID=hometusk-web
VITE_OIDC_REDIRECT_URI=http://localhost:5173/callback
```

---

### Step 6: Update type definitions

**File:** `clients/web/src/vite-env.d.ts`
**Action:** MODIFY

**Full updated file:**
```typescript
/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  readonly VITE_AUTH_PROVIDER: string;
  // OIDC config (required when VITE_AUTH_PROVIDER=keycloak)
  readonly VITE_OIDC_AUTHORITY?: string;
  readonly VITE_OIDC_CLIENT_ID?: string;
  readonly VITE_OIDC_REDIRECT_URI?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
```

**Note:** OIDC vars are optional (?) because dev mode doesn't require them.

---

## Verification Commands

After implementation, run:

```bash
# Install dependencies
cd clients/web && npm install

# Build passes (type checking + Vite build)
cd clients/web && npm run build

# Lint passes
cd clients/web && npm run lint

# Dev server starts without errors
cd clients/web && npm run dev
```

---

## Verification Checklist

- [ ] `oidc-client-ts` appears in package.json dependencies
- [ ] `src/lib/auth/oidc.ts` exists with `signinRedirect`, `signinCallback` exports
- [ ] `src/routes/Callback.tsx` exists
- [ ] `/callback` route added to router
- [ ] `.env.example` has VITE_OIDC_* variables
- [ ] `vite-env.d.ts` has OIDC type definitions
- [ ] `npm run build` passes
- [ ] `npm run lint` passes
- [ ] AuthContext.tsx NOT modified
- [ ] Login.tsx NOT modified

---

## Anti-Scope-Creep Verification

Before completing, verify you did NOT:
- [ ] Modify AuthContext.tsx (ST-303)
- [ ] Modify Login.tsx (ST-302)
- [ ] Add token refresh logic (ST-303)
- [ ] Add error UI components beyond minimal (ST-303)
- [ ] Add documentation (ST-304)

---

## Clarification (Pre-answered)

**Q: Can we add minimal token sync from OIDC to AuthContext?**

**A: No AuthContext.tsx modification allowed.** Instead, use the existing localStorage contract:
- AuthContext reads token from `localStorage.getItem('hometusk_auth_token')`
- Callback.tsx saves token to that key after successful OIDC callback
- AuthContext picks it up on navigation to /households

This is NOT AuthContext modification — it's using the existing public interface.

---

## Output

After completion, provide:
1. List of files created/modified
2. Output of `npm run build`
3. Output of `npm run lint`
4. Confirmation of verification checklist
