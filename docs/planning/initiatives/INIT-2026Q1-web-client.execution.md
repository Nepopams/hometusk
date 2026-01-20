# INIT-2026Q1-web-client — Execution Index

**Initiative:** Simple Web Client (Desktop-first)
**Status:** Ready for Gate B (Sprint S02 approval)
**Last Updated:** 2026-01-20

---

## Sources of Truth (Files Consulted)

### Planning & Strategy
- Initiative Scope: `docs/planning/initiatives/INIT-2026Q1-web-client.md`
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- PI Charter: `docs/planning/pi/2026Q1-PI01/pi.md`

### Governance
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- CLAUDE.md: Project working agreement

### Contracts & Architecture
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- API Coverage: `docs/mvp/api-coverage.md`
- Service Catalog: `docs/architecture/service-catalog.md`

### Baseline
- MVP Exit Review: `docs/planning/releases/reviews/MVP-exit-review.md`

---

## Current State Snapshot (Baseline for Web Client)

### Backend API (Post-MVP, Stable)

**Top 10 Endpoints for Web Client (NOW Increment):**

| Endpoint | Method | Purpose | Status |
|----------|--------|---------|--------|
| `/users/me` | GET | User profile + households | Ready |
| `/households` | POST | Create household | Ready |
| `/households/{id}/members` | GET | List members (for assignee filter) | Ready |
| `/households/{id}/zones` | GET | List zones (for zone filter) | Ready |
| `/households/{id}/zones` | POST | Create zone | Ready |
| `/households/{id}/tasks` | GET | List tasks with filters | Ready |
| `/households/{id}/tasks/{taskId}` | GET | Task detail (ST-204) | Ready |
| `/households/{id}/invites` | POST | Create invite | Ready |
| `/invites/accept` | POST | Accept invite | Ready |
| `/households/{id}/notifications` | GET | List notifications | Ready |
| `/notifications/{notificationId}/read` | POST | Mark notification read | Ready |

**Auth Strategy:**
- Keycloak JWT (Bearer token)
- Dev mode: token paste (acceptable for NOW)
- Target mode: OIDC redirect (defer to NEXT)

**Contract Semantics (Critical for Web):**
- **401** → logout + redirect to /login
- **403** → not a household member (access denied)
- **404** → resource not found
- **410** → invite expired/redeemed/revoked
- **Idempotency-Key** → optional header for commands (24h TTL)
- **X-Correlation-ID** → tracing header (server generates if missing)

**Household Boundary Enforcement:**
- All endpoints require membership check
- GET /users/me returns only user's households
- No cross-household data leaks (verified in HouseholdBoundarySecurityTest)

---

## Epic Decomposition

### EP-003: Web Foundation (NOW Increment)

**Goal:** Deliver working web client with auth and read-first tasks view.

**Stories:**

| ID | Title | Points | Status | Workpack | Sprint |
|----|-------|--------|--------|----------|--------|
| ST-201 | Web Foundation (Project Setup & Build) | 3 | Ready | `docs/planning/workpacks/ST-201/` | S02 |
| ST-202 | Auth Integration + Household Selector | 3 | Ready | `docs/planning/workpacks/ST-202/` | S02 |
| ST-203 | Tasks List & Filters | 3 | Ready | `docs/planning/workpacks/ST-203/` | S02 |
| ST-204 | Task Detail View | 2 | Draft | — | S03 (planned) |
| ST-205 | Zones Navigation | 2 | Draft | — | S03 (planned) |
| ST-206 | Notifications Inbox | 2 | Draft | — | S03 (planned) |

**Total (NOW):** 9 points committed (S02)
**Total (Epic):** 15 points (6 stories)

---

## Sprint Mapping

### Sprint S02 (Current)

**Goal:** Web Client Foundation (NOW Increment - Read-First)

**Committed:**
- ST-201: Web Foundation (3 pts)
- ST-202: Auth + Household Selector (3 pts)
- ST-203: Tasks List & Filters (3 pts)

**Total:** 9 points

**Sprint Artifacts:**
- Plan: `docs/planning/pi/2026Q1-PI01/sprints/S02/sprint.md`
- Scope: `docs/planning/pi/2026Q1-PI01/sprints/S02/scope.md`

**Status:** Awaiting Gate B approval

---

### Sprint S03 (Planned)

**Goal:** Complete NOW Increment + Notifications

**Planned:**
- ST-204: Task Detail View (2 pts)
- ST-205: Zones Navigation (2 pts)
- ST-206: Notifications Inbox (2 pts)

**Total:** 6 points (estimated)

---

## Workpack Structure (Per Story)

Each story has a complete workpack folder:

```
docs/planning/workpacks/<STORY_ID>/
├── workpack.md         # Implementation plan, files to change, commits, verification
├── checklist.md        # AC + DoD checklist
├── prompt-plan.md      # Codex PLAN prompt (read-only)
├── prompt-apply.md     # Codex APPLY prompt (execute approved plan)
└── prompt-review.md    # Codex REVIEW prompt (verify implementation)
```

