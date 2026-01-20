# ST-202 — Auth Integration + Household Selector

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q1-web-client.md`
- Epic: `docs/planning/epics/EP-003/epic.md`
- Story: `docs/planning/epics/EP-003/stories/ST-202-auth-household.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml` (UserProfile, HouseholdSummary schemas)
- API Coverage: `docs/mvp/api-coverage.md` (GET /users/me)

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

| Path | Action | Purpose |
|------|--------|---------|
| `clients/web/src/context/AuthContext.tsx` | Create | Auth provider and context |
| `clients/web/src/hooks/useAuth.ts` | Create | Hook to access auth context |
| `clients/web/src/lib/api.ts` | Modify | Add auth header, getMe() |
| `clients/web/src/lib/errors.ts` | Create | Error types (AuthError, ApiError) |
| `clients/web/src/routes/Login.tsx` | Modify | Token paste form |
| `clients/web/src/routes/HouseholdSelector.tsx` | Create | Household selection page |
| `clients/web/src/components/ProtectedRoute.tsx` | Create | Route guard component |
| `clients/web/src/components/HouseholdCard.tsx` | Create | Household display card |
| `clients/web/src/routes/index.tsx` | Modify | Add protected routes |
| `clients/web/src/App.tsx` | Modify | Wrap with AuthProvider |
| `clients/web/src/types/api.ts` | Create | TypeScript types from OpenAPI |

## Implementation Plan

### Commit 1 — Types and error handling
Steps:
1. Create `src/types/api.ts` with UserProfile, HouseholdSummary types
2. Create `src/lib/errors.ts` with AuthError, ApiError classes
3. Update `src/lib/api.ts` with proper error handling

Files:
- `clients/web/src/types/api.ts`
- `clients/web/src/lib/errors.ts`
- `clients/web/src/lib/api.ts`

Verification:
- TypeScript compiles without errors

### Commit 2 — Auth context and hook
Steps:
1. Create AuthContext with state: isAuthenticated, user, token, householdId
2. Create login(), logout(), selectHousehold() functions
3. Store token in localStorage
4. Create useAuth() hook
5. Wrap App with AuthProvider

Files:
- `clients/web/src/context/AuthContext.tsx`
- `clients/web/src/hooks/useAuth.ts`
- `clients/web/src/App.tsx`

Verification:
- `npm run build` passes
- useAuth() available in components

### Commit 3 — Login page (token paste)
Steps:
1. Update Login.tsx with token input form
2. On submit: store token, call getMe(), redirect
3. Handle errors (invalid token, network)
4. Show loading state

Files:
- `clients/web/src/routes/Login.tsx`

Verification:
- Paste valid JWT → fetches profile → redirects
- Paste invalid JWT → shows error

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

### Commit 5 — Protected routes and auto-select
Steps:
1. Create ProtectedRoute component
2. Redirect to /login if not authenticated
3. Redirect to /households if no household selected
4. Implement single-household auto-select
5. Update route definitions

Files:
- `clients/web/src/components/ProtectedRoute.tsx`
- `clients/web/src/routes/index.tsx`
- `clients/web/src/context/AuthContext.tsx`

Verification:
- Access /households/:householdId without auth → redirect to /login
- Login with 1 household → auto-redirect to tasks
- Login with 2+ households → show selector

## Contract Reference (OpenAPI)

### GET /users/me Response
```typescript
interface UserProfile {
  id: string;
  externalId: string;
  email: string;
  displayName: string;
  avatarUrl?: string;
  households: HouseholdSummary[];
  createdAt: string;
}

interface HouseholdSummary {
  id: string;
  name: string;
  role: 'admin' | 'member';
}
```

### Error Responses
- 401 Unauthorized → logout, redirect to /login
- Network error → show error message

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
