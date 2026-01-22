# Checklist: ST-304 — E2E Onboarding Flow & Docs

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-304/workpack.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Definition of Ready (DoR)

- [x] Story has clear title and description
- [x] Acceptance criteria defined
- [x] ST-301, ST-302, ST-303 complete

---

## Definition of Done (DoD)

### E2E Testing
- [ ] New user: register -> login -> households (pending Keycloak)
- [ ] Existing user: login -> tasks (pending Keycloak)
- [ ] Error: expired token -> redirect to login (pending Keycloak)
- [ ] Error: logout -> session cleared (pending Keycloak)

### Documentation
- [x] local-dev.md updated with Keycloak setup
- [x] web README updated with auth section
- [x] .env.example has all VITE_OIDC_* vars (port fixed to 8180)

### Quality
- [x] Documentation is clear and concise
- [x] No broken links
- [x] Steps are reproducible

---

## Acceptance Criteria Verification

| AC | Status | Evidence |
|----|--------|----------|
| New user E2E works | [ ] | Pending Keycloak test |
| Existing user E2E works | [ ] | Pending Keycloak test |
| local-dev.md updated | [x] | Web Client, Keycloak Config, E2E Checklist, Troubleshooting sections added |
| web README updated | [x] | Auth Setup section with env table |
| .env.example complete | [x] | Port 8180, comments added |

---

## Code Review Summary (2026-01-22)

**Reviewer:** Claude Code
**Result:** ✅ PASS (documentation complete, E2E pending Keycloak)

**Files verified:**
- `clients/web/.env.example` — port fixed to 8180, comments added
- `docs/runbooks/local-dev.md` — 4 new sections (Web Setup, Keycloak Config, E2E Checklist, Troubleshooting)
- `clients/web/README.md` — Auth Setup section with env table

**Notes:**
- Manual E2E testing requires Keycloak realm to be configured
- E2E checklist provided in local-dev.md for manual verification
