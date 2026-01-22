# Checklist: ST-403 — Create Invite & Share

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-403/workpack.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Definition of Ready (DoR)

- [ ] Story approved
- [ ] ADR-010 reviewed (invite semantics)
- [ ] ST-401 complete (household context)
- [ ] API endpoint documented in OpenAPI

---

## Definition of Done (DoD)

### Code Quality
- [ ] `InviteModal.tsx` created
- [ ] `lib/api/invites.ts` created with createInvite
- [ ] Members page has "Invite Member" button
- [ ] No lint errors
- [ ] Build passes

### Functionality
- [ ] Click "Invite" → modal opens
- [ ] Loading state while creating
- [ ] Token and link displayed on success
- [ ] "Copy link" copies to clipboard
- [ ] Expiry info shown (7 days)
- [ ] Error 403 → error message
- [ ] Close modal works

### Testing
- [ ] Manual test: create invite E2E
- [ ] Manual test: copy link works
- [ ] Manual test: link is valid (opens accept page)

### Security
- [ ] householdId from context (not user input)
- [ ] Token not logged to console

---

## Acceptance Criteria Verification

| AC | Status | Evidence |
|----|--------|----------|
| Invite token generated | [ ] | API response |
| Link copied to clipboard | [ ] | Paste test |
| Expiry shown | [ ] | UI check |
| 403 handled | [ ] | Manual test with non-member |

---

## Notes
(To be filled during implementation)
