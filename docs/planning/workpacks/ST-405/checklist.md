# Checklist: ST-405 — Members List View

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-405/workpack.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Definition of Ready (DoR)

- [ ] Story approved
- [ ] ST-401 complete (household context)
- [ ] ST-403 complete (invite modal to integrate)
- [ ] API endpoint documented in OpenAPI

---

## Definition of Done (DoD)

### Code Quality
- [ ] `Members.tsx` created
- [ ] `MembersList.tsx` component created
- [ ] `lib/api/households.ts` has getMembers
- [ ] Route added
- [ ] Nav link added
- [ ] No lint errors
- [ ] Build passes

### Functionality
- [ ] Members page loads
- [ ] List displays name, role, joined date
- [ ] Role badge shown
- [ ] "Invite Member" button opens modal
- [ ] Loading state works
- [ ] Error state shows with retry

### Testing
- [ ] Manual test: view members list
- [ ] Manual test: single-member household
- [ ] Manual test: invite button works

### Security
- [ ] householdId from context
- [ ] 403 handled (shouldn't occur)

---

## Acceptance Criteria Verification

| AC | Status | Evidence |
|----|--------|----------|
| Members list displays | [ ] | Manual test |
| Role badges shown | [ ] | UI check |
| Invite button works | [ ] | Manual test |

---

## Notes
(To be filled during implementation)
