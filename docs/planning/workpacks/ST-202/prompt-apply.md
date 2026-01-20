# ST-202 APPLY Prompt

**Mode:** IMPLEMENTATION — Execute approved plan

---

## Context

You are implementing ST-202: Auth Integration + Household Selector for the HomeTusk web client.

**Read these files (mandatory):**
- `docs/planning/workpacks/ST-202/workpack.md` — implementation plan
- `docs/planning/epics/EP-003/stories/ST-202-auth-household.md` — story spec
- `docs/contracts/http/commands.openapi.yaml` — API contract
- Your approved PLAN output (if available)

**Prerequisite files (verify exist):**
- `clients/web/src/lib/api.ts` (apiFetch function - to extend with auth)
- `clients/web/src/routes/Login.tsx` (placeholder - to replace)
- `clients/web/src/routes/index.tsx` (router - to add /households route)
- `clients/web/src/App.tsx` (RouterProvider - to wrap with AuthProvider)

**ST-201 Baseline (Actual State):**
```
clients/web/src/lib/api.ts:
- Exports: apiFetch<T>(path, options)
- Has: Base URL from env, generic error handling
- Missing: Auth header injection, 401 handling, typed getMe()
```

**IMPORTANT:** Do NOT rewrite api.ts from scratch. EXTEND it:
1. Add auth header to existing apiFetch function
2. Add AuthError handling on 401
3. Add getMe() function using apiFetch

---

## Your Task

Implement auth integration according to the workpack.

**Deliverables:**
1. Auth context with login/logout/selectHousehold
2. API client with auth header
3. Login page with token paste
4. Household selector
5. Protected route wrapper

---

## Allowed Operations

### Files to create
- `clients/web/src/types/api.ts`
- `clients/web/src/lib/errors.ts`
- `clients/web/src/context/AuthContext.tsx`
- `clients/web/src/hooks/useAuth.ts`
- `clients/web/src/routes/HouseholdSelector.tsx`
- `clients/web/src/components/ProtectedRoute.tsx`
- `clients/web/src/components/HouseholdCard.tsx`

### Files to modify
- `clients/web/src/lib/api.ts`
- `clients/web/src/routes/Login.tsx`
- `clients/web/src/routes/index.tsx`
- `clients/web/src/App.tsx`

### Commands allowed
- `npm run lint`
- `npm run build`
- `npm run dev`

### Forbidden
- **DO NOT create** new backend endpoints
- **DO NOT modify** backend code
- **DO NOT implement** Keycloak OIDC (defer)
- **DO NOT add** token refresh logic (defer)

---

## Key Implementations

### 1. Types (src/types/api.ts)

**From OpenAPI contract:**
```typescript
export type AuthErrorCode = 'AUTH_TOKEN_MISSING' | 'AUTH_TOKEN_INVALID' | 'AUTH_TOKEN_EXPIRED';

export interface AuthErrorResponse {
  errorCode: AuthErrorCode;
  message: string;
}

export type HouseholdRole = 'admin' | 'member';

export interface HouseholdSummary {
  id: string;        // uuid
  name: string;
  role: HouseholdRole;
}

export interface UserProfile {
  id: string;        // uuid
  externalId: string;
  email: string;
  displayName: string;
  avatarUrl?: string;
  households: HouseholdSummary[];
  createdAt: string; // date-time (ISO 8601)
}
```

**Notes:**
- OpenAPI doesn't mark fields as required; treat all as required in app logic
- Guard for missing values with fallbacks (e.g., `households: []`)

### 2. Error Classes (src/lib/errors.ts)
```typescript
import type { AuthErrorCode } from '../types/api';

export class AuthError extends Error {
  constructor(
    message: string,
    public code?: AuthErrorCode
  ) {
    super(message);
    this.name = 'AuthError';
  }
}

export class ApiError extends Error {
  constructor(
    public status: number,
    public body: unknown,
    message?: string
  ) {
    super(message || `API Error: ${status}`);
    this.name = 'ApiError';
  }
}
```

### 3. API Client Updates (src/lib/api.ts)

**CRITICAL:** EXTEND the existing api.ts, do NOT rewrite from scratch.

**Storage key constant:**
```typescript
const AUTH_TOKEN_KEY = 'hometusk_auth_token';
```

**Current state (from ST-201):**
```typescript
export type ApiOptions = {
  method?: string;
  body?: unknown;
  headers?: HeadersInit;
};

export async function apiFetch<T>(path: string, options: ApiOptions = {}): Promise<T> {
  // existing implementation
}
```

**Changes to make:**

1. Add imports at top:
```typescript
import { AuthError, ApiError } from './errors';
import type { UserProfile, AuthErrorResponse } from '../types/api';
```

2. Add storage constant:
```typescript
const AUTH_TOKEN_KEY = 'hometusk_auth_token';
```

