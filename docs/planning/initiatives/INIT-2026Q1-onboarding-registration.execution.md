# INIT-2026Q1-onboarding-registration — Execution Index

**Initiative:** Registration & Sign-in (Web)
**Status:** Approved for Sprint S03
**Last Updated:** 2026-01-22

---

## Sources of Truth (Files Consulted)

### Planning & Strategy
- Initiative Scope: `docs/planning/initiatives/INIT-2026Q1-onboarding-registration.md`
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- PI Charter: `docs/planning/pi/2026Q1-PI01/pi.md`

### Governance
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- CLAUDE.md: Project working agreement

### Contracts & Architecture
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- Service Catalog: `docs/architecture/service-catalog.md`
- Backend Security: `services/backend/src/main/java/com/hometusk/config/SecurityConfig.java`

### Baseline (Web Client)
- S02 Complete: `docs/planning/pi/2026Q1-PI01/sprints/S02/scope.md`
- Web Auth Context: `clients/web/src/context/AuthContext.tsx`
- Web Routes: `clients/web/src/routes/index.tsx`
- Web Login: `clients/web/src/routes/Login.tsx`

---

## Current State Snapshot (Baseline for Onboarding)

### Backend Auth (Post-MVP, Stable)

**Strategy:**
- Spring Security OAuth2 Resource Server
- Keycloak JWT validation
- Stateless sessions

**Key Endpoints:**
- `GET /api/v1/users/me` — returns profile, auto-creates from JWT if new

**Contract Semantics:**
- 401 -> token invalid/expired
- 403 -> not household member

### Web Client Auth (Post-S02)

**Current Implementation:**
- Dev mode only (token paste)
- Token stored in localStorage
- AuthContext: login(token) -> GET /users/me

**What's Missing:**
- Keycloak OIDC redirect (PKCE)
- Real login/register screens
- Token refresh
- Proper logout

---

## Epic Decomposition

### EP-004: Registration & Sign-in (NOW Increment)

**Goal:** Enable real user onboarding via Keycloak OIDC.

**Stories:**

| ID | Title | Points | Status | Workpack | Sprint |
|----|-------|--------|--------|----------|--------|
| ST-301 | Keycloak OIDC Integration | 3 | Planned | `docs/planning/workpacks/ST-301/` | S03 |
| ST-302 | Login & Registration Screens | 2 | Planned | `docs/planning/workpacks/ST-302/` | S03 |
| ST-303 | Session Management & Error UX | 3 | Planned | `docs/planning/workpacks/ST-303/` | S03 |
| ST-304 | E2E Onboarding Flow & Docs | 2 | Planned | `docs/planning/workpacks/ST-304/` | S03 |

**Total (NOW):** 10 points committed (S03)

---

## Sprint Mapping

### Sprint S03 (Approved)

**Goal:** Registration & Sign-in (Web Onboarding)

**Committed:**
- ST-301: Keycloak OIDC Integration (3 pts)
- ST-302: Login & Registration Screens (2 pts)
- ST-303: Session Management & Error UX (3 pts)
- ST-304: E2E Onboarding Flow & Docs (2 pts)

**Total:** 10 points

**Sprint Artifacts:**
- Plan: `docs/planning/pi/2026Q1-PI01/sprints/S03/sprint.md`
- Scope: `docs/planning/pi/2026Q1-PI01/sprints/S03/scope.md`

**Status:** Gate B approved

---

## Workpack Structure (Per Story)

Each story has a workpack folder:

```
docs/planning/workpacks/<STORY_ID>/
├── workpack.md         # Implementation plan, files to change, verification
└── checklist.md        # DoR + DoD checklist
```

**Note:** Prompt packs (prompt-plan.md, prompt-apply.md, prompt-review.md) will be generated after Keycloak prereqs verified, not in this planning phase.

---

## Execution Pipeline (Per Story)

### Phase 1: PLAN
1. Developer (Codex) receives PLAN prompt
2. Codex reads workpack, story, contracts
3. Codex outputs detailed plan (NO edits)
4. **Human Gate C:** Approve plan

