# Checklist: ST-304 — E2E Onboarding Flow & Docs

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-304/workpack.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Definition of Ready (DoR)

- [x] Story has clear title and description
- [x] Acceptance criteria defined
- [ ] ST-301, ST-302, ST-303 complete

---

## Definition of Done (DoD)

### E2E Testing
- [ ] New user: register -> login -> households
- [ ] Existing user: login -> tasks
- [ ] Error: expired token -> redirect to login
- [ ] Error: logout -> session cleared

### Documentation
- [ ] local-dev.md updated with Keycloak setup
- [ ] web README updated with auth section
- [ ] .env.example has all VITE_OIDC_* vars

### Quality
- [ ] Documentation is clear and concise
- [ ] No broken links
- [ ] Steps are reproducible

---

## Acceptance Criteria Verification

| AC | Status | Evidence |
|----|--------|----------|
| New user E2E works | [ ] | Screenshot/video |
| Existing user E2E works | [ ] | Screenshot/video |
| local-dev.md updated | [ ] | Diff/commit |
| web README updated | [ ] | Diff/commit |
| .env.example complete | [ ] | File review |
