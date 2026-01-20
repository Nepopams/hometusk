# ST-202 DoD Checklist

## Story Reference
- Story: `docs/planning/epics/EP-003/stories/ST-202-auth-household.md`
- Workpack: `docs/planning/workpacks/ST-202/workpack.md`

---

## Acceptance Criteria

- [ ] **AC1:** Auth context exists
  - [ ] useAuth() hook available
  - [ ] Returns isAuthenticated: boolean
  - [ ] Returns user: UserProfile | null
  - [ ] Returns token: string | null
  - [ ] Provides login() function
  - [ ] Provides logout() function

- [ ] **AC2:** Dev mode auth works
  - [ ] /login shows token input field
  - [ ] Paste JWT → store in localStorage
  - [ ] After store → redirect to household selector or tasks
  - [ ] useAuth().isAuthenticated === true

- [ ] **AC3:** Token attached to requests
  - [ ] api.fetch() adds Authorization header
  - [ ] Header format: `Bearer <token>`
  - [ ] Works for all API calls

- [ ] **AC4:** GET /users/me integration
  - [ ] Called after login
  - [ ] Response stored in auth context
  - [ ] user.households available
  - [ ] 401 response → logout + redirect to /login

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
  - [ ] Not authenticated → redirect /login
  - [ ] Authenticated, no household → redirect /households
  - [ ] Authenticated + household → access granted

---

## Types (from OpenAPI)

- [ ] UserProfile type matches schema
- [ ] HouseholdSummary type matches schema
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

- [ ] Token stored in localStorage only
- [ ] No token logging to console
- [ ] 401 handled (logout + redirect)
- [ ] No hardcoded tokens in code

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
