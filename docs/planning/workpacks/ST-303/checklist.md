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
- [ ] ST-301, ST-302 complete

---

## Definition of Done (DoD)

### Code Quality
- [ ] AuthContext refactored for OIDC
- [ ] Token refresh implemented
- [ ] Logout implemented
- [ ] Global 401 handling in api.ts
- [ ] Error display on Login
- [ ] No lint errors
- [ ] Build passes

### Functionality
- [ ] Session persists across page refresh
- [ ] Token refresh works (silent)
- [ ] Logout clears session + Keycloak
- [ ] 401 triggers auto-logout
- [ ] Error messages display correctly

### Security
- [ ] Access token stored in memory (not localStorage)
- [ ] No tokens logged to console
- [ ] Keycloak end-session called on logout

### Testing
- [ ] Manual: Session persists
- [ ] Manual: Token refresh works
- [ ] Manual: 401 handling works
- [ ] Manual: Logout clears everything

---

## Acceptance Criteria Verification

| AC | Status | Evidence |
|----|--------|----------|
| Silent refresh works | [ ] | Network tab shows token refresh |
| Logout clears session + Keycloak | [ ] | Keycloak shows no active session |
| 401 triggers logout | [ ] | Force 401, verify redirect |
| Error message shows | [ ] | Screenshot of error on /login |
