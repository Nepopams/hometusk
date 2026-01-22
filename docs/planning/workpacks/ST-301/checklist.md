# Checklist: ST-301 — Keycloak OIDC Integration

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-301/workpack.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Definition of Ready (DoR)

- [x] Story has clear title and description
- [x] Acceptance criteria defined (Given/When/Then)
- [x] In scope / out of scope explicit
- [x] Technical approach identified
- [x] Dependencies identified (web baseline, Keycloak)
- [x] Files to change listed
- [x] Verification commands defined
- [ ] Keycloak realm configured (Fix C):
  - [ ] Client `hometusk-web` created (public client)
  - [ ] Valid Redirect URIs: `http://localhost:5173/callback`
  - [ ] Web Origins: `http://localhost:5173`
  - [ ] PKCE enabled (code_challenge_method = S256)
  - [ ] User registration enabled in realm (Login tab)
  - [ ] Backend CORS allows web origin (already configured)

---

## Definition of Done (DoD)

### Code Quality
- [x] oidc-client-ts installed
- [x] `src/lib/auth/oidc.ts` created with UserManager
- [x] `src/routes/Callback.tsx` created
- [x] `/callback` route added to router
- [x] `.env.example` updated
- [x] Type definitions added for env vars
- [x] No lint errors: `npm run lint`
- [x] Build passes: `npm run build`

### Functionality
- [x] UserManager initializes without errors (code review: lazy init with validation)
- [x] `signinRedirect()` redirects to Keycloak (code review: exported, throws on invalid config)
- [x] `/callback` handles successful login (code review: saves token, navigates)
- [x] `/callback` handles errors gracefully (code review: logs, shows minimal error UI)
- [x] Redirect to /households after successful callback (code review: `navigate('/households', { replace: true })`)

### Testing
- [ ] Manual test: OIDC redirect flow works (pending Keycloak)
- [ ] Manual test: Callback processes tokens (pending Keycloak)
- [ ] Manual test: Error case doesn't crash app (pending Keycloak)

### Documentation
- [ ] (Defer to ST-304)

### Security
- [x] PKCE code_challenge included in auth request (response_type: 'code' enables PKCE)
- [x] No tokens logged to console (only errors logged, no token values)
- [x] Env vars not hardcoded (all from import.meta.env)

---

## Acceptance Criteria Verification

| AC | Status | Evidence |
|----|--------|----------|
| OIDC redirect includes PKCE | [x] | Code: `response_type: 'code'` (oidc-client-ts adds PKCE automatically) |
| Callback exchanges code for tokens | [x] | Code: `signinRedirectCallback()` in Callback.tsx |
| Redirect to /households after callback | [x] | Code: `navigate('/households', { replace: true })` |
| Invalid config shows error (no crash) | [x] | Code: `console.error('[OIDC]...')` + throws with message |

---

## Code Review Summary (2026-01-22)

**Reviewer:** Claude Code
**Result:** ✅ PASS

**Files verified:**
- `clients/web/package.json` — oidc-client-ts ^3.0.0 added
- `clients/web/src/lib/auth/oidc.ts` — 60 lines, correct implementation
- `clients/web/src/routes/Callback.tsx` — 67 lines, token bridging correct
- `clients/web/src/routes/index.tsx` — /callback route added
- `clients/web/.env.example` — OIDC vars documented
- `clients/web/src/vite-env.d.ts` — optional types for OIDC vars

**Scope compliance:**
- ✅ AuthContext.tsx NOT modified
- ✅ Login.tsx NOT modified
- ✅ No token refresh logic
- ✅ No error UI beyond minimal

**Notes:**
- npm audit reports 5 vulnerabilities (transitive deps) — not a blocker, track for patching
- Manual testing pending Keycloak realm configuration
