# ST-202 REVIEW Prompt

**Mode:** CODE REVIEW — Verify implementation against spec

---

## Context

Review the ST-202 implementation: Auth Integration + Household Selector.

**Read these files (mandatory):**
- `docs/planning/workpacks/ST-202/workpack.md` — implementation plan
- `docs/planning/workpacks/ST-202/checklist.md` — DoD checklist
- `docs/planning/epics/EP-003/stories/ST-202-auth-household.md` — story spec
- `docs/contracts/http/commands.openapi.yaml` — API contract

**Review these files:**
- `clients/web/src/types/api.ts`
- `clients/web/src/lib/errors.ts`
- `clients/web/src/lib/api.ts`
- `clients/web/src/context/AuthContext.tsx`
- `clients/web/src/hooks/useAuth.ts`
- `clients/web/src/routes/Login.tsx`
- `clients/web/src/routes/HouseholdSelector.tsx`
- `clients/web/src/components/ProtectedRoute.tsx`
- `clients/web/src/routes/index.tsx`

---

## Review Checklist

### 1. Types (api.ts)
- [ ] UserProfile matches OpenAPI schema
- [ ] HouseholdSummary matches OpenAPI schema
- [ ] No `any` types
- [ ] Proper optional fields marked

### 2. Error Handling (errors.ts)
- [ ] AuthError class exists
- [ ] ApiError class with status and body
- [ ] Properly extends Error

### 3. API Client (api.ts)
- [ ] Uses VITE_API_BASE_URL
- [ ] Adds Authorization header when token exists
- [ ] Throws AuthError on 401
- [ ] Throws ApiError on other failures
- [ ] getMe() returns UserProfile

### 4. Auth Context
- [ ] State: isAuthenticated, isLoading, user, token, householdId
- [ ] login() stores token and fetches profile
- [ ] logout() clears everything
- [ ] selectHousehold() stores and sets state
- [ ] Auto-select for single household
- [ ] Loads user on mount if token exists
- [ ] Handles 401 → logout

### 5. useAuth Hook
- [ ] Throws if used outside AuthProvider
- [ ] Returns all context values

### 6. Login Page
- [ ] Token input field
- [ ] Submit handler calls login()
- [ ] Loading state shown
- [ ] Error state shown
- [ ] Redirects after success

### 7. Household Selector
- [ ] Shows user.households list
- [ ] HouseholdCard shows name and role
- [ ] Click → selectHousehold → navigate
- [ ] Loading state

### 8. Protected Route
- [ ] Checks isAuthenticated
- [ ] Checks householdId for household routes
- [ ] Redirects to /login if not auth
- [ ] Redirects to /households if no household
- [ ] Shows loading during check

### 9. Routes
- [ ] /households route added
- [ ] Household routes wrapped with ProtectedRoute
- [ ] Login route accessible without auth

---

## Security Checks

- [ ] Token stored in localStorage (acceptable for MVP)
- [ ] No token in URL
- [ ] No token logging
- [ ] 401 properly handled
- [ ] No XSS in user input
- [ ] No infinite redirect loops

---

## Contract Compliance

Verify types match OpenAPI exactly:
- [ ] UserProfile.households is HouseholdSummary[]
- [ ] HouseholdSummary.role is 'admin' | 'member'
- [ ] All required fields present
- [ ] Optional fields marked with ?

---

## Verification Commands

```bash
cd clients/web
npm run lint    # Should pass
npm run build   # Should pass
npm run dev     # Manual testing

# Manual test flow:
# 1. Visit /household/xxx → redirect to /login
# 2. Enter invalid JWT → error shown
# 3. Enter valid JWT → profile fetched
# 4. If 1 household → auto-redirect to tasks
# 5. If 2+ households → selector shown
# 6. Click household → navigate to tasks
# 7. Refresh → stay logged in
# 8. Click logout → redirect to login
```

---

## Output Format

```markdown
# ST-202 Code Review Report

## Summary
[1-2 sentence summary]

## Verification Results
| Command | Result | Notes |
|---------|--------|-------|
| npm run lint | PASS/FAIL | |
| npm run build | PASS/FAIL | |
| Manual auth flow | PASS/FAIL | |

## Checklist Results
| Category | Status | Notes |
|----------|--------|-------|
| Types | PASS/FAIL | |
| Error Handling | PASS/FAIL | |
| API Client | PASS/FAIL | |
| Auth Context | PASS/FAIL | |
| Login Page | PASS/FAIL | |
| Household Selector | PASS/FAIL | |
| Protected Route | PASS/FAIL | |
| Routes | PASS/FAIL | |
| Security | PASS/FAIL | |
| Contract Compliance | PASS/FAIL | |

## Must-Fix Issues
[Critical issues]

## Should-Fix Issues
[Non-critical improvements]

## Verdict
**GO / NO-GO**

[Justification]
```

---

## GO/NO-GO Criteria

**GO if:**
- All verification commands pass
- Auth flow works end-to-end
- Types match OpenAPI contract
- Security checks pass

**NO-GO if:**
- Build/lint fails
- Auth flow broken
- Types don't match contract
- Security vulnerabilities found
- Infinite redirect loops
