# Workpack: ST-304 — E2E Onboarding Flow & Docs

## Sources of Truth
- Story: `docs/planning/epics/EP-004/stories/ST-304-e2e-docs.md`
- Epic: `docs/planning/epics/EP-004/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-onboarding-registration.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Goal
Validate complete E2E onboarding flow and update documentation.

## User Value
Developers can set up and troubleshoot auth system easily.

---

## In Scope
- E2E manual testing of full onboarding flow
- Documentation updates for auth setup
- Environment variable documentation

## Out of Scope
- Automated E2E tests (NEXT)
- Performance testing (NEXT)

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `docs/runbooks/local-dev.md` | MODIFY | Add Keycloak setup |
| `clients/web/.env.example` | VERIFY | All vars documented |
| `clients/web/README.md` | MODIFY | Add auth setup section |

---

## Implementation Plan

### Task 1: E2E Testing
- Test new user: register -> login -> see households
- Test existing user: login -> see tasks
- Test error flows: expired token, logout

### Task 2: Update local-dev.md
- Add Keycloak realm setup steps
- Add client configuration steps
- Add troubleshooting section

### Task 3: Update web README
- Add auth setup section
- Explain dev mode vs Keycloak mode
- List required environment variables

### Task 4: Verify .env.example
- Ensure all VITE_* vars are documented
- Add comments explaining each var

---

## Verification Commands

```bash
# Documentation renders correctly
# (manual check)

# All env vars documented
grep -r "VITE_" clients/web/src | grep "import.meta.env" | sort | uniq
# Compare with .env.example
```

---

## Demo Scenario (E2E)

**New User Flow:**
1. Clear Keycloak user (if exists)
2. Visit app -> /login
3. Click "Register"
4. Complete Keycloak registration
5. Verify redirect to /households
6. Verify GET /users/me returns new profile

**Existing User Flow:**
1. Visit app -> /login
2. Click "Sign in"
3. Complete Keycloak login
4. Verify redirect to /households
5. Select household
6. Verify tasks list loads

---

## Risks

| Risk | Mitigation |
|------|------------|
| Documentation gets out of date | Keep it minimal, link to Keycloak docs |

---

## Rollback
- Documentation changes can be reverted

---

## Anti-Scope-Creep

DO NOT:
- Add automated E2E tests (NEXT)
- Add performance benchmarks (NEXT)
- Add user guides beyond dev setup
