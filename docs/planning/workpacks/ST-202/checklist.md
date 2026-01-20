# ST-202 DoD Checklist

## Story Reference
- Story: `docs/planning/epics/EP-003/stories/ST-202-auth-household.md`
- Workpack: `docs/planning/workpacks/ST-202/workpack.md`

---

## Acceptance Criteria

- [ ] **AC1:** Auth context exists
  - [ ] useAuth() hook available
  - [ ] Returns status: AuthStatus ('idle' | 'loading' | 'authenticated' | 'unauthenticated')
  - [ ] Returns isAuthenticated: boolean (derived from status)
  - [ ] Returns user: UserProfile | null
  - [ ] Returns token: string | null
  - [ ] Returns householdId: string | null
  - [ ] Provides login() function
  - [ ] Provides logout() function
  - [ ] Provides selectHousehold() function
  - [ ] Uses named localStorage constants (hometusk_auth_token, hometusk_household_id)

- [ ] **AC2:** Dev mode auth works
  - [ ] Dev-only guard: VITE_AUTH_PROVIDER !== 'dev' → shows unsupported message
  - [ ] /login shows token paste textarea
  - [ ] Paste JWT → store in localStorage (hometusk_auth_token)
  - [ ] After login → redirect to /households (selector handles auto-redirect)
  - [ ] useAuth().isAuthenticated === true
  - [ ] useAuth().status === 'authenticated'

- [ ] **AC3:** Token attached to requests
  - [ ] api.fetch() adds Authorization header
  - [ ] Header format: `Bearer <token>`
  - [ ] Works for all API calls

- [ ] **AC4:** GET /users/me integration
  - [ ] Called after login
  - [ ] Response stored in auth context
  - [ ] user.households available
  - [ ] 401 response → parse AuthErrorResponse → logout + redirect to /login
  - [ ] AuthError includes errorCode if available

- [ ] **AC5:** Household selector
  - [ ] Shows list of user.households
  - [ ] Each shows name and role
  - [ ] Click → selectHousehold() called
  - [ ] Navigate to /households/:householdId/tasks

- [ ] **AC6:** Single household shortcut
  - [ ] If user.households.length === 1
  - [ ] Auto-select without showing selector
  - [ ] Direct redirect to /households/:householdId/tasks

- [ ] **AC7:** Protected routes
  - [ ] ProtectedRoute supports requireHousehold prop (optional, default: false)
  - [ ] status === 'loading' → shows loading placeholder (no flash redirects)
  - [ ] Not authenticated → redirect /login
  - [ ] requireHousehold=true + no household → redirect /households
  - [ ] Authenticated + household → access granted
  - [ ] /households uses ProtectedRoute without requireHousehold
  - [ ] /households/:householdId uses ProtectedRoute with requireHousehold=true

---

## Files Created/Modified

**New files created:**
- [ ] `clients/web/src/types/api.ts` (UserProfile, HouseholdSummary)
- [ ] `clients/web/src/lib/errors.ts` (AuthError, ApiError)
- [ ] `clients/web/src/context/AuthContext.tsx` (provider + state)
- [ ] `clients/web/src/hooks/useAuth.ts` (hook)
- [ ] `clients/web/src/routes/HouseholdSelector.tsx` (selector page)
- [ ] `clients/web/src/components/ProtectedRoute.tsx` (route guard)
- [ ] `clients/web/src/components/HouseholdCard.tsx` (card component)

**Files modified:**
- [ ] `clients/web/src/lib/api.ts` (auth header + 401 handling + getMe)
- [ ] `clients/web/src/routes/Login.tsx` (token paste form)
- [ ] `clients/web/src/routes/index.tsx` (add /households route, protected routes)
- [ ] `clients/web/src/App.tsx` (wrap with AuthProvider)

---

## Types (from OpenAPI)

- [ ] UserProfile type matches schema (id, externalId, email, displayName, avatarUrl?, households, createdAt)
- [ ] HouseholdSummary type matches schema (id, name, role)
- [ ] HouseholdRole = 'admin' | 'member'
- [ ] AuthErrorCode = 'AUTH_TOKEN_MISSING' | 'AUTH_TOKEN_INVALID' | 'AUTH_TOKEN_EXPIRED'
- [ ] AuthErrorResponse interface (errorCode, message)
- [ ] No `any` types in auth code

---

## Code Quality (DoD)

- [ ] TypeScript strict (no `any`)
- [ ] ESLint passes
- [ ] Prettier applied
- [ ] Error handling for all API calls
- [ ] Loading states shown
- [ ] No infinite redirect loops

---

## Security

- [ ] Token stored in localStorage with named constant (hometusk_auth_token)
- [ ] No token logging to console (check error messages)
- [ ] 401 handled (parse response → logout + redirect)
- [ ] No hardcoded tokens in code
- [ ] Dev-only guard prevents accidental prod usage (VITE_AUTH_PROVIDER check)

---

## Verification Commands

```bash
cd clients/web
npm run lint    # → passes
npm run build   # → passes
npm run dev     # → manual test flow

# Manual testing:
# 1. Visit protected route → redirect to /login
# 2. Paste valid JWT → login succeeds
# 3. Profile loads → households displayed (or auto-select)
# 4. Select household → navigate to tasks
# 5. Refresh page → stay authenticated
# 6. Logout → clear token → redirect to /login
```

---

## Sign-off

| Role | Name | Date | Status |
|------|------|------|--------|
| Developer | | | |
| Reviewer | | | |
