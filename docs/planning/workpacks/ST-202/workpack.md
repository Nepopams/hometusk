# ST-202 — Auth Integration + Household Selector

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q1-web-client.md`
- Epic: `docs/planning/epics/EP-003/epic.md`
- Story: `docs/planning/epics/EP-003/stories/ST-202-auth-household.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml` (UserProfile, HouseholdSummary schemas)
- API Coverage: `docs/mvp/api-coverage.md` (GET /users/me)
- ST-201 Baseline: `clients/web/` (completed)

## Baseline from ST-201 (Actual State)

**Project structure:**
```
clients/web/
├── package.json (name: hometusk-web, type: module)
├── tsconfig.json (strict: true, jsx: react-jsx)
├── tsconfig.node.json (composite: true)
├── vite.config.ts (port: 5173, host: 0.0.0.0)
├── .eslintrc.cjs (react-in-jsx-scope: off)
├── .env.example (VITE_API_BASE_URL, VITE_AUTH_PROVIDER)
└── src/
    ├── main.tsx
    ├── App.tsx (RouterProvider)
    ├── vite-env.d.ts (VITE_* types)
    ├── routes/
    │   ├── index.tsx (router definitions)
    │   ├── Login.tsx (placeholder)
    │   ├── HouseholdLayout.tsx (Layout + Outlet)
    │   ├── TasksList.tsx, TaskDetail.tsx, ZonesList.tsx, Notifications.tsx, NotFound.tsx
    ├── components/Layout/
    │   ├── Layout.tsx (shell)
    │   ├── Header.tsx (app name, user placeholder)
    │   └── Sidebar.tsx (nav links)
    ├── lib/
    │   └── api.ts (apiFetch function - NO auth yet)
    └── styles/index.css
```

**Dependencies (actual versions):**
- react@18.2.0, react-dom@18.2.0, react-router-dom@6.16.0
- typescript@5.4.5 (not 5.4.0 - registry availability)
- vite@5.0.0, @vitejs/plugin-react@5.1.2 (not 4.0.0 - peer deps)
- @typescript-eslint/*@6.21.0 (not 7.0.0 - peer deps)
- eslint@8.56.0, prettier@3.2.5

**Existing api.ts capabilities:**
- apiFetch<T>(path, options) function
- Base URL from env (VITE_API_BASE_URL)
- Generic error handling (throws Error on !response.ok)
- NO auth header injection yet
- NO 401 handling yet
- NO TypeScript types for API responses yet

## Outcome
User can:
1. Authenticate via token paste (dev mode)
2. See their profile and households
3. Select a household to work with
4. Access protected routes only when authenticated

## Acceptance Criteria
- [ ] AC1: useAuth() hook provides auth state
- [ ] AC2: Dev mode token paste works (login → store → redirect)
- [ ] AC3: Authorization header attached to all requests
- [ ] AC4: GET /users/me integration (fetch profile, handle 401)
- [ ] AC5: Household selector shows list from user.households
- [ ] AC6: Single household auto-select
- [ ] AC7: Protected routes redirect to /login

## Non-goals (explicit)
- No Keycloak OIDC redirect (target mode — defer)
- No token refresh logic
- No persistent sessions beyond localStorage
- No user profile editing

## Files to create/modify

**New directories to create:**
- `clients/web/src/context/`
- `clients/web/src/hooks/`
- `clients/web/src/types/`

**Files:**

| Path | Action | Purpose |
|------|--------|---------|
| `clients/web/src/types/api.ts` | **Create** | TypeScript types from OpenAPI (UserProfile, HouseholdSummary) |
| `clients/web/src/lib/errors.ts` | **Create** | Error classes (AuthError, ApiError) |
| `clients/web/src/lib/api.ts` | **Modify** | Add auth header injection, 401 handling, typed getMe() |
| `clients/web/src/context/AuthContext.tsx` | **Create** | Auth provider and context (state + actions) |
| `clients/web/src/hooks/useAuth.ts` | **Create** | Hook to access auth context |
| `clients/web/src/routes/Login.tsx` | **Modify** | Replace placeholder with token paste form |
| `clients/web/src/routes/HouseholdSelector.tsx` | **Create** | Household selection page |
| `clients/web/src/routes/index.tsx` | **Modify** | Add /households route, wrap protected routes |
| `clients/web/src/components/ProtectedRoute.tsx` | **Create** | Route guard component |
| `clients/web/src/components/HouseholdCard.tsx` | **Create** | Household display card |
| `clients/web/src/App.tsx` | **Modify** | Wrap RouterProvider with AuthProvider |

## Implementation Plan

### Commit 1 — Types and error handling
Steps:
1. Create `src/types/api.ts` with UserProfile, HouseholdSummary types from OpenAPI
2. Create `src/lib/errors.ts` with AuthError, ApiError classes
3. Update `src/lib/api.ts`:
   - Import error classes
   - Add 401 detection → throw AuthError
   - Keep existing apiFetch signature
   - Add getMe() function returning Promise<UserProfile>
   - NO auth header injection yet (Commit 2)

Files:
- `clients/web/src/types/api.ts` (new)
- `clients/web/src/lib/errors.ts` (new)
- `clients/web/src/lib/api.ts` (modify)

Verification:
- `npm run build` passes
- TypeScript strict mode passes

### Commit 2 — Auth context and hook
Steps:
1. Create `src/context/AuthContext.tsx` with:
   - State: status (AuthStatus), user, token, householdId
   - Derived: isAuthenticated = status === 'authenticated'
   - Actions: login(token), logout(), selectHousehold(id)
   - Token persistence: localStorage with constants (hometusk_auth_token, hometusk_household_id)
   - Auto-select single household in login()
2. Create `src/hooks/useAuth.ts` hook
3. Update `src/lib/api.ts`:
   - Add storage constant AUTH_TOKEN_KEY
   - Add auth header injection to apiFetch (read token from localStorage)
   - If token exists → add Authorization: Bearer <token>
4. Update `src/App.tsx`:
   - Import AuthProvider
   - Wrap <RouterProvider> with <AuthProvider>

Files:
- `clients/web/src/context/AuthContext.tsx` (new)
- `clients/web/src/hooks/useAuth.ts` (new)
- `clients/web/src/lib/api.ts` (modify)
- `clients/web/src/App.tsx` (modify)

Verification:
- `npm run build` passes
- useAuth() available in components
- apiFetch includes Authorization header when token exists
- localStorage uses named constants

### Commit 3 — Login page (token paste)
Steps:
1. Update Login.tsx with:
   - Dev-only guard (check VITE_AUTH_PROVIDER === 'dev')
   - Token paste textarea + submit button
   - On submit: await login(token), navigate to /households
   - Error handling (AuthError, network errors)
   - Loading state during login
2. HouseholdSelector handles auto-redirect if 1 household

Files:
- `clients/web/src/routes/Login.tsx` (modify)

Verification:
- VITE_AUTH_PROVIDER !== 'dev' → shows unsupported message
- Paste valid JWT → login succeeds → redirects to /households
- Paste invalid JWT → shows error message

### Commit 4 — Household selector
Steps:
1. Create HouseholdSelector page
2. Show list of households from user.households
3. On click: selectHousehold(), navigate to /households/:householdId/tasks
4. Create HouseholdCard component
5. Add route for /households (selector page)

Files:
- `clients/web/src/routes/HouseholdSelector.tsx`
- `clients/web/src/components/HouseholdCard.tsx`
- `clients/web/src/routes/index.tsx`

Verification:
- After login → see household list
- Click household → navigate to tasks

### Commit 5 — Protected routes
Steps:
1. Create ProtectedRoute component with:
   - requireHousehold prop (optional, default: false)
   - status checking (prevent flash redirects during 'loading')
   - Redirect to /login if status === 'unauthenticated'
   - Redirect to /households if requireHousehold && !householdId
2. Update route definitions in index.tsx:
   - /households wrapped with <ProtectedRoute /> (no household required)
   - /households/:householdId wrapped with <ProtectedRoute requireHousehold />
3. Single-household auto-select already in AuthContext.login()

Files:
- `clients/web/src/components/ProtectedRoute.tsx` (new)
- `clients/web/src/routes/index.tsx` (modify)

Verification:
- Access /households/:householdId without auth → redirect to /login
- Access /households/:householdId without household selected → redirect to /households
- Login with 1 household → auto-select in AuthContext → navigate to /households → auto-redirect to tasks
- Login with 2+ households → navigate to /households → show selector
- No flash redirects during loading

## Contract Reference (OpenAPI)

### GET /users/me Response (200 OK)
```typescript
interface UserProfile {
  id: string;        // uuid
  externalId: string;
  email: string;
  displayName: string;
  avatarUrl?: string;
  households: HouseholdSummary[];
  createdAt: string; // date-time (ISO 8601)
}

