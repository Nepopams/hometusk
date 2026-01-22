# Web Client Authentication Test Cases

Manual test cases for EP-004: Keycloak OIDC Integration.

## Prerequisites

- [ ] Keycloak running on `http://localhost:8180`
- [ ] `hometusk-web` client configured (see `keycloak-setup.md`)
- [ ] Backend running on `http://localhost:8080`
- [ ] Web client running on `http://localhost:5173`
- [ ] User registration enabled in Keycloak realm

---

## TC-001: New User Registration

**Objective:** Verify new user can register and access the app.

### Preconditions
- No existing user with test email in Keycloak
- `VITE_AUTH_PROVIDER=keycloak` in `.env.local`

### Steps

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Visit `http://localhost:5173` | Redirects to `/login` |
| 2 | Observe login page | "Sign in" button and "Sign in to register" link visible |
| 3 | Click "Sign in to register" | Redirects to Keycloak login page |
| 4 | Click "Register" link on Keycloak page | Registration form appears |
| 5 | Fill registration form (username, email, password) | Form accepts input |
| 6 | Submit registration | Keycloak processes registration |
| 7 | Observe redirect | Redirects to `/callback` then `/households` |
| 8 | Check households page | Empty household list displayed (Fix D) |

### Verification
- [ ] User created in Keycloak (Admin Console → Users)
- [ ] GET `/users/me` returns new user profile
- [ ] No console errors

---

## TC-002: Existing User Login

**Objective:** Verify existing user can login and access households/tasks.

### Preconditions
- Test user exists (alice/alice123)
- User has at least one household (optional)

### Steps

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Visit `http://localhost:5173/login` | Login page displayed |
| 2 | Click "Sign in" | Redirects to Keycloak login |
| 3 | Enter credentials (alice/alice123) | Credentials accepted |
| 4 | Submit login | Keycloak authenticates |
| 5 | Observe redirect | Redirects to `/callback` then `/households` |
| 6 | Check households page | User's households displayed |
| 7 | Select a household | Navigates to `/households/{id}` |
| 8 | Click "Tasks" | Tasks list displayed |

### Verification
- [ ] User session active
- [ ] Token in sessionStorage (via oidc-client-ts)
- [ ] API calls include Authorization header

---

## TC-003: Session Persistence (Page Refresh)

**Objective:** Verify session survives page refresh.

### Preconditions
- User logged in via Keycloak

### Steps

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Login as existing user | At `/households` page |
| 2 | Press F5 (refresh page) | Page reloads |
| 3 | Observe state | Still at `/households`, still authenticated |
| 4 | Navigate to tasks | Tasks load successfully |

### Verification
- [ ] No redirect to `/login` after refresh
- [ ] Token retrieved from sessionStorage
- [ ] API calls work after refresh

---

## TC-004: Logout

**Objective:** Verify logout clears session.

### Preconditions
- User logged in via Keycloak

### Steps

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Login as existing user | At `/households` page |
| 2 | Click Logout (if available) or clear session manually | Logout initiated |
| 3 | Observe redirect | Redirects to `/login` |
| 4 | Check sessionStorage | OIDC data cleared |
| 5 | Visit `/households` directly | Redirects to `/login` |

### Verification
- [ ] Session cleared from sessionStorage
- [ ] Protected routes redirect to login
- [ ] No lingering auth state

---

## TC-005: 401 Auto-Logout

**Objective:** Verify 401 response triggers automatic logout with error message.

### Preconditions
- User logged in via Keycloak
- Method to invalidate token (wait for expiry or manual)

### Steps

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Login as existing user | At `/households` page |
| 2 | Invalidate token (clear sessionStorage or wait for expiry) | Token invalid |
| 3 | Navigate to trigger API call (e.g., refresh tasks) | API returns 401 |
| 4 | Observe behavior | Auto-logout occurs |
| 5 | Check URL | Redirects to `/login?error=session_expired` |
| 6 | Check login page | "Session expired" message displayed |

### Verification
- [ ] 401 triggers handleAuthError
- [ ] Error parameter in URL
- [ ] Error message visible on login page

---

## TC-006: Silent Token Refresh

**Objective:** Verify tokens refresh automatically before expiry.

### Preconditions
- User logged in via Keycloak
- Short token lifetime configured in Keycloak (for testing)
- `offline_access` scope enabled

