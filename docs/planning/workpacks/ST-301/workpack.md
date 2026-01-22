# Workpack: ST-301 — Keycloak OIDC Integration

## Sources of Truth
- Story: `docs/planning/epics/EP-004/stories/ST-301-oidc-integration.md`
- Epic: `docs/planning/epics/EP-004/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-onboarding-registration.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- Web Baseline: `clients/web/`

---

## Goal
Integrate Keycloak OIDC into web client using oidc-client-ts with PKCE flow.

## User Value
Enable secure authentication via Keycloak without manual token handling.

---

## In Scope
- Add oidc-client-ts npm dependency
- Create OIDC configuration module
- Implement PKCE authorization code flow
- Add /callback route
- Add environment variable support

## Out of Scope
- UI changes (ST-302)
- Token refresh (ST-303)
- Error UX (ST-303)
- Documentation (ST-304)

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `clients/web/package.json` | MODIFY | Add oidc-client-ts dependency |
| `clients/web/src/lib/auth/oidc.ts` | CREATE | UserManager configuration and wrapper |
| `clients/web/src/routes/Callback.tsx` | CREATE | Handle OIDC redirect callback |
| `clients/web/src/routes/index.tsx` | MODIFY | Add /callback route |
| `clients/web/.env.example` | MODIFY | Add OIDC environment variables |
| `clients/web/src/vite-env.d.ts` | MODIFY | Type definitions for env vars |

---

## Implementation Plan

### Commit 1: Add oidc-client-ts dependency
```bash
cd clients/web && npm install oidc-client-ts
```
- Update package.json
- Verify npm ci works

### Commit 2: Create OIDC configuration module
- Create `src/lib/auth/oidc.ts`
- Export UserManager instance
- Export helper functions: `signinRedirect()`, `signinCallback()`
- Handle missing env vars gracefully

### Commit 3: Add Callback route
- Create `src/routes/Callback.tsx`
- Handle `signinCallback()` result
- Redirect to /households on success
- Handle errors (log, show error state)

### Commit 4: Wire up routing
- Add `/callback` route to router
- Update `.env.example` with required vars

---

## API Dependencies

| Endpoint | Method | Purpose |
|----------|--------|---------|
| GET /users/me | GET | Validate token after OIDC callback |

**External:**
- Keycloak authorization endpoint
- Keycloak token endpoint

---

## Environment Variables

```bash
# .env.example additions
VITE_AUTH_PROVIDER=keycloak  # or 'dev' for token paste
VITE_OIDC_AUTHORITY=http://localhost:8080/realms/hometusk
VITE_OIDC_CLIENT_ID=hometusk-web
VITE_OIDC_REDIRECT_URI=http://localhost:5173/callback
```

---

## Verification Commands

```bash
# Build passes
cd clients/web && npm run build

# Lint passes
cd clients/web && npm run lint

# Dev server starts
cd clients/web && npm run dev
```

---

## Demo Scenario (Manual)

1. Set VITE_AUTH_PROVIDER=keycloak in .env
2. Start web dev server
3. Open browser console
4. Call `import { signinRedirect } from './lib/auth/oidc'; signinRedirect()`
5. Verify redirect to Keycloak
6. Complete Keycloak login
7. Verify redirect to /callback
8. Verify redirect to /households

---

## Risks

| Risk | Mitigation |
|------|------------|
| Keycloak realm not configured | Document required realm settings, test before start |
| CORS issues with token endpoint | Keycloak handles CORS, verify config |
| PKCE not supported by Keycloak | Keycloak 17+ supports PKCE by default |
| Session lost on page refresh | Use sessionStorage for OIDC state (MVP decision) |

---

## Decision Notes

### Token/Session Storage Strategy (Fix A)

**MVP Decision:** Use `sessionStorage` via oidc-client-ts `WebStorageStateStore`.

```typescript
userStore: new WebStorageStateStore({ store: window.sessionStorage })
```

**Why sessionStorage (not memory-only):**
- Requirement: "session persists across page refresh"
- sessionStorage survives refresh, clears on tab close
- Acceptable tradeoff for MVP

**Future hardening (NEXT/LATER):**
- Consider memory-only store + silent refresh for enhanced security

---

## Keycloak Prereqs Checklist (Fix C)

Before starting implementation, verify:
- [ ] Client `hometusk-web` exists (public client)
- [ ] Valid Redirect URIs: `http://localhost:5173/callback`
- [ ] Web Origins: `http://localhost:5173`
- [ ] PKCE enabled (S256)
- [ ] User registration enabled in realm (if Register flow needed)
- [ ] Backend CORS allows web origin (already configured for localhost)

---

## Rollback
- Revert package.json changes
- Remove new files
- VITE_AUTH_PROVIDER=dev continues to work

---

## Anti-Scope-Creep

DO NOT:
- Add token refresh logic (ST-303)
- Modify Login.tsx UI (ST-302)
- Add error UI components (ST-303)
- Update documentation (ST-304)
