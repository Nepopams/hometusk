# Checklist: ST-404 — Accept Invite Flow

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-404/workpack.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Definition of Ready (DoR)

- [ ] Story approved
- [ ] ADR-010 reviewed (invite semantics)
- [ ] ST-401 complete (HouseholdContext)
- [ ] ST-403 tested (can create invites to test)
- [ ] API endpoint documented in OpenAPI

---

## Definition of Done (DoD)

### Code Quality
- [ ] `AcceptInvite.tsx` created
- [ ] `lib/api/invites.ts` has acceptInvite function
- [ ] Route `/invite` added
- [ ] No lint errors
- [ ] Build passes

### Functionality
- [ ] Token read from URL `?token=xxx`
- [ ] Auto-accept when authenticated
- [ ] Loading state during accept
- [ ] 200: join household, redirect to dashboard
- [ ] 404: "Invalid invite" error
- [ ] 410: "Expired" error
- [ ] 401: redirect to login, preserve token
- [ ] After login: redirect back to invite page
- [ ] Profile refreshed on success

### Testing
- [ ] Manual test: E2E accept flow (authenticated)
- [ ] Manual test: E2E accept flow (not authenticated → login → accept)
- [ ] Manual test: invalid token (404)
- [ ] Manual test: expired token (410)
- [ ] Manual test: already member (200, no-op)

### Security
- [ ] Request contains ONLY inviteToken (anti-IDOR)
- [ ] Token not logged to console
- [ ] Stored pending token cleared after use

---

## Acceptance Criteria Verification

| AC | Status | Evidence |
|----|--------|----------|
| Valid invite → join household | [ ] | Manual test |
| Invalid token → 404 error | [ ] | Manual test |
| Expired → 410 error | [ ] | Manual test |
| Login redirect preserves token | [ ] | Manual test |
| Already member → success (no-op) | [ ] | Manual test |

---

## Notes
(To be filled during implementation)