### Steps

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Login as existing user | At `/households` page |
| 2 | Open browser DevTools → Network | Monitor requests |
| 3 | Wait for token to approach expiry | automaticSilentRenew triggers |
| 4 | Observe network | Token refresh request sent |
| 5 | Continue using app | No interruption, new token used |

### Verification
- [ ] Console shows "[Auth] Token expiring" log
- [ ] Silent renew request in Network tab
- [ ] No redirect to login

---

## TC-007: Dev Mode (Token Paste)

**Objective:** Verify dev mode works without Keycloak.

### Preconditions
- `VITE_AUTH_PROVIDER=dev` in `.env.local`
- Valid JWT token obtained (see `local-dev.md`)

### Steps

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Restart dev server with dev mode | Server starts |
| 2 | Visit `http://localhost:5173/login` | Token paste form displayed |
| 3 | Obtain JWT token via curl | Token received |
| 4 | Paste token into form | Token accepted |
| 5 | Click Login | Authentication attempted |
| 6 | Observe redirect | Redirects to `/households` |

### Verification
- [ ] Token paste form visible (not Keycloak buttons)
- [ ] Token saved to localStorage
- [ ] API calls work with pasted token

---

## TC-008: Invalid OIDC Configuration

**Objective:** Verify graceful handling of invalid OIDC config.

### Preconditions
- Invalid `VITE_OIDC_AUTHORITY` (e.g., wrong port)

### Steps

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Set invalid VITE_OIDC_AUTHORITY | Config invalid |
| 2 | Restart dev server | Server starts |
| 3 | Visit `/login` | Login page displayed |
| 4 | Click "Sign in" | Redirect attempted |
| 5 | Observe behavior | Error logged, no crash |
| 6 | Check console | "[OIDC] Missing required environment variables" or similar |

### Verification
- [ ] App doesn't crash
- [ ] Error logged to console
- [ ] User sees feedback (redirect fails gracefully)

---

## TC-009: PKCE Verification

**Objective:** Verify PKCE is used in OIDC flow.

### Preconditions
- User not logged in
- Browser DevTools open

### Steps

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Visit `/login` | Login page displayed |
| 2 | Open DevTools → Network | Monitor requests |
| 3 | Click "Sign in" | Redirect to Keycloak |
| 4 | Inspect redirect URL | Check query parameters |

### Verification
- [ ] URL contains `code_challenge` parameter
- [ ] URL contains `code_challenge_method=S256`
- [ ] No client_secret in URL (public client)

---

## TC-010: Error Message Display

**Objective:** Verify all error types display correctly.

### Steps

| Error Type | URL | Expected Message |
|------------|-----|------------------|
| Session expired | `/login?error=session_expired` | "Session expired, please login again." |
| Auth unavailable | `/login?error=auth_unavailable` | "Authentication service unavailable." |
| Auth failed | `/login?error=auth_failed` | "Authentication failed. Please try again." |

### Verification
- [ ] Each error type shows correct message
- [ ] Error styling visible (red background)
- [ ] Error clears on new login attempt

---

## Test Summary Template

| TC | Description | Status | Notes |
|----|-------------|--------|-------|
| TC-001 | New User Registration | ⬜ | |
| TC-002 | Existing User Login | ⬜ | |
| TC-003 | Session Persistence | ⬜ | |
| TC-004 | Logout | ⬜ | |
| TC-005 | 401 Auto-Logout | ⬜ | |
| TC-006 | Silent Token Refresh | ⬜ | |
| TC-007 | Dev Mode | ⬜ | |
| TC-008 | Invalid OIDC Config | ⬜ | |
| TC-009 | PKCE Verification | ⬜ | |
| TC-010 | Error Messages | ⬜ | |

**Legend:** ✅ Pass | ❌ Fail | ⬜ Not Tested | ⏭️ Skipped

---

## Environment Checklist

Before testing, verify:

- [ ] Keycloak: `docker-compose ps` shows keycloak healthy
- [ ] Backend: `curl http://localhost:8080/actuator/health`
- [ ] Web: `http://localhost:5173` loads
- [ ] Client: `hometusk-web` exists in Keycloak
- [ ] Registration: User registration enabled in realm
- [ ] Scopes: `offline_access` added to client
