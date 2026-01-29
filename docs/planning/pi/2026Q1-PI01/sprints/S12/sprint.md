# Sprint S12 — Routines UI + Lifecycle

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Scope Anchor: `docs/planning/initiatives/INIT-2026Q3-recurring-tasks-scheduling.md`
- Epic: `docs/planning/epics/EP-010/epic.md`
- PI Charter: `docs/planning/pi/2026Q1-PI01/pi.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/routines.openapi.yaml`
- ADR-013: `docs/adr/013-routine-scheduler-design.md`

---

## Goal

Complete the Routines feature by implementing the web UI for routine management and the pause/resume lifecycle with upcoming instances preview.

**Success Metric:** Users can create, edit, pause/resume routines through the web interface and see upcoming task instances for each routine.

**Product Goal Alignment:** Reduces cognitive load by providing visible, controllable automation of recurring tasks (Pillar: Fairness & Transparency).

---

## Prioritization Rationale

This sprint completes EP-010 (Recurring Tasks & Scheduling v0):

1. **UI enables user value:** Backend is ready (S10 CRUD, S11 scheduler), users need interface
2. **Lifecycle control:** Pause/resume is essential for vacations, schedule changes
3. **Visibility:** Upcoming instances preview helps users understand what will be generated

**Why S12 (after S10, S11):**
- S10 delivered foundation (entity + CRUD + security)
- S11 delivered engine (scheduler + assignment policies + task indicator)
- S12 completes the feature with UI + lifecycle control

---

## Scope

### Committed (DoR-ready, P1)

| Story | Title | Points | Dependencies | Flags |
|-------|-------|--------|--------------|-------|
| ST-1005 | Routines Page (List + Create/Edit Form) | 5 | ST-1001 (CRUD) | - |
| ST-1006 | Pause/Resume + Upcoming Instances View | 3 | ST-1003 (Scheduler), ST-1005 | contract_impact |

**Total committed:** 8 points

**Deliverables:**
- Routines list page (`/households/{id}/routines`)
- Create routine form with frequency presets (Daily, Weekly, Monthly, Every N days)
- Edit routine form with pre-populated fields
- Delete routine with confirmation
- Sidebar navigation link "Routines"
- Pause/resume API endpoints: `POST /routines/{id}/pause`, `POST /routines/{id}/resume`
- Upcoming instances endpoint: `GET /routines/{id}/upcoming?days=7`
- Pause/resume buttons in routine list
- Upcoming instances section in routine detail view
- Status badges (ACTIVE/PAUSED)

---

### Out of Scope (explicit)

| Item | Reason | Deferred To |
|------|--------|-------------|
| Calendar view of upcoming | UX complexity | LATER |
| Vacation mode (bulk pause all) | Scope creep | LATER |
| Custom RRULE input (advanced) | RRULE-lite only for v0 | LATER |
| Drag-and-drop ordering | Nice-to-have | LATER |
| Bulk operations | Nice-to-have | LATER |
| Delete pending instances on pause | Keep simple | LATER |
| Push notifications for pause reminders | Out of epic scope | LATER |

---

## Capacity Note

**Assumptions:**
- 1 developer (Codex)
- 2-3 days per story (based on S11 velocity)
- Total: ~6-9 days for committed scope

**Constraints:**
- ST-1005 must complete before ST-1006 (UI base needed)
- ST-1006 API endpoints can run parallel with ST-1005 UI work

**Buffer:** 20% (~2 points) for:
- Form validation edge cases
- CSS/styling adjustments
- Review feedback rework

---

## Assumptions

1. S11 scheduler stories (ST-1003, ST-1004, ST-1007) are complete and merged
2. Routines CRUD endpoints work correctly (verified in S10)
3. Web client patterns (hooks, routes, components) are stable
4. React/TypeScript patterns established in existing pages apply

---

## Dependencies

| Dependency | Owner | Status | Risk |
|------------|-------|--------|------|
| Routine CRUD (ST-1001) | Backend | Done (S10) | LOW |
| Scheduler (ST-1003) | Backend | Done (S11) | LOW |
| Assignment policies (ST-1004) | Backend | Done (S11) | LOW |
| Task routine indicator (ST-1007) | Backend | Done (S11) | LOW |
| Web client infrastructure | Frontend | Exists | LOW |

**Critical path:** ST-1005 (UI base) -> ST-1006 (lifecycle UI)

---

## Risks & Mitigations (ROAM-lite)

| Risk | Impact | Probability | Strategy | Notes |
|------|--------|-------------|----------|-------|
| Complex form validation (recurrence rules) | MEDIUM | MEDIUM | Mitigate | Use presets only, no custom RRULE |
| CSS styling inconsistency | LOW | MEDIUM | Mitigate | Follow existing page patterns |
| Upcoming instances calculation complexity | MEDIUM | LOW | Resolve | Already have RecurrenceRuleParser |
| Round-robin preview accuracy | LOW | MEDIUM | Accept | Preview is "projected", may change |
| Scope creep (advanced features) | MEDIUM | MEDIUM | Own | Strict out-of-scope list |

---

## Definition of Ready Check

**DoR Status:** READY (all 2 stories pass)

All prerequisites:
- [x] Epic EP-010 approved
- [x] S10, S11 stories completed and merged
- [x] Backend CRUD endpoints available
- [x] Scheduler service working
- [x] All stories have AC with Given/When/Then
- [x] Test strategies defined for each story
- [x] Web client patterns established

---

## Gate B Ask

**Request:** Approve Sprint S12 goal, committed scope (ST-1005, ST-1006), and capacity note.

**What we commit to:**
1. Deliver Routines list page with create/edit/delete
2. Deliver frequency presets (Daily, Weekly, Monthly, Every N days)
3. Deliver assignment policy selector (Fixed, Round-robin, Manual)
4. Deliver pause/resume API endpoints
5. Deliver upcoming instances API endpoint
6. Deliver pause/resume buttons in UI
7. Deliver upcoming instances view in routine detail
8. Add Routines link to sidebar navigation

**What we won't do:**
- Calendar view
- Vacation mode
- Custom RRULE input
- Bulk operations
- Push notifications

**Approval needed:**
- [ ] Sprint goal approved
- [ ] Committed scope (8 points) approved
- [ ] Out of scope explicit list approved
- [ ] Risks accepted

---

## Sprint Artifacts

| Artifact | Path |
|----------|------|
| Sprint Plan | `docs/planning/pi/2026Q1-PI01/sprints/S12/sprint.md` (this file) |
| Scope Detail | `docs/planning/pi/2026Q1-PI01/sprints/S12/scope.md` |
| Demo Plan | `docs/planning/pi/2026Q1-PI01/sprints/S12/demo.md` |
| Retro | `docs/planning/pi/2026Q1-PI01/sprints/S12/retro.md` |

---

## Exit Criteria

Sprint is **done** when:
1. All committed stories completed (AC verified, DoD met)
2. `./gradlew build` passes (backend)
3. `npm run build` passes (web)
4. All unit and integration tests pass
5. OpenAPI contract matches implementation (new endpoints)
6. Routines page accessible and functional
7. Pause/resume works end-to-end
8. Upcoming instances display correctly
9. Demo prepared

**Sprint fails if:**
- Cannot create/edit/delete routines via UI
- Pause/resume buttons don't work
- Upcoming instances not displayed
- Navigation link missing
