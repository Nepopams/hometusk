# Story: ST-304 — E2E Onboarding Flow & Docs

## Sources of Truth
- Epic: `docs/planning/epics/EP-004/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q1-onboarding-registration.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Summary
Validate the complete E2E onboarding flow and update documentation.

## User Value
As a developer/operator, I want clear documentation so that I can set up and troubleshoot the auth system.

---

## In Scope

### E2E Validation
- Test complete flow: visit app -> register -> login -> see households
- Test complete flow: visit app -> login (existing user) -> see tasks
- Verify UserProfile auto-creation (new user)
- Verify error flows work as designed

### Documentation Updates
- Update `docs/runbooks/local-dev.md` with Keycloak setup
- Create/update auth strategy doc (if ADR needed)
- Update `.env.example` with all auth-related vars
- Update `clients/web/README.md` with auth setup instructions
- Update service catalog if needed

## Out of Scope
- Automated E2E tests (NEXT)
- Performance testing (NEXT)

---

## Acceptance Criteria

```gherkin
Given a new user with no HomeTusk account
When they visit the app, click Register, complete Keycloak registration
And Keycloak redirects back to app
Then GET /users/me returns their profile (auto-created)
And they see household selector (empty list OK)

Given an existing user
When they visit the app, click Sign in, complete Keycloak login
Then they see their households
And can navigate to tasks

Given documentation
Then local-dev.md includes Keycloak realm setup steps
And .env.example includes all VITE_OIDC_* variables
And web README includes auth setup section
```

---

## Documentation Checklist

- [ ] `docs/runbooks/local-dev.md` — Keycloak realm configuration
- [ ] `clients/web/.env.example` — OIDC environment variables
- [ ] `clients/web/README.md` — Auth setup instructions
- [ ] `docs/architecture/service-catalog.md` — Update if needed

---

## Flags

| Flag | Value |
|------|-------|
| contract_impact | no |
| adr_needed | no |
| security_sensitive | no |

---

## Dependencies
- ST-301, ST-302, ST-303 complete

## Points
2 (testing + docs)

## Priority
P1
