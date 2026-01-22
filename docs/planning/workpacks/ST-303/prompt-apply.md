# Codex APPLY Prompt: ST-303 — Session Management & Error UX

## Role
You are a developer implementing ST-303. Execute the approved implementation plan below. **DO NOT deviate from the plan without explicit approval.**

---

## CRITICAL CONSTRAINTS (MUST FOLLOW)

### Allowed Actions
- Create/modify files listed in "Files to Change"
- Run `npm run build`, `npm run lint` for verification

### Forbidden Actions
- **NO new npm dependencies** (oidc-client-ts already installed)
- **NO "Remember me" feature** (LATER scope)
- **NO session timeout warning modal** (LATER scope)
- **NO multi-tab session sync** (LATER scope)
- **NO git commit/push** (human gate)

### STOP-THE-LINE Rule
If you encounter blockers or need to deviate from plan → **STOP and request clarification**. Do not improvise.

---

## Blocking Question Answer

**Q: Silent refresh — refresh tokens or iframe silent renew?**

**A: Use Refresh Tokens (simpler for MVP)**

Configuration:
```typescript
// In UserManager config, add:
scope: 'openid profile offline_access',  // ADD offline_access
automaticSilentRenew: true,              // Enable automatic renewal
// No silent_redirect_uri needed
```

Keycloak prereq: Client must have refresh tokens enabled (default for public clients).

---

## Sources of Truth

| File | Purpose |
|------|---------|
| `docs/planning/workpacks/ST-303/workpack.md` | Implementation plan |
| `docs/planning/epics/EP-004/stories/ST-303-session-management.md` | Story spec, AC |

---

## Approved Implementation Plan

### Step 1: Create tokenProvider.ts

**File:** `clients/web/src/lib/auth/tokenProvider.ts`
**Action:** CREATE

```typescript
/**
 * Token Provider — centralized token access for api.ts
 * Allows AuthContext to register token getter and auth error handler
 */

let tokenGetter: () => string | null = () => null;
let authErrorHandler: (reason?: string) => void = () => {};

export function setTokenGetter(getter: () => string | null): void {
  tokenGetter = getter;
}

export function getAuthToken(): string | null {
  return tokenGetter();
}

export function setAuthErrorHandler(handler: (reason?: string) => void): void {
  authErrorHandler = handler;
}

export function handleAuthError(reason?: string): void {
  authErrorHandler(reason);
}
```

---

### Step 2: Extend oidc.ts

**File:** `clients/web/src/lib/auth/oidc.ts`
**Action:** MODIFY

**Changes:**
1. Update UserManager config for refresh tokens
2. Add getUser(), signinSilent(), signout(), removeUser()
3. Add event registration function

**Full updated file:**

```typescript
import { UserManager, WebStorageStateStore, type User } from 'oidc-client-ts';

const authority = import.meta.env.VITE_OIDC_AUTHORITY;
const clientId = import.meta.env.VITE_OIDC_CLIENT_ID;
const redirectUri = import.meta.env.VITE_OIDC_REDIRECT_URI;
const postLogoutRedirectUri = typeof window !== 'undefined' ? window.location.origin + '/login' : '';

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

let userManager: UserManager | null = null;

function getUserManager(): UserManager | null {
  if (userManager) return userManager;

  if (!validateConfig()) return null;

  userManager = new UserManager({
    authority: authority as string,
    client_id: clientId as string,
    redirect_uri: redirectUri as string,
    post_logout_redirect_uri: postLogoutRedirectUri,
    response_type: 'code',
    scope: 'openid profile offline_access', // offline_access for refresh tokens
    userStore: new WebStorageStateStore({ store: window.sessionStorage }),
    automaticSilentRenew: true, // Enable automatic token refresh
  });

  return userManager;
}

export async function signinRedirect(): Promise<void> {
  const manager = getUserManager();
  if (!manager) {
    throw new Error('OIDC configuration is invalid. Check environment variables.');
  }
  await manager.signinRedirect();
}

export async function signinCallback(): Promise<User> {
  const manager = getUserManager();
  if (!manager) {
    throw new Error('OIDC configuration is invalid. Check environment variables.');
  }
  return manager.signinRedirectCallback();
}

export function isOidcConfigured(): boolean {
  return validateConfig();
}

// NEW: Get current user from UserManager
export async function getUser(): Promise<User | null> {
  const manager = getUserManager();
  if (!manager) return null;
  return manager.getUser();
}

// NEW: Silent token refresh
export async function signinSilent(): Promise<User> {
  const manager = getUserManager();
  if (!manager) {
    throw new Error('OIDC configuration is invalid. Check environment variables.');
  }
  return manager.signinSilent();
}

// NEW: Full logout (redirects to Keycloak end-session)
export async function signoutRedirect(): Promise<void> {
  const manager = getUserManager();
  if (!manager) return;
  await manager.signoutRedirect();
}

// NEW: Local-only logout (clears user without Keycloak redirect)
export async function removeUser(): Promise<void> {
  const manager = getUserManager();
  if (!manager) return;
  await manager.removeUser();
}

// NEW: Register token event handlers
export function registerTokenEvents(
  onExpiring: () => void,
  onExpired: () => void,
  onSilentRenewError: (error: Error) => void
): () => void {
  const manager = getUserManager();
  if (!manager) return () => {};

  manager.events.addAccessTokenExpiring(onExpiring);
  manager.events.addAccessTokenExpired(onExpired);
  manager.events.addSilentRenewError(onSilentRenewError);

  // Return cleanup function
  return () => {
    manager.events.removeAccessTokenExpiring(onExpiring);
    manager.events.removeAccessTokenExpired(onExpired);
    manager.events.removeSilentRenewError(onSilentRenewError);
  };
}
```

