# Checklist: ST-402 — Create Household Form

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-402/workpack.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Definition of Ready (DoR)

- [ ] Story approved
- [ ] API endpoint documented in OpenAPI
- [ ] ST-401 complete (HouseholdContext exists)
- [ ] AuthContext has refetchUser capability

---

## Definition of Done (DoD)

### Code Quality
- [ ] `CreateHousehold.tsx` created
- [ ] `lib/api/households.ts` has createHousehold function
- [ ] Route `/households/new` added
- [ ] No lint errors
- [ ] Build passes

### Functionality
- [ ] Form displays with name input
- [ ] Validation: empty name shows error
- [ ] Validation: >80 chars shows error
- [ ] Submit calls POST /households
- [ ] Loading state during submission
- [ ] Success: profile refreshed, household selected
- [ ] Success: redirect to dashboard
- [ ] Error 400: validation message shown
- [ ] Error 401: redirect to login

### Testing
- [ ] Manual test: create household E2E
- [ ] Manual test: validation errors
- [ ] Manual test: network error recovery

### Security
- [ ] No sensitive data logged
- [ ] Auth required for page access

---

## Acceptance Criteria Verification

| AC | Status | Evidence |
|----|--------|----------|
| Valid name creates household | [ ] | Manual test |
| Invalid name shows error | [ ] | Manual test |
| New household appears in selector | [ ] | Manual test |
| User becomes admin | [ ] | API response check |

---

## Notes
(To be filled during implementation)