3. Modify existing apiFetch function:
   - Add token retrieval: `const token = localStorage.getItem(AUTH_TOKEN_KEY)`
   - Add Authorization header if token exists:
     ```typescript
     const headers: HeadersInit = {
       ...(options.body !== undefined ? { 'Content-Type': 'application/json' } : {}),
       ...(token && { Authorization: `Bearer ${token}` }),  // ADD THIS
       ...options.headers,
     };
     ```
   - Replace generic error handling with typed 401 handling:
     ```typescript
     // BEFORE:
     if (!response.ok) {
       throw new Error(`Request failed with status ${response.status}`);
     }

     // AFTER:
     if (response.status === 401) {
       // Try to parse AuthErrorResponse
       const errorBody = await response.json().catch(() => null) as AuthErrorResponse | null;
       throw new AuthError(
         errorBody?.message || 'Unauthorized',
         errorBody?.errorCode
       );
     }

     if (!response.ok) {
       const body = await response.json().catch(() => ({}));
       throw new ApiError(response.status, body);
     }
     ```

4. Add getMe() function at the end of the file:
```typescript
export async function getMe(): Promise<UserProfile> {
  return apiFetch<UserProfile>('/users/me');
}
```

**Result:** api.ts will export both apiFetch and getMe, with auth header injection.

---

### 4. Auth Context (src/context/AuthContext.tsx)

**Storage keys (constants):**
```typescript
const AUTH_TOKEN_KEY = 'hometusk_auth_token';
const HOUSEHOLD_ID_KEY = 'hometusk_household_id';
```

**Implementation:**
```typescript
import { createContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { getMe } from '../lib/api';
import { AuthError } from '../lib/errors';
import type { UserProfile } from '../types/api';

const AUTH_TOKEN_KEY = 'hometusk_auth_token';
const HOUSEHOLD_ID_KEY = 'hometusk_household_id';

type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'unauthenticated';

interface AuthContextType {
  status: AuthStatus;
  isAuthenticated: boolean; // derived: status === 'authenticated'
  user: UserProfile | null;
  token: string | null;
  householdId: string | null;
  login: (token: string) => Promise<void>;
  logout: () => void;
  selectHousehold: (id: string) => void;
}

export const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem(AUTH_TOKEN_KEY)
  );
  const [user, setUser] = useState<UserProfile | null>(null);
  const [householdId, setHouseholdId] = useState<string | null>(() =>
    localStorage.getItem(HOUSEHOLD_ID_KEY)
  );

  const logout = useCallback(() => {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(HOUSEHOLD_ID_KEY);
    setToken(null);
    setUser(null);
    setHouseholdId(null);
    setStatus('unauthenticated');
  }, []);

  const login = useCallback(async (newToken: string) => {
    setStatus('loading');
    localStorage.setItem(AUTH_TOKEN_KEY, newToken);
    setToken(newToken);

    try {
      const profile = await getMe();
      setUser(profile);
      setStatus('authenticated');

      // Auto-select if single household
      if (profile.households.length === 1) {
        const hid = profile.households[0].id;
        localStorage.setItem(HOUSEHOLD_ID_KEY, hid);
        setHouseholdId(hid);
      }
    } catch (error) {
      logout();
      throw error;
    }
  }, [logout]);

  const selectHousehold = useCallback((id: string) => {
    localStorage.setItem(HOUSEHOLD_ID_KEY, id);
    setHouseholdId(id);
  }, []);

  // Load user on mount if token exists
  useEffect(() => {
    if (token && !user) {
      getMe()
        .then((profile) => {
          setUser(profile);
          setStatus('authenticated');
        })
        .catch((error) => {
          if (error instanceof AuthError) {
            logout();
          } else {
            setStatus('unauthenticated');
          }
        });
    } else if (!token) {
      setStatus('unauthenticated');
    }
  }, [token, user, logout]);

  const isAuthenticated = status === 'authenticated';

  return (
    <AuthContext.Provider
      value={{
        status,
        isAuthenticated,
        user,
        token,
        householdId,
        login,
        logout,
        selectHousehold,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
```

### 5. useAuth Hook (src/hooks/useAuth.ts)
```typescript
import { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
```

### 6. Protected Route (src/components/ProtectedRoute.tsx)

**Props:**
```typescript
interface ProtectedRouteProps {
  requireHousehold?: boolean; // default: false
}
```

**Implementation:**
```typescript
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

interface ProtectedRouteProps {
  requireHousehold?: boolean;
}

export function ProtectedRoute({ requireHousehold = false }: ProtectedRouteProps) {
  const { status, isAuthenticated, householdId } = useAuth();
  const location = useLocation();

  // Loading state - prevent flash redirects
  if (status === 'loading') {
    return <div className="page">Loading...</div>;
  }

  // Not authenticated - redirect to login
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Authenticated but household required and not selected
  if (requireHousehold && !householdId) {
    return <Navigate to="/households" replace />;
  }

  return <Outlet />;
}
```

### 7. Login Page (src/routes/Login.tsx)