---

### Step 3: Refactor AuthContext.tsx

**File:** `clients/web/src/context/AuthContext.tsx`
**Action:** MODIFY

**Full updated file:**

```typescript
import type { ReactNode } from 'react';
import { createContext, useCallback, useEffect, useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMe } from '../lib/api';
import { AuthError } from '../lib/errors';
import type { UserProfile } from '../types/api';
import { setTokenGetter, setAuthErrorHandler } from '../lib/auth/tokenProvider';
import {
  getUser as getOidcUser,
  removeUser as removeOidcUser,
  signoutRedirect,
  registerTokenEvents,
  signinSilent,
} from '../lib/auth/oidc';

const AUTH_TOKEN_KEY = 'hometusk_auth_token';
const HOUSEHOLD_ID_KEY = 'hometusk_household_id';

type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'unauthenticated';
type AuthErrorCode = 'session_expired' | 'auth_unavailable' | 'auth_failed' | null;

interface AuthContextType {
  status: AuthStatus;
  isAuthenticated: boolean;
  user: UserProfile | null;
  token: string | null;
  householdId: string | null;
  error: AuthErrorCode;
  login: (token: string) => Promise<void>;
  logout: () => void;
  selectHousehold: (id: string) => void;
  clearError: () => void;
}

export const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const authProvider = import.meta.env.VITE_AUTH_PROVIDER;
  const isKeycloakMode = authProvider === 'keycloak';

  const [status, setStatus] = useState<AuthStatus>('loading');
  const [token, setToken] = useState<string | null>(() =>
    isKeycloakMode ? null : localStorage.getItem(AUTH_TOKEN_KEY)
  );
  const [user, setUser] = useState<UserProfile | null>(null);
  const [householdId, setHouseholdId] = useState<string | null>(() =>
    localStorage.getItem(HOUSEHOLD_ID_KEY)
  );
  const [error, setError] = useState<AuthErrorCode>(null);

  const tokenRef = useRef<string | null>(token);
  tokenRef.current = token;

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  const logout = useCallback(async () => {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(HOUSEHOLD_ID_KEY);
    setToken(null);
    setUser(null);
    setHouseholdId(null);
    setStatus('unauthenticated');

    if (isKeycloakMode) {
      try {
        await removeOidcUser();
      } catch (err) {
        console.error('[Auth] Failed to remove OIDC user:', err);
      }
    }
  }, [isKeycloakMode]);

  const handleAuthErrorInternal = useCallback(
    (reason?: string) => {
      setError((reason as AuthErrorCode) ?? 'session_expired');
      logout();
    },
    [logout]
  );

  // Register token provider callbacks
  useEffect(() => {
    setTokenGetter(() => tokenRef.current);
    setAuthErrorHandler(handleAuthErrorInternal);
  }, [handleAuthErrorInternal]);

  // Keycloak mode: initialize from UserManager
  useEffect(() => {
    if (!isKeycloakMode) return;

    let isMounted = true;

    async function initKeycloakAuth() {
      try {
        const oidcUser = await getOidcUser();

        if (!isMounted) return;

        if (oidcUser?.access_token) {
          setToken(oidcUser.access_token);

          try {
            const profile = await getMe();
            if (!isMounted) return;

            setUser(profile);
            setStatus('authenticated');

            if (profile.households.length === 1) {
              const hid = profile.households[0].id;
              localStorage.setItem(HOUSEHOLD_ID_KEY, hid);
              setHouseholdId(hid);
            }
          } catch (err) {
            if (!isMounted) return;
            console.error('[Auth] Failed to fetch profile:', err);
            handleAuthErrorInternal('auth_failed');
          }
        } else {
          setStatus('unauthenticated');
        }
      } catch (err) {
        if (!isMounted) return;
        console.error('[Auth] Failed to get OIDC user:', err);
        setStatus('unauthenticated');
      }
    }

    initKeycloakAuth();

    return () => {
      isMounted = false;
    };
  }, [isKeycloakMode, handleAuthErrorInternal]);

  // Keycloak mode: register token events
  useEffect(() => {
    if (!isKeycloakMode) return;

    const cleanup = registerTokenEvents(
      // onExpiring
      () => {
        console.log('[Auth] Token expiring, attempting silent refresh...');
      },
      // onExpired
      () => {
        console.log('[Auth] Token expired');
        handleAuthErrorInternal('session_expired');
      },
      // onSilentRenewError
      (err) => {
        console.error('[Auth] Silent renew failed:', err);
        handleAuthErrorInternal('session_expired');
      }
    );

    return cleanup;
  }, [isKeycloakMode, handleAuthErrorInternal]);

  // Keycloak mode: update token when user changes
  useEffect(() => {
    if (!isKeycloakMode) return;

    const checkUserToken = async () => {
      const oidcUser = await getOidcUser();
      if (oidcUser?.access_token && oidcUser.access_token !== tokenRef.current) {
        setToken(oidcUser.access_token);
      }
    };

    // Check periodically for token updates from automatic silent renew
    const interval = setInterval(checkUserToken, 30000);
    return () => clearInterval(interval);
  }, [isKeycloakMode]);

  // Dev mode: login with token
  const login = useCallback(
    async (newToken: string) => {
      if (isKeycloakMode) {
        console.warn('[Auth] login() called in keycloak mode, ignoring');
        return;
      }

      setStatus('loading');
      clearError();
      localStorage.setItem(AUTH_TOKEN_KEY, newToken);
      setToken(newToken);

      try {
        const profile = await getMe();
        setUser(profile);
        setStatus('authenticated');

        if (profile.households.length === 1) {
          const hid = profile.households[0].id;
          localStorage.setItem(HOUSEHOLD_ID_KEY, hid);
          setHouseholdId(hid);
        }
      } catch (err) {
        logout();
        throw err;
      }
    },
    [isKeycloakMode, logout, clearError]
  );

  // Dev mode: initialize from localStorage
  useEffect(() => {
    if (isKeycloakMode) return;

    if (token && !user) {
      getMe()
        .then((profile) => {
          setUser(profile);
          setStatus('authenticated');
        })
        .catch((err) => {
          if (err instanceof AuthError) {
            logout();
          } else {
            setStatus('unauthenticated');
          }
        });
    } else if (!token) {
      setStatus('unauthenticated');
    }
  }, [token, user, logout, isKeycloakMode]);

  const selectHousehold = useCallback((id: string) => {
    localStorage.setItem(HOUSEHOLD_ID_KEY, id);
    setHouseholdId(id);
  }, []);

  const isAuthenticated = status === 'authenticated';

  return (
    <AuthContext.Provider
      value={{
        status,
        isAuthenticated,
        user,
        token,
        householdId,
        error,
        login,
        logout,
        selectHousehold,
        clearError,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
```