**Example: ST-201**
- Workpack: `docs/planning/workpacks/ST-201/workpack.md`
- Checklist: `docs/planning/workpacks/ST-201/checklist.md`
- PLAN: `docs/planning/workpacks/ST-201/prompt-plan.md`
- APPLY: `docs/planning/workpacks/ST-201/prompt-apply.md`
- REVIEW: `docs/planning/workpacks/ST-201/prompt-review.md`

---

## Execution Pipeline (Per Story)

### Phase 1: PLAN
1. Developer (Codex) receives PLAN prompt
2. Codex reads workpack, story, contracts
3. Codex outputs detailed plan (NO edits, NO commands)
4. **Human Gate C:** Approve plan

### Phase 2: APPLY
1. Developer (Codex) receives APPLY prompt
2. Codex implements according to approved plan
3. Codex runs verification commands
4. Codex reports results (files created, commands run, AC status)

### Phase 3: REVIEW
1. Reviewer (Claude Code or Codex) receives REVIEW prompt
2. Reads implementation + workpack + checklist
3. Verifies AC, DoD, contract compliance, security
4. Outputs GO/NO-GO report
5. **Human Gate D:** Approve merge or request fixes

---

## Gate Sequence

### Gate A (Initiative Scope) ✅ DONE
- Initiative INIT-2026Q1-web-client approved
- NOW/NEXT/LATER boundaries clear
- Epic EP-003 scoped

### Gate B (Sprint Commitment) ⏳ PENDING
- **Decision:** Approve Sprint S02 goal + committed scope (ST-201, ST-202, ST-203)
- **Artifacts:**
  - `docs/planning/pi/2026Q1-PI01/sprints/S02/sprint.md`
  - `docs/planning/pi/2026Q1-PI01/sprints/S02/scope.md`
- **Ask:** Approve 9 points committed + out-of-scope list + risks

### Gate C (Story Plan Approval) — Per Story
- **Decision:** Approve Codex PLAN output before APPLY
- **Artifacts:** Codex PLAN output (Markdown plan)
- **Ask:** Approve implementation approach

### Gate D (Merge Approval) — Per Story
- **Decision:** Approve merge after REVIEW
- **Artifacts:** Codex APPLY report + REVIEW GO/NO-GO report
- **Ask:** Merge to main or request fixes

---

## Risks & Mitigations

| Risk | Impact | Mitigation | Status |
|------|--------|------------|--------|
| CORS issues block integration | HIGH | Test early with real backend | Monitored |
| Contract drift (API ≠ OpenAPI) | HIGH | OpenAPI as source of truth, verify before implementation | Mitigated |
| Scope creep to writes | MEDIUM | Explicit Out of Scope, Gate B enforcement | Mitigated |
| Auth complexity exceeds estimate | MEDIUM | Dev mode only (token paste), defer OIDC | Mitigated |
| Learning curve for web stack | LOW | Accept as expected, functional > perfect | Accepted |

---

## Out of Scope (Explicit Boundaries)

### Deferred to NEXT Increment
- Task creation/editing UI
- Command box (text command input)
- Complete/assign task actions
- Keycloak OIDC integration

### Deferred to LATER Increment
- Analytics dashboard
- Agreements view
- Mobile/PWA
- Calendar integrations

### Never in Web Client Scope
- New backend microservices
- RBAC expansion
- Backend logic changes

---

## Success Metrics (NOW Increment)

From initiative exit criteria:

1. **E2E Flow Works:**
   - User can authenticate (dev token paste)
   - User can see household context (zones, tasks)
   - User can complete: login → select household → see tasks with filters

2. **Notifications:**
   - GET /households/{id}/notifications works
   - POST /notifications/{notificationId}/read works (idempotent)

3. **Security:**
   - No cross-household leaks
   - 403/404 on unauthorized access

4. **Contract Compliance:**
   - OpenAPI remains source of truth
   - Types match schemas exactly

---

## Next Steps

### Immediate (After Gate B Approval)
1. Execute ST-201 (PLAN → approve → APPLY → REVIEW → merge)
2. Execute ST-202 (PLAN → approve → APPLY → REVIEW → merge)
3. Execute ST-203 (PLAN → approve → APPLY → REVIEW → merge)

### After Sprint S02
1. Demo web client foundation
2. Retro Sprint S02
3. Plan Sprint S03 (ST-204, ST-205, ST-206)
4. Execute Sprint S03
5. Complete NOW increment exit review

---

## Artifacts Index

### Planning
- Initiative: `docs/planning/initiatives/INIT-2026Q1-web-client.md`
- Epic: `docs/planning/epics/EP-003/epic.md`
- Stories: `docs/planning/epics/EP-003/stories/ST-*.md`

### Workpacks
- ST-201: `docs/planning/workpacks/ST-201/`
- ST-202: `docs/planning/workpacks/ST-202/`
- ST-203: `docs/planning/workpacks/ST-203/`

### Sprint
- S02 Plan: `docs/planning/pi/2026Q1-PI01/sprints/S02/sprint.md`
- S02 Scope: `docs/planning/pi/2026Q1-PI01/sprints/S02/scope.md`

### Contracts
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- API Coverage: `docs/mvp/api-coverage.md`
- Service Catalog: `docs/architecture/service-catalog.md`