### Phase 2: APPLY
1. Developer (Codex) receives APPLY prompt
2. Codex implements according to approved plan
3. Codex runs verification commands
4. Codex reports results

### Phase 3: REVIEW
1. Reviewer uses REVIEW prompt
2. Verifies AC, DoD, security
3. Outputs GO/NO-GO report
4. **Human Gate D:** Approve merge

---

## Gate Sequence

### Gate A (Initiative Scope) DONE
- Initiative INIT-2026Q1-onboarding-registration approved
- NOW/NEXT/LATER boundaries clear
- Epic EP-004 scoped

### Gate B (Sprint Commitment) DONE
- Sprint S03 goal + committed scope approved
- 10 points committed
- Out of scope list approved
- Risks accepted

### Gate C (Story Plan Approval) — Per Story
- Approve Codex PLAN before APPLY

### Gate D (Merge Approval) — Per Story
- Approve merge after REVIEW

---

## Prerequisites (Before Sprint Start)

### Keycloak Configuration
- [ ] Client `hometusk-web` created in Keycloak realm
- [ ] Valid redirect URIs configured (http://localhost:5173/callback)
- [ ] PKCE enabled (code_challenge_method = S256)
- [ ] Self-registration enabled

### Dependencies
- [x] Sprint S02 completed and merged
- [x] Web client build passing

---

## Risks & Mitigations

| Risk | Impact | Mitigation | Status |
|------|--------|------------|--------|
| Keycloak config complexity | HIGH | Document setup, verify before sprint | Open |
| Silent refresh issues | MEDIUM | Fallback to explicit re-auth | Planned |
| CORS with credentials | MEDIUM | Test early, adjust backend if needed | Planned |
| Scope creep to NEXT | MEDIUM | Strict Out of Scope enforcement | Mitigated |

---

## Out of Scope (Explicit Boundaries)

### Deferred to NEXT Increment
- Password reset UI
- Email verification UI
- Invite-only registration
- Rate limiting

### Deferred to LATER Increment
- Social login
- Magic link
- Multi-device session

### Never in Onboarding Scope
- New backend microservices
- RBAC expansion
- Backend logic changes

---

## Success Metrics (NOW Increment)

From initiative exit criteria:

1. **Registration Works:**
   - New user can register via Keycloak web redirect
   - GET /users/me returns auto-created profile

2. **Login Works:**
   - User can login via Keycloak OIDC (PKCE)
   - Session persists (token refresh)

3. **Logout Works:**
   - Logout clears local + Keycloak session

4. **Error UX:**
   - Clear messages for auth failures
   - 401 handled gracefully

5. **Documentation:**
   - Auth setup documented

---

## Next Steps

### Immediate
1. Configure Keycloak prereqs (client, PKCE, self-registration)
2. Generate prompt-plan.md for ST-301
3. Execute ST-301 (PLAN -> approve -> APPLY -> REVIEW)

### Sequential Execution
1. ST-301 -> ST-302 -> ST-303 -> ST-304

### After Sprint S03
1. Demo onboarding flow
2. Retro Sprint S03
3. Plan NEXT increment (INIT-2026Q1-household-lifecycle or password reset)

---

## Artifacts Index

### Planning
- Initiative: `docs/planning/initiatives/INIT-2026Q1-onboarding-registration.md`
- Epic: `docs/planning/epics/EP-004/epic.md`
- Stories: `docs/planning/epics/EP-004/stories/ST-*.md`

### Workpacks
- ST-301: `docs/planning/workpacks/ST-301/`
- ST-302: `docs/planning/workpacks/ST-302/`
- ST-303: `docs/planning/workpacks/ST-303/`
- ST-304: `docs/planning/workpacks/ST-304/`

### Sprint
- S03 Plan: `docs/planning/pi/2026Q1-PI01/sprints/S03/sprint.md`
- S03 Scope: `docs/planning/pi/2026Q1-PI01/sprints/S03/scope.md`

### Contracts
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- Service Catalog: `docs/architecture/service-catalog.md`
