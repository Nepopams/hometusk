# Initiative Execution Index: INIT-2026Q1-household-lifecycle

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md`
- Epic: `docs/planning/epics/EP-005/epic.md`
- Sprint: `docs/planning/pi/2026Q1-PI01/sprints/S04/sprint.md`

---

## Status
**Planned** (pending Human Gate approval)

---

## Epic

| Epic ID | Title | Status |
|---------|-------|--------|
| EP-005 | Household Lifecycle (Create/Join/Invites) | Draft |

---

## Stories

| Story ID | Title | Status | Sprint | Workpack |
|----------|-------|--------|--------|----------|
| ST-401 | Household Selector & Empty State | Planned | S04 | [workpack](../../workpacks/ST-401/workpack.md) |
| ST-402 | Create Household Form | Planned | S04 | [workpack](../../workpacks/ST-402/workpack.md) |
| ST-403 | Create Invite & Share | Planned | S04 | [workpack](../../workpacks/ST-403/workpack.md) |
| ST-404 | Accept Invite Flow | Planned | S04 | [workpack](../../workpacks/ST-404/workpack.md) |
| ST-405 | Members List View | Planned | S04 | [workpack](../../workpacks/ST-405/workpack.md) |

---

## Sprint Assignment

### Sprint S04 (Committed)
- ST-401: Household Selector & Empty State (2 pts)
- ST-402: Create Household Form (2 pts)
- ST-403: Create Invite & Share (2 pts)
- ST-404: Accept Invite Flow (3 pts)
- ST-405: Members List View (2 pts)

**Total:** 11 points

---

## Key Artifacts

| Artifact | Path |
|----------|------|
| Initiative | `docs/planning/initiatives/INIT-2026Q1-household-lifecycle.md` |
| Epic | `docs/planning/epics/EP-005/epic.md` |
| Sprint Plan | `docs/planning/pi/2026Q1-PI01/sprints/S04/sprint.md` |
| Sprint Scope | `docs/planning/pi/2026Q1-PI01/sprints/S04/scope.md` |
| ADR-010 (Invites) | `docs/architecture/decisions/010-household-invites.md` |

---

## Exit Criteria Tracking

From initiative NOW scope:

| Criterion | Story | Status |
|-----------|-------|--------|
| User can create household in web | ST-402 | Planned |
| User can see/switch households | ST-401 | Planned |
| User can invite another user | ST-403 | Planned |
| Invited user can accept and join | ST-404 | Planned |
| Members list visible | ST-405 | Planned |
| No cross-household leaks | All | Planned |
| Docs/contracts up to date | ST-405+ | Planned |

---

## Dependencies

| Dependency | Status |
|------------|--------|
| S03 (Registration/Sign-in) | Done |
| Backend API (households/invites) | Ready |
| ADR-010 (Invite semantics) | Accepted |

---

## Next Steps

1. **Human Gate:** Approve Sprint S04 scope
2. **Generate prompts:** After approval, generate prompt-plan.md for each story
3. **Execute:** Run PLAN → APPLY → REVIEW cycle per story
4. **Validate:** E2E testing of full household lifecycle

---

## Files Created

```
docs/planning/epics/EP-005/epic.md
docs/planning/epics/EP-005/stories/ST-401-household-selector.md
docs/planning/epics/EP-005/stories/ST-402-create-household.md
docs/planning/epics/EP-005/stories/ST-403-create-invite.md
docs/planning/epics/EP-005/stories/ST-404-accept-invite.md
docs/planning/epics/EP-005/stories/ST-405-members-list.md
docs/planning/workpacks/ST-401/workpack.md
docs/planning/workpacks/ST-401/checklist.md
docs/planning/workpacks/ST-402/workpack.md
docs/planning/workpacks/ST-402/checklist.md
docs/planning/workpacks/ST-403/workpack.md
docs/planning/workpacks/ST-403/checklist.md
docs/planning/workpacks/ST-404/workpack.md
docs/planning/workpacks/ST-404/checklist.md
docs/planning/workpacks/ST-405/workpack.md
docs/planning/workpacks/ST-405/checklist.md
docs/planning/pi/2026Q1-PI01/sprints/S04/sprint.md
docs/planning/pi/2026Q1-PI01/sprints/S04/scope.md
docs/planning/initiatives/INIT-2026Q1-household-lifecycle.execution.md
```
