# Sprint S17

## Sources of Truth
- Product goal: `docs/planning/strategy/product-goal.md`
- Scope anchor: `docs/planning/releases/MVP.md`
- Current initiative: `docs/planning/initiatives/INIT-2026Q2-shopping-marketplaces.md`
- Epic: `docs/planning/epics/EP-013/epic.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**COMPLETED** ✅ — Retro 2026-02-06 | 14/14 points (100%)

## Sprint Goal
**Deliver Shopping Marketplaces UI layer: REST endpoints for ShoppingRun + Share/Export buttons + Marketplace link-out buttons.**

### Key Outcomes
1. ShoppingRun CRUD endpoints operational
2. Share/Export UI buttons in ShoppingDetail header
3. Marketplace link-out buttons on shopping items
4. Full E2E flow: create run → shop → close run

---

## Committed Scope (14 points)

| ID | Title | Points | Epic | Status |
|----|-------|--------|------|--------|
| [ST-1302](../../epics/EP-013/stories/ST-1302-shopping-run-endpoints.md) | ShoppingRun REST endpoints | 8 | EP-013 | DONE |
| [ST-1305](../../epics/EP-013/stories/ST-1305-share-export-ui.md) | Share/Export UI buttons | 3 | EP-013 | DONE |
| [ST-1306](../../epics/EP-013/stories/ST-1306-marketplace-buttons-ui.md) | Marketplace link-out buttons | 3 | EP-013 | DONE |

**Total committed:** 14 points
**Total completed:** 14 points (100%)

---

## Stretch Scope

None planned — focused sprint to complete EP-013 core UI.

---

## Out of Scope

| Item | Reason | Deferred To |
|------|--------|-------------|
| ST-1307 ShoppingRun creation UI | Story not ready | S18 |
| ST-1308 ShoppingRun checklist UI | Blocked by ST-1307 | S18 |
| Voice hardening (ST-1206–1208) | Completed in S16 | DONE |

---

## Dependencies

| Dependency | Owner | Status |
|------------|-------|--------|
| ST-1301 ShoppingRun entity (S16) | Backend | DONE |
| ST-1303 Export endpoint (S16) | Backend | DONE |
| ST-1304 Marketplace templates (S16) | Backend | DONE |
| ST-1310 URL encoding utility (S16) | Frontend | DONE |

**All dependencies resolved from S16.**

---

## Commits

| Story | Commit | Description |
|-------|--------|-------------|
| ST-1302 | `0f31dbb` | ShoppingRun REST endpoints + integration tests |
| ST-1305 | `26a367e` | Share/Export UI buttons with Snackbar feedback |
| ST-1306 | `3884470` | Marketplace link-out buttons with safe URL encoding |

---

## Risks (ROAM-lite)

| Risk | Impact | Likelihood | Outcome |
|------|--------|------------|---------|
| API contract mismatch | Medium | Low | **Resolved** — contract-first approach worked |
| Safari link handling | Low | Low | **Resolved** — target="_blank" with rel="noopener" |

---

## Definition of Done (Sprint Level)
- [x] All committed stories meet DoD
- [x] Tests pass (unit + integration)
- [x] No critical lint errors in changed files
- [x] Demo scenarios executable
- [x] Retro conducted

---

## Artifacts
- [Scope Details](./scope.md)
- [Retro](./retro.md)
