# Sprint S03 — Scope Detail

## Sources of Truth
- Sprint Plan: `docs/planning/pi/2026Q1-PI01/sprints/S03/sprint.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-onboarding-registration.md`
- Epic: `docs/planning/epics/EP-004/epic.md`

---

## Committed Scope

### ST-301: Keycloak OIDC Integration
**Points:** 3
**Priority:** P1
**Status:** Ready (pending Keycloak prereqs)
**Workpack:** `docs/planning/workpacks/ST-301/`

**What's included:**
- Add oidc-client-ts npm dependency
- Create lib/auth/oidc.ts with UserManager
- Implement PKCE authorization code flow
- Add /callback route
- Environment variable configuration

**What's NOT included:**
- UI changes (ST-302)
- Token refresh logic (ST-303)
- Documentation (ST-304)

**DoR:** PENDING (Keycloak prereqs)
**Dependencies:** S02 complete

---

### ST-302: Login & Registration Screens
**Points:** 2
**Priority:** P1
**Status:** Ready
**Workpack:** `docs/planning/workpacks/ST-302/`

**What's included:**
- Update Login.tsx with Keycloak mode
- "Sign in" button triggers OIDC redirect
- "Register" link opens Keycloak registration
- Loading state during redirect
- Dev mode preserved (VITE_AUTH_PROVIDER=dev)

**What's NOT included:**
- Error message UI (ST-303)
- Token refresh (ST-303)
- Forgot password link (NEXT)

**DoR:** PASS
**Dependencies:** ST-301

---

### ST-303: Session Management & Error UX
**Points:** 3
**Priority:** P1
**Status:** Ready
**Workpack:** `docs/planning/workpacks/ST-303/`

**What's included:**
- Refactor AuthContext for OIDC
- Token storage (memory, not localStorage)
- Silent token refresh
- Logout (local + Keycloak end-session)
- Global 401 handling in api.ts
- Error display on Login screen

**What's NOT included:**
- Multi-device session (LATER)
- Remember me (LATER)

**DoR:** PASS
**Dependencies:** ST-301, ST-302

---

### ST-304: E2E Onboarding Flow & Docs
**Points:** 2
**Priority:** P1
**Status:** Ready
**Workpack:** `docs/planning/workpacks/ST-304/`

**What's included:**
- E2E manual testing of full onboarding flow
- Update docs/runbooks/local-dev.md with Keycloak setup
- Update clients/web/README.md with auth section
- Verify .env.example completeness

**What's NOT included:**
- Automated E2E tests (NEXT)
- Performance testing (NEXT)

**DoR:** PASS
**Dependencies:** ST-301, ST-302, ST-303

---

## Out of Scope (Explicit)

### Deferred to NEXT Increment
- Password reset UI
- Email verification UI
- Invite-only registration flag
- Rate limiting / anti-abuse
- Automated E2E tests

### Deferred to LATER Increment
- Social login (Google, etc.)
- Magic link authentication
- Multi-device session management

### Never in Onboarding Scope
- New backend microservices
- RBAC expansion
- Backend logic changes

---

## Acceptance Criteria Summary

**Sprint succeeds if:**
1. New user can register via Keycloak web flow (via Keycloak login page with Register option)
2. User can login via Keycloak OIDC
3. Session persists across page refresh (via sessionStorage)
4. Logout clears local + Keycloak session
5. 401 triggers auto-logout + redirect
6. Error messages are clear
7. After login, user sees household selector OR empty-state (if zero households)
8. Documentation is updated
9. npm run build/lint pass

**Sprint fails if:**
- Cannot login via Keycloak
- Token refresh crashes app
- 401 not handled
- No documentation updates

---

## Readiness Notes

**All committed stories:**
- Have workpacks with implementation plans
- Have checklists
- Dependencies are sequential and clear

**Prerequisite (before sprint start):**
- Keycloak realm must be configured with:
  - Client `hometusk-web` (public client)
  - Valid Redirect URIs: `http://localhost:5173/callback`
  - Web Origins: `http://localhost:5173`
  - PKCE enabled (code_challenge_method = S256)
  - User registration enabled in realm (Login tab)
  - Backend CORS allows web origin (already configured for localhost)

**Key decisions documented in EP-004:**
- Fix A: sessionStorage for OIDC state (survives refresh, clears on tab close)
- Fix B: Registration via standard Keycloak login page (not hardcoded /registrations URL)
- Fix D: Zero households shows empty-state (household creation is NEXT initiative)

**Risks mitigated:**
- Keycloak config: verify before start
- CORS: test early with real backend
- Scope creep: explicit Out of Scope list

**Human gates:**
- Gate B: approve committed scope (this sprint) - DONE
- Gate C: approve PLAN before APPLY (each story)
- Gate D: approve merge (each story)