---

### Step 4: Update api.ts

**File:** `clients/web/src/lib/api.ts`
**Action:** MODIFY

**Changes:**
- Import from tokenProvider
- Replace localStorage read with getAuthToken()
- Call handleAuthError on 401

**Full updated file:**

```typescript
import { ApiError, AuthError } from './errors';
import { getAuthToken, handleAuthError } from './auth/tokenProvider';
import type {
  AuthErrorResponse,
  HouseholdMember,
  Task,
  TaskFilters,
  UserProfile,
  Zone,
} from '../types/api';

export type ApiOptions = {
  method?: string;
  body?: unknown;
  headers?: HeadersInit;
};

export async function apiFetch<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const baseUrl = import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '');
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const url = `${baseUrl}${normalizedPath}`;

  const token = getAuthToken();
  const headers: HeadersInit = {
    ...(options.body !== undefined ? { 'Content-Type': 'application/json' } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const response = await fetch(url, {
    method: options.method ?? 'GET',
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });

  if (response.status === 401) {
    const errorBody = (await response.json().catch(() => null)) as AuthErrorResponse | null;
    handleAuthError('session_expired');
    throw new AuthError(errorBody?.message || 'Unauthorized', errorBody?.errorCode);
  }

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new ApiError(response.status, body);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export async function getMe(): Promise<UserProfile> {
  return apiFetch<UserProfile>('/users/me');
}

function buildQueryString(filters: TaskFilters): string {
  const params = new URLSearchParams();

  if (filters.status) params.set('status', filters.status);
  if (filters.assigneeId) params.set('assigneeId', filters.assigneeId);
  if (filters.zoneId) params.set('zoneId', filters.zoneId);

  const query = params.toString();
  return query ? `?${query}` : '';
}

export async function getTasks(householdId: string, filters: TaskFilters = {}): Promise<Task[]> {
  const query = buildQueryString(filters);
  return apiFetch<Task[]>(`/households/${householdId}/tasks${query}`);
}

export async function getZones(householdId: string): Promise<Zone[]> {
  return apiFetch<Zone[]>(`/households/${householdId}/zones`);
}

export async function getMembers(householdId: string): Promise<HouseholdMember[]> {
  return apiFetch<HouseholdMember[]>(`/households/${householdId}/members`);
}
```