**Replace existing placeholder with:**
```typescript
import { useState, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function Login() {
  const [token, setToken] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login, user } = useAuth();
  const navigate = useNavigate();

  // Dev-only guard
  const authProvider = import.meta.env.VITE_AUTH_PROVIDER;
  if (authProvider !== 'dev') {
    return (
      <div className="page">
        <h1>Authentication Not Available</h1>
        <p>VITE_AUTH_PROVIDER must be 'dev' for token paste login.</p>
        <p>Current: {authProvider || 'not set'}</p>
      </div>
    );
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login(token.trim());

      // IMPORTANT: After login(), the auth context is updated but local `user` state
      // may not reflect the new value immediately due to React's async state updates.
      // The navigation logic relies on auto-select happening in AuthContext.login().
      // Strategy: If login succeeds, check user.households.length after context updates.

      // Option 1 (simpler): Always navigate to /households, let HouseholdSelector auto-redirect
      navigate('/households');

      // Option 2 (explicit): Make login() return UserProfile and use that for navigation
      // const profile = await login(token.trim());
      // if (profile.households.length === 1) {
      //   navigate(`/households/${profile.households[0].id}/tasks`);
      // } else {
      //   navigate('/households');
      // }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page">
      <h1>Login (Dev Mode)</h1>
      <p>Paste your JWT token to authenticate.</p>

      <form onSubmit={handleSubmit} className="card">
        <label>
          JWT Token:
          <textarea
            value={token}
            onChange={(e) => setToken(e.target.value)}
            placeholder="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            rows={6}
            disabled={loading}
            required
          />
        </label>

        {error && <div className="error">{error}</div>}

        <button type="submit" disabled={loading || !token.trim()}>
          {loading ? 'Logging in...' : 'Login'}
        </button>
      </form>
    </div>
  );
}
```

### 8. Household Selector (src/routes/HouseholdSelector.tsx)

```typescript
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import HouseholdCard from '../components/HouseholdCard';

export default function HouseholdSelector() {
  const { user, selectHousehold } = useAuth();
  const navigate = useNavigate();

  // Auto-select if only one household
  useEffect(() => {
    if (user && user.households.length === 1) {
      const hid = user.households[0].id;
      selectHousehold(hid);
      navigate(`/households/${hid}/tasks`, { replace: true });
    }
  }, [user, selectHousehold, navigate]);

  if (!user || user.households.length === 0) {
    return (
      <div className="page">
        <h1>No Households</h1>
        <p>You are not a member of any households yet.</p>
      </div>
    );
  }

  const handleSelect = (id: string) => {
    selectHousehold(id);
    navigate(`/households/${id}/tasks`);
  };

  return (
    <div className="page">
      <h1>Select Household</h1>
      <p>Choose a household to continue:</p>

      <div className="household-list">
        {user.households.map((household) => (
          <HouseholdCard
            key={household.id}
            household={household}
            onSelect={() => handleSelect(household.id)}
          />
        ))}
      </div>
    </div>
  );
}
```

### 9. Household Card (src/components/HouseholdCard.tsx)

```typescript
import type { HouseholdSummary } from '../types/api';

interface HouseholdCardProps {
  household: HouseholdSummary;
  onSelect: () => void;
}

export default function HouseholdCard({ household, onSelect }: HouseholdCardProps) {
  return (
    <div className="card household-card" onClick={onSelect}>
      <h3>{household.name}</h3>
      <p className="chip">{household.role}</p>
      <button type="button">Select</button>
    </div>
  );
}
```

### 10. Route Updates (src/routes/index.tsx)

**Modify existing router:**
```typescript
import { Navigate, createBrowserRouter } from 'react-router-dom';
import { ProtectedRoute } from '../components/ProtectedRoute';
import HouseholdLayout from './HouseholdLayout';
import HouseholdSelector from './HouseholdSelector';
import Login from './Login';
// ... other imports

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/login" replace /> },
  { path: '/login', element: <Login /> },

  // Household selector (protected, no household required)
  {
    path: '/households',
    element: <ProtectedRoute />,
    children: [
      { index: true, element: <HouseholdSelector /> },
    ],
  },

  // Household routes (protected + require household)
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

### 11. App.tsx Update

**Wrap RouterProvider with AuthProvider:**
```typescript
import { RouterProvider } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { router } from './routes';

export default function App() {
  return (
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
  );
}
```

---

## Acceptance Criteria Verification

After implementation, verify:
- [ ] AC1: useAuth() returns correct state
- [ ] AC2: Token paste → login → redirect
- [ ] AC3: API calls have Authorization header
- [ ] AC4: GET /users/me works, 401 logs out
- [ ] AC5: Household selector shows list
- [ ] AC6: Single household auto-selects
- [ ] AC7: Protected routes redirect

---

## STOP-THE-LINE

If you encounter:
- Missing ST-201 files
- API connection issues
- TypeScript errors that need architectural decisions

**STOP and report** — do not proceed with workarounds.

---

## Report Format

After completion:
```markdown
# ST-202 Implementation Report

## Files Created
- [list]

## Files Modified
- [list]

## Verification
- npm run lint: PASS/FAIL
- npm run build: PASS/FAIL
- Manual auth flow: PASS/FAIL

## AC Status
- AC1: PASS/FAIL
- AC2: PASS/FAIL
- AC3: PASS/FAIL
- AC4: PASS/FAIL
- AC5: PASS/FAIL
- AC6: PASS/FAIL
- AC7: PASS/FAIL

## Issues
- [any issues]
```
