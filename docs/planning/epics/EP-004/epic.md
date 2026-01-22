# Epic: EP-004 — Registration & Sign-in (Web Onboarding)

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q1-onboarding-registration.md`
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- MVP Exit Review: `docs/planning/releases/reviews/MVP-exit-review.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- Web Client Baseline: `clients/web/` (post-S02)

---

## Status
**Approved** (Gate A passed 2026-01-22)

## Initiative Alignment
This epic implements the **NOW** increment of INIT-2026Q1-onboarding-registration:
- Web Registration screen + Login screen (Keycloak OIDC)
- PKCE flow for SPA security
- Auto-provision UserProfile (backend already supports)
- Error UX for auth failures
- Documentation update

---

## Epic Goal
Enable a new user to:
1. Access HomeTusk web application
2. Register a new account (via Keycloak self-registration)
3. Login (via Keycloak OIDC redirect with PKCE)
4. Get a valid session and see household selector
5. Understand errors clearly (expired token, not authorized)

This removes the dev-mode token paste dependency and enables real user onboarding.

---

## In Scope

### OIDC Integration
- Add `oidc-client-ts` library for Keycloak OIDC
- Implement PKCE authorization code flow
- Handle callback redirect (`/callback` route)
- Configure Keycloak client settings (env vars)

### Login & Registration Screens
- Login screen with "Sign in" button (triggers OIDC redirect to Keycloak)
- "Register" link opens standard Keycloak login page where user can register (if realm has "User registration" enabled)
- Loading states during OIDC flow
- Dev mode toggle (for local development without Keycloak)

> **MVP Decision (Fix B):** Registration handled via standard Keycloak authorize/login page, not hardcoded `/registrations` endpoint.
> User clicks "Register" → redirected to Keycloak login → clicks "Register" link on Keycloak page (if enabled in realm).

### Session Management
- Store OIDC session/state using `sessionStorage` (survives page refresh, cleared on tab close)
- Access token retrieved from oidc-client-ts User object (managed by library)
- Implement silent token refresh
- Logout flow (local + Keycloak end-session)
- Handle 401 responses (auto-logout + redirect)

> **MVP Decision (Fix A):** For NOW, use `sessionStorage` as baseline for OIDC state persistence.
> This allows session to survive page refresh while clearing on browser/tab close.
> "Memory-only access token with enhanced security" is deferred to NEXT/LATER as future hardening.

### Error UX
- Clear error messages for:
  - Token expired
  - Not authorized (403)
  - Network errors
  - Keycloak unavailable
- Retry/login again options

### Documentation
- Update auth strategy documentation
- Update local dev runbook
- Update environment variables documentation

---

## Out of Scope (explicit)

- **No password reset** — defer to NEXT (Keycloak handles this)
- **No email verification** — defer to NEXT (Keycloak handles this)
- **No invite-only registration** — defer to NEXT
- **No social login** — defer to LATER
- **No magic link** — defer to LATER
- **No multi-device session** — defer to LATER
- **No backend code changes** — backend already supports auto-provision
- **No RBAC expansion** — existing membership sufficient
- **No new microservices** — single backend
- **No household creation** — NEXT initiative (INIT-2026Q1-household-lifecycle)

> **Clarification (Fix C):** "No backend changes" means no backend feature code changes.
> Environment/infrastructure configuration (Keycloak client, CORS origins) is required and documented below.

---

## Keycloak Client Configuration (Environment Prereqs)

Before sprint start, verify Keycloak realm/client configuration:

| Setting | Value | Notes |
|---------|-------|-------|
| Client ID | `hometusk-web` | Public client (no secret) |
| Client Protocol | `openid-connect` | Standard OIDC |
| Access Type | `public` | SPA cannot keep secrets |
| Valid Redirect URIs | `http://localhost:5173/callback` (dev) | Add prod URI when deployed |
| Web Origins | `http://localhost:5173` (dev) | For CORS; add prod origin when deployed |
| PKCE Code Challenge | `S256` | Required for SPA security |
| User Registration | `Enabled` (in realm) | Required for "Register" flow |

**Backend CORS (if web and backend on different origins):**
- Backend `SecurityConfig.java` already allows `http://localhost:*` origins
- For production: ensure backend CORS config includes web origin (environment config, not code change)

---

## Stories

| ID | Title | Status | Priority | Points |
|----|-------|--------|----------|--------|
| ST-301 | Keycloak OIDC Integration | Ready | P1 | 3 |
| ST-302 | Login & Registration Screens | Ready | P1 | 2 |
| ST-303 | Session Management & Error UX | Ready | P1 | 3 |
| ST-304 | E2E Onboarding Flow & Docs | Ready | P1 | 2 |

### Sprint Mapping
- **Sprint S03 (committed):** ST-301, ST-302, ST-303, ST-304

---

## Dependencies

| Dependency | Type | Status | Notes |
|------------|------|--------|-------|
| Web Client (S02) | Internal | Done | Auth context, routes, build |
| Backend API | Internal | Done | GET /users/me auto-provision |
| Keycloak | External | Ready | Local instance, needs realm config |
| oidc-client-ts | NPM | Available | Industry-standard OIDC library |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Keycloak realm config complexity | Blocks auth flow | Verify config before ST-301 start |
| Silent refresh fails | Poor UX (frequent re-login) | Fallback to explicit re-auth |
| CORS with credentials | Blocks token flow | Test early, adjust backend if needed |
| Scope creep to NEXT features | Delays NOW | Strict Out of Scope enforcement |

---

## Exit Criteria (NOW delivered)

From initiative INIT-2026Q1-onboarding-registration:

1. New user can register via Keycloak (redirect to Keycloak login page with "Register" option enabled in realm)
2. User can login via Keycloak OIDC (PKCE flow)
3. After login, GET /users/me returns profile (auto-created if new)
4. Session persists across page refresh via `sessionStorage` (cleared on tab/browser close)
5. Logout clears session and redirects to login
6. Clear error messages for auth failures
7. After login, user lands in app and sees:
   - Household selector (if user has households), OR
   - Empty-state "No households yet" with guidance (if user has zero households; household creation is NEXT initiative scope)
8. Documentation updated

> **Clarification (Fix D):** NOW increment does NOT include household creation. Users with zero households see read-only empty-state.
> Household create/join flow is in INIT-2026Q1-household-lifecycle (NEXT).

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | Consuming existing API |
| adr_needed | maybe | ADR for auth strategy if significant decisions |
| diagrams_needed | no | No structural changes |
| security_sensitive | yes | Auth/session handling |

---

## Related Artifacts

| Artifact | Path |
|----------|------|
| Initiative | `docs/planning/initiatives/INIT-2026Q1-onboarding-registration.md` |
| OpenAPI | `docs/contracts/http/commands.openapi.yaml` |
| Service Catalog | `docs/architecture/service-catalog.md` |
| Backend Security | `services/backend/src/main/java/com/hometusk/config/SecurityConfig.java` |
| Web Auth Context | `clients/web/src/context/AuthContext.tsx` |