---

### Step 5: Update Login.tsx for error display

**File:** `clients/web/src/routes/Login.tsx`
**Action:** MODIFY

**Changes:**
- Add useSearchParams import
- Read error from query param
- Display error message
- Clear error on login attempt

**Full updated file:**

```typescript
import { useState, type FormEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { signinRedirect } from '../lib/auth/oidc';
import { useAuth } from '../hooks/useAuth';

const ERROR_MESSAGES: Record<string, string> = {
  session_expired: 'Session expired, please login again.',
  auth_unavailable: 'Authentication service unavailable. Please try again later.',
  auth_failed: 'Authentication failed. Please try again.',
};

export default function Login() {
  const [token, setToken] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [isRedirecting, setIsRedirecting] = useState(false);
  const { login, clearError } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const authProvider = import.meta.env.VITE_AUTH_PROVIDER;
  const errorParam = searchParams.get('error');
  const errorMessage = errorParam ? ERROR_MESSAGES[errorParam] || 'An error occurred.' : null;

  const clearErrorParam = () => {
    if (errorParam) {
      setSearchParams({}, { replace: true });
    }
    clearError();
  };

  const handleSignIn = async () => {
    clearErrorParam();
    setIsRedirecting(true);
    try {
      await signinRedirect();
    } catch (err) {
      console.error('[Login] OIDC redirect failed', err);
      setIsRedirecting(false);
    }
  };

  const handleRegister = async () => {
    clearErrorParam();
    setIsRedirecting(true);
    try {
      await signinRedirect();
    } catch (err) {
      console.error('[Login] OIDC redirect failed', err);
      setIsRedirecting(false);
    }
  };

  if (authProvider === 'keycloak') {
    return (
      <div className="page">
        <h1>HomeTusk</h1>
        {errorMessage && (
          <div className="card" style={{ marginBottom: '16px', background: '#fff3f3', borderColor: '#ffcdd2' }}>
            <p style={{ color: '#c62828', margin: 0 }}>{errorMessage}</p>
          </div>
        )}
        {isRedirecting ? (
          <p>Redirecting to login...</p>
        ) : (
          <div className="card">
            <h2>Welcome back!</h2>
            <button className="button" type="button" onClick={handleSignIn}>
              Sign in
            </button>
            <p style={{ marginTop: '16px' }}>
              Don&apos;t have an account?{' '}
              <button className="ghost-button" type="button" onClick={handleRegister}>
                Sign in to register
              </button>
            </p>
          </div>
        )}
      </div>
    );
  }

  if (authProvider !== 'dev') {
    return (
      <div className="page">
        <h1>Authentication Not Available</h1>
        <p>VITE_AUTH_PROVIDER must be &apos;dev&apos; or &apos;keycloak&apos;.</p>
        <p>Current: {authProvider || 'not set'}</p>
      </div>
    );
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    clearErrorParam();
    setError('');
    setLoading(true);

    try {
      await login(token.trim());
      navigate('/households');
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

      {errorMessage && (
        <div className="card" style={{ marginBottom: '16px', background: '#fff3f3', borderColor: '#ffcdd2' }}>
          <p style={{ color: '#c62828', margin: 0 }}>{errorMessage}</p>
        </div>
      )}

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

        <button className="button" type="submit" disabled={loading || !token.trim()}>
          {loading ? 'Logging in...' : 'Login'}
        </button>
      </form>
    </div>
  );
}
```

