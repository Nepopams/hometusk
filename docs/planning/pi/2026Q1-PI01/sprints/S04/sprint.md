# Sprint S04 — Household Lifecycle (Create/Join/Invites)

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md`
- Epic: `docs/planning/epics/EP-005/epic.md`
- PI Charter: `docs/planning/pi/2026Q1-PI01/pi.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- ADR-010: `docs/architecture/decisions/010-household-invites.md`

---

## Goal

Enable complete household lifecycle: users can create households, invite members, accept invites, and view their team.

**Success Metric:** User can create household → invite another user → they accept → both see each other in members list.

---

## Prioritization Rationale

This sprint implements the **NOW** increment of INIT-2026Q1-household-lifecycle:
1. **Team formation is blocking product use:** Single-user households limit product value
2. **Foundation exists:** S03 delivered auth, session management
3. **Backend ready:** All endpoints exist (create, invites, members)

**Why now:**
- Roadmap marks household-lifecycle as current NOW focus
- Enables multi-user product validation
- No backend changes needed

---

## Scope

### Committed (DoR-ready, P1)

| Story | Title | Points | Workpack |
|-------|-------|--------|----------|
| ST-401 | Household Selector & Empty State | 2 | `docs/planning/workpacks/ST-401/` |
| ST-402 | Create Household Form | 2 | `docs/planning/workpacks/ST-402/` |
| ST-403 | Create Invite & Share | 2 | `docs/planning/workpacks/ST-403/` |
| ST-404 | Accept Invite Flow | 3 | `docs/planning/workpacks/ST-404/` |
| ST-405 | Members List View | 2 | `docs/planning/workpacks/ST-405/` |

**Total committed:** 11 points

**Deliverables:**
- Household selector with empty state handling
- Create household functionality
- Invite creation and sharing (link/token)
- Accept invite flow (with auth integration)
- Members list (read-only)

---

### Out of Scope (explicit)

- **No temporary households** — defer to NEXT
- **No invite revoke** — defer to NEXT
- **No leave household** — defer to NEXT
- **No role management** — defer to LATER
- **No backend changes** — consume existing API
- **No email/SMS invite delivery** — in-app/link only

---

## Capacity Note

**Assumptions:**
- 1 developer (Codex)
- 2-3 days per story
- Total: ~10-15 days for committed scope

**Constraints:**
- Depends on S03 completion (auth working)
- Sequential dependencies: ST-401 → ST-402 → ST-403/ST-404/ST-405

---

## Assumptions

1. S03 (Registration/Sign-in) is complete
2. Backend API endpoints are stable
3. ADR-010 invite semantics are correct
4. Keycloak realm properly configured

---

## Dependencies

| Dependency | Owner | Status | Risk |
|------------|-------|--------|------|
| S03 (Auth) | Web | Done | LOW |
| Backend API | Backend | Ready | LOW |
| ADR-010 (Invites) | Arch | Accepted | LOW |

**Critical path:** ST-401 → ST-402 → (ST-403 || ST-404) → ST-405

---

## Risks & Mitigations

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Auth integration issues | HIGH | LOW | S03 already handles this |
| Invite flow confusing | MEDIUM | MEDIUM | Clear error messages, copy button |
| Scope creep to NEXT | MEDIUM | MEDIUM | Strict Out of Scope enforcement |
| Empty state not discoverable | LOW | LOW | Prominent CTA design |

---

## Definition of Ready Check

**DoR Status:** READY

All prerequisites:
- [x] S03 complete (auth working)
- [x] Backend endpoints exist (verified in OpenAPI)
- [x] ADR-010 accepted (invite semantics)
- [x] Workpacks created for all stories

---

## Gate B Ask

**Request:** Approve Sprint S04 goal, committed scope (ST-401 through ST-405), and capacity note.

**What we commit to:**
1. Deliver household selector with empty state
2. Deliver create household functionality
3. Deliver invite creation and sharing
4. Deliver accept invite flow
5. Deliver members list view

**What we won't do:**
- Temporary households
- Invite revoke
- Leave household
- Backend changes

**Approval needed:**
- [ ] Sprint goal approved
- [ ] Committed scope (11 points) approved
- [ ] Out of scope explicit list approved
- [ ] Risks accepted

---

## Sprint Artifacts

| Artifact | Path |
|----------|------|
| Sprint Plan | `docs/planning/pi/2026Q1-PI01/sprints/S04/sprint.md` (this file) |
| Scope Detail | `docs/planning/pi/2026Q1-PI01/sprints/S04/scope.md` |
| Demo Plan | `docs/planning/pi/2026Q1-PI01/sprints/S04/demo.md` (TBD) |
| Retro | `docs/planning/pi/2026Q1-PI01/sprints/S04/retro.md` (TBD) |

---

## Exit Criteria

Sprint is **done** when:
1. All committed stories completed (AC verified, DoD met)
2. `npm run build/lint` pass
3. Manual E2E test passes: create household → invite → accept → see members
4. No cross-household data leaks
5. Demo prepared
