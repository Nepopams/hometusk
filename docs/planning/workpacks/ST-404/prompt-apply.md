# APPLY Prompt: ST-404 — Accept Invite Flow

## Role
You are a development agent. Your task is to **implement** the changes described below.

## Critical Constraints
- **ONLY modify files listed in "Files to Change"**
- **NO new dependencies** (npm install forbidden)
- **Follow existing code patterns** (BEM CSS, React hooks, apiFetch pattern)
- If anything is unclear → STOP and ask

---

## Sources of Truth (reference only)
- Story: `docs/planning/epics/EP-005/stories/ST-404-accept-invite.md`
- Workpack: `docs/planning/workpacks/ST-404/workpack.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoD: `docs/_governance/dod.md`

---

## Files to Change

### 1. MODIFY: `clients/web/src/types/api.ts`

**Changes:**
Add after `CreateInviteResponse`:

```typescript
export interface InviteMembership {
  id: string;
  role: HouseholdRole;
  joinedAt: string;
}

export interface AcceptInviteResponse {
  membership: InviteMembership;
  household: Household;
}
```

---

### 2. MODIFY: `clients/web/src/lib/api.ts`

**Changes:**
1. Add `AcceptInviteResponse` to imports
2. Add function at end:

```typescript
export async function acceptInvite(inviteToken: string): Promise<AcceptInviteResponse> {
  return apiFetch<AcceptInviteResponse>('/invites/accept', {
    method: 'POST',
    body: { inviteToken },
  });
}
```

---

### 3. MODIFY: `clients/web/src/components/ProtectedRoute.tsx`

**Changes:**
Store redirect path in sessionStorage before redirecting to login (for OIDC flows where state is lost):

```typescript
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

const POST_LOGIN_REDIRECT_KEY = 'hometusk_post_login_redirect';

interface ProtectedRouteProps {
  requireHousehold?: boolean;
}

export function ProtectedRoute({ requireHousehold = false }: ProtectedRouteProps) {
  const { status, isAuthenticated, householdId, error } = useAuth();
  const location = useLocation();

  if (status === 'loading') {
    return <div className="page">Loading...</div>;
  }

  if (!isAuthenticated) {
    // Store intended destination for OIDC redirect flows
    const intendedPath = location.pathname + location.search;
    if (intendedPath !== '/login' && intendedPath !== '/') {
      sessionStorage.setItem(POST_LOGIN_REDIRECT_KEY, intendedPath);
    }
    
    const redirectPath = error ? `/login?error=${error}` : '/login';
    return <Navigate to={redirectPath} state={{ from: location }} replace />;
  }

  if (requireHousehold && !householdId) {
    return <Navigate to="/households" replace />;
  }

  return <Outlet />;
}

export { POST_LOGIN_REDIRECT_KEY };
```

---

### 4. MODIFY: `clients/web/src/routes/Callback.tsx`

**Changes:**
Check for stored redirect path and use it after successful login:

```typescript
import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { signinCallback } from '../lib/auth/oidc';
import { POST_LOGIN_REDIRECT_KEY } from '../components/ProtectedRoute';

type CallbackStatus = 'processing' | 'success' | 'error';

interface LocationState {
  from?: { pathname: string; search?: string };
}

export default function Callback() {
  const navigate = useNavigate();
  const location = useLocation();
  const [status, setStatus] = useState<CallbackStatus>('processing');

  useEffect(() => {
    let isMounted = true;

    async function handleCallback() {
      try {
        const user = await signinCallback();

        if (!isMounted) return;

        if (user.access_token) {
          setStatus('success');
          
          // Determine redirect destination
          const state = location.state as LocationState | null;
          const stateRedirect = state?.from 
            ? state.from.pathname + (state.from.search || '')
            : null;
          const storedRedirect = sessionStorage.getItem(POST_LOGIN_REDIRECT_KEY);
          
          // Clear stored redirect
          sessionStorage.removeItem(POST_LOGIN_REDIRECT_KEY);
          
          // Priority: state > sessionStorage > default
          const redirectTo = stateRedirect || storedRedirect || '/households';
          navigate(redirectTo, { replace: true });
        } else {
          throw new Error('No access token received');
        }
      } catch (err) {
        if (!isMounted) return;

        const message = err instanceof Error ? err.message : 'Unknown error';
        console.error('[Callback] OIDC callback failed:', message);
        setStatus('error');
        sessionStorage.removeItem(POST_LOGIN_REDIRECT_KEY);
        navigate('/login?error=auth_failed', { replace: true });
      }
    }

    handleCallback();

    return () => {
      isMounted = false;
    };
  }, [navigate, location.state]);

  if (status === 'processing') {
    return (
      <div style={{ padding: '2rem', textAlign: 'center' }}>
        <p>Processing login...</p>
      </div>
    );
  }

  return null;
}
```

---

### 5. CREATE: `clients/web/src/routes/AcceptInvite.tsx`

**Purpose:** Accept invite page that processes token from URL.

```typescript
import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { acceptInvite } from '../lib/api';
import { ApiError } from '../lib/errors';
import { useAuth } from '../hooks/useAuth';
import type { AcceptInviteResponse } from '../types/api';

type AcceptStatus = 'idle' | 'loading' | 'success' | 'error';

interface ErrorInfo {
  title: string;
  message: string;
}

