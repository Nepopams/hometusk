# Sprint S02 — Web Client Foundation (NOW Increment)

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q1-web-client.md`
- Epic: `docs/planning/epics/EP-003/epic.md`
- PI Charter: `docs/planning/pi/2026Q1-PI01/pi.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- API Coverage: `docs/mvp/api-coverage.md`
- MVP Exit Review: `docs/planning/releases/reviews/MVP-exit-review.md`

---

## Goal

Deliver a working web client foundation that allows users to authenticate, select a household, and view tasks with filters.

**Success Metric:** User can complete E2E flow: login → select household → see filtered tasks list.

---

## Prioritization Rationale

This sprint implements the **NOW** increment of INIT-2026Q1-web-client initiative:
1. **Foundation first:** Without web shell, no further work possible
2. **Auth next:** Required for all protected endpoints
3. **Tasks list:** Primary value delivery (read-first per initiative)

**Why before NEXT/LATER:**
- Read-first aligns with NOW increment scope
- No backend changes needed (API is stable post-MVP)
- Desktop-first focus per initiative

---

## Scope

### Committed (DoR-ready, P1)

| Story | Title | Points | Workpack |
|-------|-------|--------|----------|
| ST-201 | Web Foundation (Project Setup & Build) | 3 | `docs/planning/workpacks/ST-201/` |
| ST-202 | Auth Integration + Household Selector | 3 | `docs/planning/workpacks/ST-202/` |
| ST-203 | Tasks List & Filters | 3 | `docs/planning/workpacks/ST-203/` |

**Total committed:** 9 points

**Deliverables:**
- `clients/web/` — React + TypeScript + Vite project
- Authentication (dev mode: token paste)
- Household selector
- Tasks list with status/assignee/zone filters

---

### Stretch (if capacity allows)

- ST-204: Task Detail View (placeholder → full implementation)
- ST-205: Zones Navigation (read-only list)
- ST-206: Notifications Inbox (basic implementation)

---

### Out of Scope (explicit)

- **No write operations** — defer to Sprint S03/NEXT increment
- **No command box** — defer to Sprint S03/NEXT increment
- **No Keycloak OIDC** — dev mode (token paste) sufficient for NOW
- **No analytics dashboard** — defer to LATER increment
- **No mobile-first design** — desktop-first per initiative
- **No new backend code** — consume existing API only

---

## Capacity Note

**Assumptions:**
- 1 developer (Codex)
- 3-5 days per story
- Total: ~9-15 days for committed scope

**Constraints:**
- First web sprint — learning curve expected
- No parallel backend work
- Integration testing with real backend required

---

## Assumptions

1. Backend API is stable (MVP closed, exit review passed)
2. OpenAPI contract is accurate and up-to-date
3. Keycloak local instance available for auth testing
4. No breaking changes to API during sprint
5. CORS configured correctly on backend

---

## Dependencies

| Dependency | Owner | Status | Risk |
|------------|-------|--------|------|
| Backend API (MVP) | Backend | Ready (MVP closed) | LOW |
| OpenAPI contract | Backend | Ready | LOW |
| Keycloak (local) | DevOps | Ready | LOW |
| CORS config | Backend | Assumed ready | MEDIUM |

**Critical path:** ST-201 → ST-202 → ST-203 (sequential)

---

## Risks & Mitigations

| Risk | Impact | Probability | Mitigation | Trigger | Owner |
|------|--------|-------------|------------|---------|-------|
| CORS issues block integration | HIGH | MEDIUM | Test early with real backend, coordinate with backend team | First API call fails | Developer |
| Contract drift (API ≠ OpenAPI) | HIGH | LOW | Verify endpoints match OpenAPI before implementation | Type mismatch errors | Developer |
| Auth complexity exceeds estimate | MEDIUM | LOW | Implement dev mode only (token paste), defer Keycloak OIDC | ST-202 exceeds 3 days | Developer |
| Scope creep to writes | MEDIUM | MEDIUM | Strict Out of Scope enforcement, Gate B approval required for changes | Feature requests during sprint | Product Owner |
| Learning curve for web stack | LOW | HIGH | Accept as expected, focus on functional over perfect | — | Developer |

---

## Definition of Ready Check

**DoR Status:** PASS ✅

All committed stories meet DoR:
- [x] ST-201: Title clear, AC testable, deliverables defined, no blockers
- [x] ST-202: Title clear, AC testable, dependencies identified (ST-201), no blockers
- [x] ST-203: Title clear, AC testable, dependencies identified (ST-202), no blockers

All stories have:
- [x] Workpacks created with implementation plan
- [x] Prompt packs (PLAN/APPLY/REVIEW) ready
- [x] Checklists defined

---

## Gate B Ask

**Request:** Approve Sprint S02 goal, committed scope (ST-201, ST-202, ST-203), and capacity note.

**What we commit to:**
1. Deliver web client foundation (React + TypeScript + Vite)
2. Deliver auth integration (dev mode: token paste)
3. Deliver tasks list with filters (status/assignee/zone)

**What we won't do:**
- Write operations (create/edit/complete tasks)
- Keycloak OIDC integration
- Backend changes

**Approval needed:**
- [ ] Sprint goal approved
- [ ] Committed scope (9 points) approved
- [ ] Out of scope explicit list approved
- [ ] Risks accepted

---

## Daily Standup Structure

**Questions:**
1. What did you complete yesterday?
2. What will you work on today?
3. Any blockers?

**Focus areas:**
- CORS/backend integration issues
- Contract alignment (API vs OpenAPI)
- Scope creep signals

---

## Sprint Artifacts

| Artifact | Path |
|----------|------|
| Sprint Plan | `docs/planning/pi/2026Q1-PI01/sprints/S02/sprint.md` (this file) |
| Scope Detail | `docs/planning/pi/2026Q1-PI01/sprints/S02/scope.md` |
| Demo Plan | `docs/planning/pi/2026Q1-PI01/sprints/S02/demo.md` (TBD) |
| Retro | `docs/planning/pi/2026Q1-PI01/sprints/S02/retro.md` (TBD) |

---

## Exit Criteria

Sprint is **done** when:
1. All committed stories completed (AC verified, DoD met)
2. `npm run build/lint` pass
3. Manual E2E test passes: login → select household → see tasks with filters
4. Demo prepared (screenshots or video)
5. Retro completed

---

## Next Steps (After Gate B Approval)

1. Execute ST-201 (Codex PLAN → approve → APPLY → REVIEW)
2. Execute ST-202 (Codex PLAN → approve → APPLY → REVIEW)
3. Execute ST-203 (Codex PLAN → approve → APPLY → REVIEW)
4. Prepare demo
5. Run retro
6. Plan Sprint S03 (ST-204, ST-205, ST-206)
