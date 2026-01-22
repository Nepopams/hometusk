# Checklist: ST-302 — Login & Registration Screens

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-302/workpack.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Definition of Ready (DoR)

- [x] Story has clear title and description
- [x] Acceptance criteria defined
- [x] UI specification provided
- [x] Files to change listed
- [x] ST-301 complete (OIDC integration)
- [ ] User registration enabled in Keycloak realm (for Register flow)

---

## Definition of Done (DoD)

### Code Quality
- [x] Login.tsx updated with dual mode
- [x] No lint errors: `npm run lint`
- [x] Build passes: `npm run build`

### Functionality
- [x] Keycloak mode: shows "Sign in" button
- [x] Keycloak mode: shows "Register" link with guidance text
- [x] Keycloak mode: "Sign in" redirects to Keycloak login page
- [x] Keycloak mode: "Register" redirects to Keycloak login page (same URL, Fix B)
- [x] Dev mode: shows token paste form (unchanged)
- [x] Loading state displays during redirect

### Testing
- [ ] Manual test: Keycloak mode works (pending Keycloak)
- [ ] Manual test: Dev mode works
- [ ] Manual test: Loading state visible

---

## Acceptance Criteria Verification

| AC | Status | Evidence |
|----|--------|----------|
| Keycloak mode shows correct UI | [x] | Code: lines 36-58, "Sign in" button + "Register" link |
| "Sign in" redirects to Keycloak | [x] | Code: handleSignIn() calls signinRedirect() |
| "Register" opens Keycloak registration | [x] | Code: handleRegister() calls signinRedirect() (Fix B) |
| Dev mode shows token paste | [x] | Code: lines 70-110, unchanged |

---

## Code Review Summary (2026-01-22)

**Reviewer:** Claude Code
**Result:** ✅ PASS

**File verified:**
- `clients/web/src/routes/Login.tsx` — 111 lines, correct implementation

**Key changes:**
- Added `signinRedirect` import from oidc.ts
- Added `isRedirecting` state for loading UI
- Added `handleSignIn` and `handleRegister` handlers (both call signinRedirect)
- Added Keycloak mode branch (lines 36-58)
- Updated fallback message (lines 60-68)
- Dev mode preserved unchanged (lines 70-110)

**Fix B compliance:**
- ✅ Both "Sign in" and "Register" use same `signinRedirect()`
- ✅ No hardcoded `/registrations` URL
- ✅ UX text guides user: "Sign in to register"

**Scope compliance:**
- ✅ Only Login.tsx modified
- ✅ No AuthContext.tsx changes
- ✅ No oidc.ts changes
- ✅ No token refresh logic
- ✅ No error UI beyond console.error

**Notes:**
- Manual testing pending Keycloak realm configuration