interface HouseholdSummary {
  id: string;        // uuid
  name: string;
  role: HouseholdRole; // 'admin' | 'member'
}
```

### Error Responses
**401 Unauthorized:**
```typescript
interface AuthErrorResponse {
  errorCode: AuthErrorCode; // 'AUTH_TOKEN_MISSING' | 'AUTH_TOKEN_INVALID' | 'AUTH_TOKEN_EXPIRED'
  message: string;
}
```

**Handling:**
- 401 → parse AuthErrorResponse → throw AuthError with code → logout → redirect to /login
- Network error → show error message
- Other errors → ApiError with status and body

## Contract Impact
None — consuming existing API endpoints.

## Docs Updates
None required.

## Tests
- [ ] Unit: AuthContext stores/clears token
- [ ] Unit: useAuth returns correct state
- [ ] Unit: ProtectedRoute redirects when not auth
- [ ] Integration: Mock /users/me → verify flow

## Verification Commands
```bash
cd clients/web
npm run lint              # → passes
npm run build             # → passes
npm run dev               # → manual testing

# Manual test flow:
# 1. Visit /household/xxx → redirects to /login
# 2. Paste valid JWT → fetches profile
# 3. If 1 household → auto-redirect to /households/:householdId/tasks
# 4. If 2+ households → see selector → click → redirect
# 5. Visit /household/xxx again → works (authenticated)
# 6. Logout → redirects to /login
```

## DoD Checklist
- [ ] All 5 commits complete
- [ ] Auth context works (login/logout/selectHousehold)
- [ ] Token attached to requests (Authorization header)
- [ ] GET /users/me integrates correctly
- [ ] Protected routes redirect properly
- [ ] Single-household auto-select works
- [ ] npm run lint/build pass

## Risks
| Risk | Mitigation |
|------|------------|
| CORS issues with backend | Test early with real backend |
| Token storage security | Use localStorage (acceptable for MVP) |
| 401 handling loops | Guard against infinite redirects |

## Rollback
- Revert commits in reverse order
- No backend changes to revert
