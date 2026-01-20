# Story: Auth Integration + Household Selector

**ID:** ST-202
**Epic:** EP-003 (Web Foundation)
**Points:** 3
**Status:** Ready
**Priority:** P1
**Depends on:** ST-201

---

## Title

Integrate authentication and household selector using existing API

---

## Description

As a user, I want to authenticate and select my household, so that I can see my household's data.

**Context:**
This story integrates the web client with the backend auth (Keycloak JWT) and household API. Two auth modes:
1. **Dev mode:** Token paste (for local development without Keycloak)
2. **Target mode:** Keycloak OIDC redirect (production)

---

## Acceptance Criteria

### AC1: Auth context exists
```
Given the web client
When I implement auth context
Then useAuth() hook provides:
  - isAuthenticated: boolean
  - user: { id, email, displayName } | null
  - token: string | null
  - login(): void
  - logout(): void
```

### AC2: Dev mode auth works
```
Given VITE_AUTH_PROVIDER=dev
When I visit /login
Then I see a text input for JWT token paste
And a "Login" button

When I paste a valid JWT and click Login
Then token is stored in localStorage
And I am redirected to household selector
And useAuth().isAuthenticated === true
```

### AC3: Token attached to requests
```
Given I am authenticated
When the app makes API requests
Then Authorization: Bearer <token> header is attached
```

### AC4: GET /users/me integration
```
Given I am authenticated
When the app loads
Then GET /api/v1/users/me is called
And response is stored in auth context
And user.households array is available

Given API returns 401
Then user is logged out
And redirected to /login
```

### AC5: Household selector
```
Given user has 1+ households
When I see the household selector
Then I see list of households from user.households
With name and role displayed

When I click a household
Then householdId is stored in context
And I am redirected to /households/:householdId/tasks
```

### AC6: Single household shortcut
```
Given user has exactly 1 household
When I authenticate
Then household is auto-selected
And I am redirected directly to /households/:householdId/tasks
```

### AC7: Protected routes
```
Given I am not authenticated
When I try to access /households/:householdId/*
Then I am redirected to /login

Given I am authenticated but no household selected
When I try to access /households/:householdId/*
Then I am redirected to household selector
```

---

## Test Strategy

**Manual verification:**
- Dev mode: paste token → see user profile → see households
- Select household → navigate to tasks
- Logout → token cleared → redirect to login
- Access protected route without auth → redirect to login

**Unit tests:**
- AuthProvider stores/clears token
- useAuth hook returns correct state
- Protected route component redirects

**Integration tests (optional):**
- Mock `/users/me` response → verify household selector

---

## Technical Notes

**Auth flow (dev mode):**
```
Login page → paste JWT → store in localStorage
→ GET /users/me → store user+households
→ if 1 household: auto-select → /households/:householdId/tasks
→ if N households: show selector → user picks → /households/:householdId/tasks
```

**Auth context structure:**
```typescript
interface AuthContext {
  isAuthenticated: boolean;
  user: UserProfile | null;
  token: string | null;
  householdId: string | null;
  login: (token: string) => Promise<void>;
  logout: () => void;
  selectHousehold: (id: string) => void;
}

interface UserProfile {
  id: string;
  email: string;
  displayName: string;
  households: HouseholdSummary[];
}

interface HouseholdSummary {
  id: string;
  name: string;
  role: 'admin' | 'member';
}
```

**API client setup:**
```typescript
// src/lib/api.ts
const api = {
  baseUrl: import.meta.env.VITE_API_BASE_URL,

  async fetch<T>(path: string, options?: RequestInit): Promise<T> {
    const token = localStorage.getItem('auth_token');
    const res = await fetch(`${this.baseUrl}${path}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(token && { Authorization: `Bearer ${token}` }),
        ...options?.headers,
      },
    });

    if (res.status === 401) {
      // Handle auth error
      throw new AuthError('Unauthorized');
    }

    if (!res.ok) {
      throw new ApiError(res.status, await res.json());
    }

    return res.json();
  },

  getMe: () => api.fetch<UserProfile>('/users/me'),
};
```

**Files to create/modify:**
```
src/
├── context/
│   └── AuthContext.tsx
├── hooks/
│   └── useAuth.ts
├── lib/
│   ├── api.ts (update)
│   └── errors.ts
├── routes/
│   ├── Login.tsx (update)
│   ├── HouseholdSelector.tsx (new)
│   └── ProtectedRoute.tsx (new)
└── components/
    └── HouseholdCard.tsx (new)
```

---

## Related Artifacts

| Artifact | Path |
|----------|------|
| Epic | `docs/planning/epics/EP-003/epic.md` |
| OpenAPI | `docs/contracts/http/commands.openapi.yaml` (UserProfile, HouseholdSummary) |
| API Coverage | `docs/mvp/api-coverage.md` (GET /users/me) |

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | Consuming existing API |
| adr_needed | no | Standard auth pattern |
| diagrams_needed | no | — |

---

## Definition of Ready Checklist

- [x] Title clear
- [x] AC testable
- [x] Deliverables defined
- [x] Test strategy defined
- [x] Dependencies identified (ST-201)
- [x] No blockers
