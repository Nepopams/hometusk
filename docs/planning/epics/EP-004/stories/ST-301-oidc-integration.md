# Story: ST-301 — Keycloak OIDC Integration

## Sources of Truth
- Epic: `docs/planning/epics/EP-004/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-onboarding-registration.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Summary
Integrate Keycloak OIDC into the web client using `oidc-client-ts` library with PKCE flow.

## User Value
As a user, I want to authenticate via Keycloak so that I can securely access HomeTusk without pasting JWT tokens.

---

## In Scope
- Add `oidc-client-ts` dependency
- Create `lib/auth/oidc.ts` module with UserManager configuration
- Implement PKCE authorization code flow
- Add `/callback` route for OIDC redirect
- Environment variables for Keycloak config:
  - `VITE_OIDC_AUTHORITY` (Keycloak realm URL)
  - `VITE_OIDC_CLIENT_ID`
  - `VITE_OIDC_REDIRECT_URI`
- Integrate with existing AuthContext

## Out of Scope
- UI changes (ST-302)
- Token refresh logic (ST-303)
- Error UX (ST-303)
- Documentation (ST-304)

---

## Acceptance Criteria

```gherkin
Given the web application is loaded
And VITE_AUTH_PROVIDER=keycloak is set
When user initiates login
Then browser redirects to Keycloak authorization endpoint
And redirect includes PKCE code_challenge

Given user completes Keycloak login
When Keycloak redirects to /callback
Then application exchanges code for tokens via PKCE
And access_token is obtained
And user is redirected to /households

Given OIDC config is invalid or Keycloak unavailable
When login is initiated
Then clear error is logged (no crash)
And user sees error state (handled by ST-303)
```

---

## Technical Notes

**Library:** `oidc-client-ts` (TypeScript rewrite of oidc-client-js)

**UserManager config:**
```typescript
const config: UserManagerSettings = {
  authority: import.meta.env.VITE_OIDC_AUTHORITY,
  client_id: import.meta.env.VITE_OIDC_CLIENT_ID,
  redirect_uri: import.meta.env.VITE_OIDC_REDIRECT_URI,
  response_type: 'code',
  scope: 'openid profile email',
  automaticSilentRenew: false, // handled in ST-303
  userStore: new WebStorageStateStore({ store: window.sessionStorage }), // MVP baseline
};
```

---

## Decision Notes

### Token/Session Storage (Fix A)

**Decision:** Use `sessionStorage` for OIDC state persistence (MVP baseline).

**Rationale:**
- Session survives page refresh (exit criteria requirement)
- Session cleared on tab/browser close (acceptable for MVP)
- oidc-client-ts supports `WebStorageStateStore` with sessionStorage

**Risks:**
- Multi-tab: each tab has separate session (acceptable for MVP)
- Not "memory-only" (future hardening in NEXT/LATER)

**Mitigation:**
- Document limitation: "single tab session"
- Future: consider in-memory store with silent refresh for enhanced security

### Keycloak Client Prereqs (Fix C)

Before implementation, verify Keycloak client is configured per `docs/planning/epics/EP-004/epic.md#keycloak-client-configuration`.

**Files to create/modify:**
- `clients/web/package.json` — add oidc-client-ts
- `clients/web/src/lib/auth/oidc.ts` — UserManager wrapper
- `clients/web/src/routes/Callback.tsx` — handle redirect
- `clients/web/src/routes/index.tsx` — add /callback route
- `clients/web/.env.example` — add OIDC vars

---

## Test Strategy

**Unit tests:**
- UserManager initialization with valid config
- UserManager initialization with missing config (throws/handles)

**Integration tests:**
- Manual: Keycloak redirect flow (local Keycloak instance)

---

## Flags

| Flag | Value |
|------|-------|
| contract_impact | no |
| adr_needed | no |
| security_sensitive | yes |

---

## Dependencies
- Web client baseline (S02 complete)
- Keycloak local instance configured

## Points
3 (library integration + new module + route)

## Priority
P1 (blocks ST-302, ST-303)
