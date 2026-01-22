# Workpack: ST-302 — Login & Registration Screens

## Sources of Truth
- Story: `docs/planning/epics/EP-004/stories/ST-302-login-registration-screens.md`
- Epic: `docs/planning/epics/EP-004/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-onboarding-registration.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- Web Baseline: `clients/web/src/routes/Login.tsx`

---

## Goal
Create user-facing login and registration screens using Keycloak OIDC.

## User Value
Clear, simple login/register experience without technical knowledge.

---

## In Scope
- Update Login.tsx with Keycloak mode
- "Sign in" button triggers OIDC redirect
- "Register" link opens Keycloak registration
- Loading state during OIDC flow
- Preserve dev mode for VITE_AUTH_PROVIDER=dev

## Out of Scope
- Token refresh (ST-303)
- Error handling beyond loading state (ST-303)
- Styling overhaul

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `clients/web/src/routes/Login.tsx` | MODIFY | Add Keycloak login mode |

---

## Implementation Plan

### Commit 1: Refactor Login.tsx for dual mode
- Check VITE_AUTH_PROVIDER
- If 'keycloak': show "Sign in" button + "Register" link
- If 'dev': show existing token paste form
- Add loading state for OIDC redirect

### Commit 2: Wire up OIDC actions
- Import signinRedirect from lib/auth/oidc
- "Sign in" button calls signinRedirect()
- "Register" link navigates to Keycloak registration URL
- Show loading spinner while redirecting

---

## API Dependencies

None (UI only, uses lib/auth/oidc from ST-301)

---

## Registration Approach (Fix B)

**DO NOT use hardcoded `/registrations` URL** — it's version-dependent and may be unavailable.

**Correct approach:**
- Both "Sign in" and "Register" use the same `signinRedirect()` function
- "Register" link text guides user: "Don't have an account? Sign in to register"
- Keycloak login page shows "Register" option when realm setting enabled

**Keycloak prereq:** "User registration" must be enabled in realm (Login tab).

```typescript
// Both buttons call the same function
const handleSignIn = () => signinRedirect();
const handleRegister = () => signinRedirect(); // same flow, different UX text
```

---

## Verification Commands

```bash
cd clients/web && npm run build
cd clients/web && npm run lint
```

---

## Demo Scenario

1. Set VITE_AUTH_PROVIDER=keycloak
2. Start dev server
3. Visit /login
4. See "Sign in" button and "Register" link
5. Click "Sign in" -> redirect to Keycloak
6. Set VITE_AUTH_PROVIDER=dev
7. Restart dev server
8. Visit /login -> see token paste form

---

## Risks

| Risk | Mitigation |
|------|------------|
| User registration disabled in realm | Document prereq, verify before sprint (see epic Keycloak config section) |
| User confused by two-step register | Clear UX text: "Sign in to register" + Keycloak shows Register link |

---

## Rollback
- Revert Login.tsx changes
- Dev mode continues to work

---

## Anti-Scope-Creep

DO NOT:
- Add error message UI (ST-303)
- Add token refresh (ST-303)
- Add "Forgot password" link (NEXT)
- Add social login buttons (LATER)
