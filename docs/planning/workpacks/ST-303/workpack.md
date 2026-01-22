# Workpack: ST-303 — Session Management & Error UX

## Sources of Truth
- Story: `docs/planning/epics/EP-004/stories/ST-303-session-management.md`
- Epic: `docs/planning/epics/EP-004/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-onboarding-registration.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Goal
Implement robust session management and clear error UX for auth failures.

## User Value
Session persists reliably; clear messages when auth fails.

---

## In Scope

### Session Management
- Refactor AuthContext for OIDC
- Token storage (memory)
- Silent token refresh
- Logout (local + Keycloak end-session)
- Handle 401 responses globally

### Error UX
- Auth error state in AuthContext
- Error display on Login screen
- Auto-redirect on 401

## Out of Scope
- Multi-device session (LATER)
- Remember me (LATER)

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `clients/web/src/context/AuthContext.tsx` | MODIFY | Refactor for OIDC |
| `clients/web/src/lib/api.ts` | MODIFY | Global 401 handling |
| `clients/web/src/routes/Login.tsx` | MODIFY | Display auth errors |
| `clients/web/src/components/ProtectedRoute.tsx` | MODIFY | Handle auth errors |

---

## Implementation Plan

### Commit 1: Refactor AuthContext for OIDC
- Use UserManager from lib/auth/oidc
- Store User object from oidc-client-ts
- Get token from User.access_token
- Add error state

### Commit 2: Implement token refresh
- Subscribe to accessTokenExpiring event
- Call signinSilent()
- Handle refresh failure (redirect to login)

### Commit 3: Implement logout
- Call signoutRedirect() or manual clear + redirect
- Clear all state

### Commit 4: Global 401 handling
- In api.ts fetch wrapper, check for 401
- On 401, call logout()
- Redirect to /login with error message

### Commit 5: Error UX on Login
- Accept query param `?error=session_expired`
- Display appropriate message
- Clear error on new login attempt

---

## API Dependencies

| Endpoint | Error Code | Action |
|----------|------------|--------|
| Any protected | 401 | Logout + redirect |

---

## Verification Commands

```bash
cd clients/web && npm run build
cd clients/web && npm run lint
```

---

## Demo Scenario

1. Login via Keycloak
2. Wait for token to expire (or manually invalidate)
3. Make API call
4. See 401 -> auto-logout -> redirect to /login with message
5. Login again
6. Click Logout
7. Verify redirect to /login, Keycloak session cleared

---

## Risks

| Risk | Mitigation |
|------|------------|
| Silent refresh fails in iframe | Fallback to explicit re-auth |
| Keycloak end-session not working | Document, test with actual Keycloak |

---

## Rollback
- Revert AuthContext changes
- Revert api.ts changes
- Dev mode continues to work

---

## Anti-Scope-Creep

DO NOT:
- Add "Remember me" checkbox (LATER)
- Add session timeout warning modal (LATER)
- Add multi-tab session sync (LATER)
