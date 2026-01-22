# Story: ST-302 — Login & Registration Screens

## Sources of Truth
- Epic: `docs/planning/epics/EP-004/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-onboarding-registration.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Summary
Create user-facing login and registration screens that use Keycloak OIDC flow.

## User Value
As a new user, I want to see a clear login/register screen so that I can access HomeTusk without technical knowledge.

---

## In Scope
- Update `/login` route with dual-mode support:
  - `VITE_AUTH_PROVIDER=keycloak` — "Sign in" button triggers OIDC
  - `VITE_AUTH_PROVIDER=dev` — token paste (existing)
- "Sign in" button initiates Keycloak login (OIDC redirect)
- "Register" link opens Keycloak login page where user can register (if "User registration" enabled in realm)
- Loading state during OIDC redirect
- Basic styling consistent with existing UI

> **Decision (Fix B):** Registration uses standard Keycloak authorize URL, NOT hardcoded `/registrations` endpoint.
> Keycloak login page shows "Register" link when realm has user registration enabled.
> This approach is stable across Keycloak versions and realm configurations.

## Out of Scope
- Token refresh (ST-303)
- Error handling UI (ST-303)
- Forgot password link (NEXT)
- Social login buttons (LATER)

---

## Acceptance Criteria

```gherkin
Given VITE_AUTH_PROVIDER=keycloak
When user visits /login
Then "Sign in" button is displayed
And "Register" link is displayed
And token paste form is NOT displayed

Given user clicks "Sign in"
Then loading indicator appears
And browser redirects to Keycloak login page

Given user clicks "Register"
Then browser redirects to Keycloak login page (same as "Sign in")
And Keycloak login page shows "Register" link (if enabled in realm)

Given VITE_AUTH_PROVIDER=dev
When user visits /login
Then token paste form is displayed (existing behavior)
```

> **Note:** Both "Sign in" and "Register" redirect to the same Keycloak authorize URL.
> The difference is UX guidance: "Register" tells user to click Keycloak's "Register" link on the login page.

---

## UI Specification

**Login Screen (Keycloak mode):**
```
+-----------------------------+
|      HomeTusk               |
|                             |
|   Welcome back!             |
|                             |
|   +---------------------+   |
|   |     Sign in         |   |
|   +---------------------+   |
|                             |
|   Don't have an account?    |
|   Register here             |
|                             |
+-----------------------------+
```

---

## Technical Notes

**Files to modify:**
- `clients/web/src/routes/Login.tsx` — add Keycloak mode
- `clients/web/src/styles/login.css` — styling (if needed)

**Registration approach (Fix B):**
- Do NOT use hardcoded `/registrations` endpoint (version-dependent, may not be available)
- Both "Sign in" and "Register" call `signinRedirect()` from oidc-client-ts
- "Register" link includes UX text like "Don't have an account? Sign in to register"
- Keycloak login page displays "Register" option when realm has "User registration" enabled

**Keycloak realm prerequisite:**
- "User registration" must be enabled in realm settings (Login tab)
- See `docs/planning/epics/EP-004/epic.md#keycloak-client-configuration`

---

## Test Strategy

**Manual tests:**
- Visit /login with VITE_AUTH_PROVIDER=keycloak — see correct UI
- Visit /login with VITE_AUTH_PROVIDER=dev — see token paste
- Click "Sign in" — redirect to Keycloak
- Click "Register" — redirect to Keycloak registration

---

## Flags

| Flag | Value |
|------|-------|
| contract_impact | no |
| adr_needed | no |
| security_sensitive | no |

---

## Dependencies
- ST-301 (OIDC integration)

## Points
2 (UI changes + dual mode)

## Priority
P1
