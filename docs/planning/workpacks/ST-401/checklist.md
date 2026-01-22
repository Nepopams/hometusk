# Checklist: ST-401 — Household Selector & Empty State

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-401/workpack.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Definition of Ready (DoR)

- [ ] Story has clear title and description
- [ ] Acceptance criteria defined
- [ ] In scope / out of scope explicit
- [ ] Files to change listed
- [ ] API dependencies documented
- [ ] S03 complete (auth context working)
- [ ] `GET /users/me` returns households field

---

## Definition of Done (DoD)

### Code Quality
- [ ] `HouseholdContext.tsx` created
- [ ] `HouseholdSelector.tsx` created
- [ ] `EmptyHouseholdState.tsx` created
- [ ] Layout updated with selector
- [ ] No lint errors: `npm run lint`
- [ ] Build passes: `npm run build`

### Functionality
- [ ] Selector displays current household name
- [ ] Dropdown shows all households with roles
- [ ] Clicking household switches context
- [ ] Selection persists across refresh (sessionStorage)
- [ ] Empty state shows when 0 households
- [ ] "Create Household" CTA navigates to form

### Testing
- [ ] Manual test: multi-household selector works
- [ ] Manual test: empty state displays correctly
- [ ] Manual test: selection persists on refresh
- [ ] Manual test: invalid stored selection handled

### Documentation
- [ ] (Defer to ST-405 or separate docs story)

### Security
- [ ] No sensitive data in sessionStorage (only household ID)
- [ ] Households list comes from authenticated API

---

## Acceptance Criteria Verification

| AC | Status | Evidence |
|----|--------|----------|
| Multi-household dropdown works | [ ] | Manual test |
| Selection persists on refresh | [ ] | sessionStorage check |
| Empty state shows for 0 households | [ ] | Manual test |
| CTA navigates to create form | [ ] | Manual test |

---

## Notes
(To be filled during implementation)
