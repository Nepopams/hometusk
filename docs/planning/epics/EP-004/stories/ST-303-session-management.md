# Story: ST-303 — Session Management & Error UX

## Sources of Truth
- Epic: `docs/planning/epics/EP-004/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-onboarding-registration.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Summary
Implement robust session management (token storage, refresh, logout) and clear error UX for auth failures.

## User Value
As a user, I want my session to persist and see clear messages when something goes wrong so that I have a smooth experience.

---

## In Scope

### Session Management
- Store access token in memory (not localStorage) for security
- Store refresh token in memory or secure storage
- Implement silent token refresh (background renewal)
- Handle refresh token expiry (redirect to login)
- Logout: clear tokens + call Keycloak end-session endpoint

### Error UX
- 401 response: auto-logout + redirect to /login with message
- Token expired during session: attempt silent refresh, fallback to login
- Keycloak unavailable: show "Authentication service unavailable"
- Network error during auth: show retry option

### AuthContext Updates
- Add `refreshToken()` method
- Add `error` state for auth errors
- Handle OIDC events (token expiring, token expired)

## Out of Scope
- Multi-device session management (LATER)
- Remember me / persistent sessions (LATER)

---

## Acceptance Criteria

```gherkin
Given user is authenticated
When access token expires
Then silent refresh is attempted
And if successful, user continues without interruption
And if failed, user is redirected to /login

Given user is authenticated
When user clicks Logout
Then tokens are cleared from memory
And Keycloak end-session is called
And user is redirected to /login

Given user makes API call
When backend returns 401
Then user is logged out
And redirected to /login
And message "Session expired, please login again" is shown

Given Keycloak is unavailable
When user tries to login
Then message "Authentication service unavailable" is shown
And "Try again" button is available
```

---

## Technical Notes

**Token Storage Strategy:**
- Access token: in-memory (AuthContext state)
- Refresh token: in-memory (oidc-client-ts manages)
- User session: oidc-client-ts UserManager

**Silent Refresh:**
```typescript
userManager.events.addAccessTokenExpiring(() => {
  userManager.signinSilent().catch(() => {
    // redirect to login
  });
});
```

**Logout Flow:**
```typescript
const logout = async () => {
  await userManager.signoutRedirect();
  // or just clear local + redirect
};
```

**Files to modify:**
- `clients/web/src/context/AuthContext.tsx` — refactor for OIDC
- `clients/web/src/lib/api.ts` — handle 401 globally
- `clients/web/src/components/ProtectedRoute.tsx` — handle auth errors
- `clients/web/src/routes/Login.tsx` — show error messages

---

## Test Strategy

**Manual tests:**
- Login -> wait for token expiry -> verify silent refresh
- Login -> manually invalidate token -> make API call -> verify 401 handling
- Login -> click Logout -> verify redirect + Keycloak session cleared

---

## Flags

| Flag | Value |
|------|-------|
| contract_impact | no |
| adr_needed | maybe (token storage strategy) |
| security_sensitive | yes |

---

## Dependencies
- ST-301 (OIDC integration)
- ST-302 (Login screen for error display)

## Points
3 (complex state management + error handling)

## Priority
P1
