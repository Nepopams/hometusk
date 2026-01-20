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
- `clients/web/src/lib/api.ts`
- `clients/web/src/routes/Login.tsx`
- `clients/web/src/routes/index.tsx`
- `clients/web/src/App.tsx`

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
```typescript
export interface UserProfile {
  id: string;
  externalId: string;
  email: string;
  displayName: string;
  avatarUrl?: string;
  households: HouseholdSummary[];
  createdAt: string;
}

export interface HouseholdSummary {
  id: string;
  name: string;
  role: 'admin' | 'member';
}
```

### 2. Error Classes (src/lib/errors.ts)
```typescript
export class AuthError extends Error {
  constructor(message: string) {
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

### 3. API Client (src/lib/api.ts)
```typescript
import { AuthError, ApiError } from './errors';
import type { UserProfile } from '../types/api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

function getToken(): string | null {
  return localStorage.getItem('auth_token');
}

async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const token = getToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...options?.headers,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    throw new AuthError('Unauthorized');
  }

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new ApiError(response.status, body);
  }

  return response.json();
}

export const api = {
  getMe: () => apiFetch<UserProfile>('/users/me'),
  // Add more methods as needed
};
```

### 4. Auth Context (src/context/AuthContext.tsx)
```typescript
import { createContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { api } from '../lib/api';
import { AuthError } from '../lib/errors';
import type { UserProfile } from '../types/api';

interface AuthContextType {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: UserProfile | null;
  token: string | null;
  householdId: string | null;
  login: (token: string) => Promise<void>;
  logout: () => void;
  selectHousehold: (id: string) => void;
}

export const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem('auth_token')
  );
  const [user, setUser] = useState<UserProfile | null>(null);
  const [householdId, setHouseholdId] = useState<string | null>(() =>
    localStorage.getItem('selected_household')
  );
  const [isLoading, setIsLoading] = useState(true);

  const logout = useCallback(() => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('selected_household');
    setToken(null);
    setUser(null);
    setHouseholdId(null);
  }, []);

  const login = useCallback(async (newToken: string) => {
    localStorage.setItem('auth_token', newToken);
    setToken(newToken);

    try {
      const profile = await api.getMe();
      setUser(profile);

      // Auto-select if single household
      if (profile.households.length === 1) {
        const hid = profile.households[0].id;
        localStorage.setItem('selected_household', hid);
        setHouseholdId(hid);
      }
    } catch (error) {
      logout();
      throw error;
    }
  }, [logout]);

  const selectHousehold = useCallback((id: string) => {
    localStorage.setItem('selected_household', id);
    setHouseholdId(id);
  }, []);

  // Load user on mount if token exists
  useEffect(() => {
    if (token && !user) {
      api.getMe()
        .then(setUser)
        .catch((error) => {
          if (error instanceof AuthError) {
            logout();
          }
        })
        .finally(() => setIsLoading(false));
    } else {
      setIsLoading(false);
    }
  }, [token, user, logout]);

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated: !!token && !!user,
        isLoading,
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
```typescript
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export function ProtectedRoute() {
  const { isAuthenticated, isLoading, householdId } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <div>Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (!householdId && !location.pathname.includes('/households')) {
    return <Navigate to="/households" replace />;
  }

  return <Outlet />;
}
```

### 7. Route Updates (src/routes/index.tsx)
Add:
- `/households` route for HouseholdSelector
- Wrap household routes with ProtectedRoute

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
