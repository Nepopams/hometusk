# Sprint S03 — Registration & Sign-in (Web Onboarding)

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q1-onboarding-registration.md`
- Epic: `docs/planning/epics/EP-004/epic.md`
- PI Charter: `docs/planning/pi/2026Q1-PI01/pi.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`

---

## Goal

Enable real user onboarding by implementing Keycloak OIDC authentication in the web client.

**Success Metric:** New user can register via Keycloak, login, and see household selector without manual token handling.

---

## Prioritization Rationale

This sprint implements the **NOW** increment of INIT-2026Q1-onboarding-registration:
1. **Auth is blocking real users:** Dev mode token paste not usable by non-technical users
2. **Foundation exists:** S02 delivered web client shell, auth context, routing
3. **Backend ready:** GET /users/me auto-creates profile

**Why now:**
- Roadmap marks this as current focus after web client foundation
- Enables product validation with real users
- No backend changes needed

---

## Scope

### Committed (DoR-ready, P1)

| Story | Title | Points | Workpack |
|-------|-------|--------|----------|
| ST-301 | Keycloak OIDC Integration | 3 | `docs/planning/workpacks/ST-301/` |
| ST-302 | Login & Registration Screens | 2 | `docs/planning/workpacks/ST-302/` |
| ST-303 | Session Management & Error UX | 3 | `docs/planning/workpacks/ST-303/` |
| ST-304 | E2E Onboarding Flow & Docs | 2 | `docs/planning/workpacks/ST-304/` |

**Total committed:** 10 points

**Deliverables:**
- Keycloak OIDC integration (PKCE)
- Real login/register screens
- Robust session management
- Updated documentation

---

### Out of Scope (explicit)

- **No password reset flow** — Keycloak handles, defer UI to NEXT
- **No email verification UI** — Keycloak handles, defer to NEXT
- **No invite-only registration** — defer to NEXT
- **No social login** — defer to LATER
- **No backend changes** — consume existing API only
- **No new microservices** — single backend

---

## Capacity Note

**Assumptions:**
- 1 developer (Codex)
- 2-3 days per story
- Total: ~8-12 days for committed scope

**Constraints:**
- Requires Keycloak realm properly configured before start
- Depends on S02 completion
- Integration testing with real Keycloak required

---

## Assumptions

1. Web client foundation (S02) is complete
2. Keycloak local instance available and configurable
3. oidc-client-ts library is stable and well-documented
4. Backend GET /users/me auto-provision works as documented
5. No breaking changes to API during sprint

---

## Dependencies

| Dependency | Owner | Status | Risk |
|------------|-------|--------|------|
| Web Client (S02) | Web | Ready (S02 done) | LOW |
| Backend API | Backend | Ready (MVP closed) | LOW |
| Keycloak | DevOps | Needs config | MEDIUM |
| oidc-client-ts | NPM | Available | LOW |

**Critical path:** ST-301 -> ST-302 -> ST-303 -> ST-304 (sequential)

---

## Risks & Mitigations

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Keycloak realm config complexity | HIGH | MEDIUM | Document setup, verify before sprint |
| Silent refresh not working | MEDIUM | LOW | Fallback to explicit re-auth |
| CORS issues with credentials | MEDIUM | LOW | Test early, adjust backend if needed |
| Scope creep to NEXT features | MEDIUM | MEDIUM | Strict Out of Scope enforcement |

---

## Definition of Ready Check

**DoR Status:** PENDING

Before sprint start, verify:
- [ ] Keycloak realm configured with:
  - [ ] Client `hometusk-web` created
  - [ ] Valid redirect URIs
  - [ ] PKCE enabled
  - [ ] Self-registration enabled
- [ ] S02 stories completed and merged

---

## Gate B Ask

**Request:** Approve Sprint S03 goal, committed scope (ST-301 through ST-304), and capacity note.

**What we commit to:**
1. Deliver Keycloak OIDC integration
2. Deliver real login/register screens
3. Deliver robust session management
4. Deliver updated documentation

**What we won't do:**
- Password reset UI
- Social login
- Backend changes

**Approval needed:**
- [x] Sprint goal approved
- [x] Committed scope (10 points) approved
- [x] Out of scope explicit list approved
- [x] Risks accepted
- [ ] Keycloak prereqs verified

---

## Sprint Artifacts

| Artifact | Path |
|----------|------|
| Sprint Plan | `docs/planning/pi/2026Q1-PI01/sprints/S03/sprint.md` (this file) |
| Scope Detail | `docs/planning/pi/2026Q1-PI01/sprints/S03/scope.md` |
| Demo Plan | `docs/planning/pi/2026Q1-PI01/sprints/S03/demo.md` (TBD) |
| Retro | `docs/planning/pi/2026Q1-PI01/sprints/S03/retro.md` (TBD) |

---

## Exit Criteria

Sprint is **done** when:
1. All committed stories completed (AC verified, DoD met)
2. `npm run build/lint` pass
3. Manual E2E test passes: register -> login -> households
4. Documentation updated
5. Demo prepared
6. Retro completed