function getErrorInfo(err: unknown): ErrorInfo {
  if (err instanceof ApiError) {
    if (err.status === 404) {
      return {
        title: 'Invalid invite link',
        message: 'This invite link is not valid. Please ask for a new invite.',
      };
    }
    if (err.status === 410) {
      return {
        title: 'Invite expired',
        message: 'This invite has expired or was already used. Please ask for a new invite.',
      };
    }
  }
  return {
    title: 'Something went wrong',
    message: 'Failed to accept invite. Please try again.',
  };
}

export default function AcceptInvite() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { selectHousehold, refetchUser } = useAuth();

  const token = searchParams.get('token');

  const [status, setStatus] = useState<AcceptStatus>('idle');
  const [error, setError] = useState<ErrorInfo | null>(null);
  const [response, setResponse] = useState<AcceptInviteResponse | null>(null);

  useEffect(() => {
    if (!token) {
      setStatus('error');
      setError({
        title: 'Invalid invite link',
        message: 'No invite token found in the URL.',
      });
      return;
    }

    let isMounted = true;

    async function processInvite() {
      setStatus('loading');

      try {
        const result = await acceptInvite(token);
        
        if (!isMounted) return;

        setResponse(result);
        setStatus('success');

        // Refresh user profile to include new household
        await refetchUser();
        
        // Select the new household
        selectHousehold(result.household.id);

        // Redirect to household dashboard
        navigate(`/households/${result.household.id}/tasks`, { replace: true });
      } catch (err) {
        if (!isMounted) return;

        setStatus('error');
        setError(getErrorInfo(err));
      }
    }

    processInvite();

    return () => {
      isMounted = false;
    };
  }, [token, navigate, selectHousehold, refetchUser]);

  return (
    <div className="page accept-invite">
      {status === 'loading' && (
        <div className="accept-invite__card">
          <h1>Accepting invite...</h1>
          <p>Please wait while we process your invite.</p>
        </div>
      )}

      {status === 'success' && response && (
        <div className="accept-invite__card accept-invite__card--success">
          <h1>Welcome!</h1>
          <p>You have joined <strong>{response.household.name}</strong>.</p>
          <p>Redirecting to your dashboard...</p>
        </div>
      )}

      {status === 'error' && error && (
        <div className="accept-invite__card accept-invite__card--error">
          <h1>{error.title}</h1>
          <p>{error.message}</p>
          <Link to="/households" className="button">
            Back to Home
          </Link>
        </div>
      )}
    </div>
  );
}
```

---

### 6. MODIFY: `clients/web/src/routes/index.tsx`

**Changes:**
1. Add import at top:
```typescript
import AcceptInvite from './AcceptInvite';
```

2. Add `/invite` route under ProtectedRoute (after `/households` routes):

```typescript
{
  path: '/invite',
  element: <ProtectedRoute />,
  children: [{ index: true, element: <AcceptInvite /> }],
},
```

Full routes array should look like:
```typescript
export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/login" replace /> },
  { path: '/login', element: <Login /> },
  { path: '/callback', element: <Callback /> },
  {
    path: '/households',
    element: <ProtectedRoute />,
    children: [
      { index: true, element: <HouseholdSelector /> },
      { path: 'new', element: <CreateHousehold /> },
    ],
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
  {
    path: '/invite',
    element: <ProtectedRoute />,
    children: [{ index: true, element: <AcceptInvite /> }],
  },
  { path: '*', element: <NotFound /> },
]);
```

---

### 7. MODIFY: `clients/web/src/styles/index.css`

**Add at end of file:**

```css
/* Accept Invite Page */
.accept-invite {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}

.accept-invite__card {
  text-align: center;
  padding: 48px;
  max-width: 400px;
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e7e1d7;
  box-shadow: 0 10px 24px rgba(18, 18, 18, 0.08);
}

.accept-invite__card h1 {
  margin: 0 0 12px;
  font-size: 1.5rem;
}

.accept-invite__card p {
  margin: 0 0 16px;
  color: #6a6257;
}

.accept-invite__card--success {
  border-color: #86efac;
  background: #f0fdf4;
}

.accept-invite__card--success h1 {
  color: #166534;
}

.accept-invite__card--error {
  border-color: #fecaca;
  background: #fef2f2;
}

.accept-invite__card--error h1 {
  color: #b91c1c;
}

.accept-invite__card .button {
  margin-top: 8px;
}
```

---

## Verification Commands

```bash
# In clients/web directory:
npm run lint
npm run build
```

---

## Manual Test Scenarios

1. **Happy path (valid token):**
   - Get invite link from ST-403 flow
   - Open in browser (logged in)
   - "Accepting invite..." shown
   - Success → redirected to household tasks
   - Header shows new household

2. **Happy path (not logged in):**
   - Open invite link in incognito
   - Redirected to login
   - Complete login
   - Returns to `/invite?token=xxx`
   - Accepts and joins household

3. **Invalid token (404):**
   - Open `/invite?token=invalid_xxx`
   - "Invalid invite link" error shown
   - "Back to Home" button works

4. **Expired/used token (410):**
   - Use same invite link twice
   - Second attempt shows "Invite expired" error

5. **Missing token:**
   - Open `/invite` without token
   - "Invalid invite link - No token found" error

---

## Anti-Scope-Creep

DO NOT:
- Add invite preview (show household name before accepting)
- Add decline functionality
- Add manual token input field
- Add guest accept
- Modify invite creation (ST-403)

---

## Commit Plan

1. **Commit 1**: Add InviteMembership + AcceptInviteResponse types, add acceptInvite API
2. **Commit 2**: Add redirect persistence to ProtectedRoute + Callback handling
3. **Commit 3**: Create AcceptInvite page + route + CSS
