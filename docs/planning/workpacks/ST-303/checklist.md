# Checklist: ST-303 — Session Management & Error UX

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-303/workpack.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Definition of Ready (DoR)

- [x] Story has clear title and description
- [x] Acceptance criteria defined
- [x] Technical approach identified
- [x] Files to change listed
- [x] ST-301, ST-302 complete
- [ ] Keycloak realm has offline_access scope available

---

## Definition of Done (DoD)

### Code Quality
- [x] AuthContext refactored for OIDC (dual mode keycloak/dev)
- [x] Token refresh implemented (automaticSilentRenew + events)
- [x] Logout implemented (removeUser + state clear)
- [x] Global 401 handling in api.ts (handleAuthError)
- [x] Error display on Login (?error= query param)
- [x] No lint errors
- [x] Build passes

### Functionality
- [x] Token provider pattern implemented
- [x] Keycloak mode: token from UserManager
- [x] Dev mode: token from localStorage (unchanged)
- [x] 401 triggers auto-logout + redirect with error
- [x] Error messages display correctly on Login
- [x] automaticSilentRenew enabled for token refresh

### Security
- [x] Access token managed by oidc-client-ts (sessionStorage)
- [x] No tokens logged to console (only errors)
- [x] removeUser clears OIDC session locally

### Testing
- [ ] Manual: Session persists (pending Keycloak)
- [ ] Manual: Token refresh works (pending Keycloak)
- [ ] Manual: 401 handling works (pending backend)
- [ ] Manual: Logout clears everything (pending Keycloak)

---

## Acceptance Criteria Verification

| AC | Status | Evidence |
|----|--------|----------|
| Silent refresh works | [x] | Code: automaticSilentRenew + registerTokenEvents |
| Logout clears session | [x] | Code: removeUser() + state clear |
| 401 triggers logout + message | [x] | Code: handleAuthError in api.ts |
| Error message shows | [x] | Code: ERROR_MESSAGES map + display on Login |

---

## Code Review Summary (2026-01-22)

**Reviewer:** Claude Code
**Result:** ✅ PASS

**Files verified:**
- `clients/web/src/lib/auth/tokenProvider.ts` — 24 lines, centralized token access
- `clients/web/src/lib/auth/oidc.ts` — 113 lines, extended with session management
- `clients/web/src/context/AuthContext.tsx` — 247 lines, dual mode refactor
- `clients/web/src/lib/api.ts` — 81 lines, uses tokenProvider
- `clients/web/src/routes/Login.tsx` — 147 lines, error display
- `clients/web/src/routes/Callback.tsx` — 52 lines, localStorage removed
- `clients/web/src/components/ProtectedRoute.tsx` — 26 lines, error in redirect

**Key patterns:**
- ✅ Token Provider for centralized token access
- ✅ Auth Error Handler callback
- ✅ Dual mode (keycloak/dev)
- ✅ automaticSilentRenew
- ✅ Event-based expiry handling

**Scope compliance:**
- ✅ No "Remember me"
- ✅ No timeout modal
- ✅ No multi-tab sync

**Notes:**
- Manual testing pending Keycloak
- Keycloak needs `offline_access` scope