---

### Step 6: Update Callback.tsx

**File:** `clients/web/src/routes/Callback.tsx`
**Action:** MODIFY

**Changes:**
- Remove localStorage write
- Redirect with error param on failure

**Full updated file:**

```typescript
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { signinCallback } from '../lib/auth/oidc';

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
          // Token is now managed by oidc-client-ts UserManager (sessionStorage)
          // AuthContext will read it via getUser() on mount
          setStatus('success');
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
        // Redirect to login with error
        navigate('/login?error=auth_failed', { replace: true });
      }
    }

    handleCallback();

    return () => {
      isMounted = false;
    };
  }, [navigate]);

  if (status === 'processing') {
    return (
      <div style={{ padding: '2rem', textAlign: 'center' }}>
        <p>Processing login...</p>
      </div>
    );
  }

  // Error and success states redirect immediately, so this is just fallback
  return null;
}
```

---

### Step 7: Update ProtectedRoute.tsx

**File:** `clients/web/src/components/ProtectedRoute.tsx`
**Action:** MODIFY

**Changes:**
- Read error from useAuth()
- Redirect with error param if present

**Full updated file:**

```typescript
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

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
    const redirectPath = error ? `/login?error=${error}` : '/login';
    return <Navigate to={redirectPath} state={{ from: location }} replace />;
  }

  if (requireHousehold && !householdId) {
    return <Navigate to="/households" replace />;
  }

  return <Outlet />;
}
```

---

### Step 8: Update useAuth hook

**File:** `clients/web/src/hooks/useAuth.ts`
**Action:** MODIFY (if needed)

Check if useAuth hook needs to expose `error` and `clearError`. If the hook just re-exports context, update the type.

```typescript
import { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
```

---

## Verification Commands

After implementation, run:

```bash
# Build passes (type checking + Vite build)
cd clients/web && npm run build

# Lint passes
cd clients/web && npm run lint

# Dev server starts without errors
cd clients/web && npm run dev
```

---

## Verification Checklist

- [ ] `tokenProvider.ts` created with setTokenGetter, getAuthToken, setAuthErrorHandler, handleAuthError
- [ ] `oidc.ts` extended with getUser, signinSilent, signoutRedirect, removeUser, registerTokenEvents
- [ ] `oidc.ts` config has `offline_access` scope and `automaticSilentRenew: true`
- [ ] `AuthContext.tsx` supports dual mode (keycloak/dev)
- [ ] `AuthContext.tsx` has error state and clearError
- [ ] `AuthContext.tsx` registers token provider callbacks
- [ ] `AuthContext.tsx` registers token events in keycloak mode
- [ ] `api.ts` uses getAuthToken() instead of localStorage
- [ ] `api.ts` calls handleAuthError on 401
- [ ] `Login.tsx` reads error from query param
- [ ] `Login.tsx` displays error message with appropriate styling
- [ ] `Login.tsx` clears error on new login attempt
- [ ] `Callback.tsx` does NOT write to localStorage
- [ ] `Callback.tsx` redirects to /login?error=auth_failed on error
- [ ] `ProtectedRoute.tsx` includes error in redirect path
- [ ] `npm run build` passes
- [ ] `npm run lint` passes

---

## Acceptance Criteria Verification

| AC | How to verify |
|----|---------------|
| Silent refresh on token expiring | Token events registered, automaticSilentRenew enabled |
| Logout clears tokens | removeUser() called, state cleared |
| 401 triggers auto-logout + message | handleAuthError called in api.ts, error in query param |
| Dev mode unchanged | Dev mode path in AuthContext preserved |

---

## Anti-Scope-Creep Verification

Before completing, verify you did NOT:
- [ ] Add "Remember me" feature (LATER)
- [ ] Add session timeout warning modal (LATER)
- [ ] Add multi-tab session sync (LATER)
- [ ] Add full Keycloak signoutRedirect on every logout (local removeUser is sufficient for MVP)

---

## Keycloak Prerequisites (for testing)

Ensure Keycloak client is configured with:
- [ ] Refresh tokens enabled (default for public clients)
- [ ] `offline_access` scope available (may need to add to client scopes)

---

## Output

After completion, provide:
1. List of files created/modified
2. Output of `npm run build`
3. Output of `npm run lint`
4. Confirmation of verification checklist
