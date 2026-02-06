# Sprint S16

## Sources of Truth
- Product goal: `docs/planning/strategy/product-goal.md`
- Scope anchor: `docs/planning/releases/MVP.md`
- Current initiative: `docs/planning/initiatives/INIT-2026Q2-shopping-marketplaces.md`
- Epic (primary): `docs/planning/epics/EP-013/epic.md`
- Epic (polish): `docs/planning/epics/EP-012/epic.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**COMPLETED** ✅ — Retro 2026-02-06 | 27/24 points (112%)

## Sprint Goal
**Establish Shopping Marketplaces backend foundation (entity + export + marketplace config) and complete Voice Input EP-012 polish.**

### Key Outcomes
1. ShoppingRun entity and repository ready for endpoint implementation (S17)
2. Export endpoint functional (text/CSV)
3. Marketplace templates served from config
4. Task-shopping navigation visible in UI
5. Voice Input error handling, telemetry, and cross-browser support complete

---

## Committed Scope (24 points)

| ID | Title | Points | Epic | Status |
|----|-------|--------|------|--------|
| [ST-1301](../../epics/EP-013/stories/ST-1301-shopping-run-entity.md) | ShoppingRun entity + repository | 5 | EP-013 | Ready |
| [ST-1303](../../epics/EP-013/stories/ST-1303-export-shopping-list.md) | Export shopping list (text/CSV) | 3 | EP-013 | Ready |
| [ST-1304](../../epics/EP-013/stories/ST-1304-marketplace-linkouts.md) | Marketplace link-out templates + config | 5 | EP-013 | Ready |
| [ST-1309](../../epics/EP-013/stories/ST-1309-task-shopping-navigation.md) | Task-shopping navigation surfaces | 3 | EP-013 | Ready |
| [ST-1206](../../epics/EP-012/stories/ST-1206-error-handling-ux.md) | Error handling UX | 3 | EP-012 | Ready |
| [ST-1207](../../epics/EP-012/stories/ST-1207-client-telemetry.md) | Client telemetry events | 2 | EP-012 | Ready |
| [ST-1208](../../epics/EP-012/stories/ST-1208-cross-browser-accessibility.md) | Cross-browser + accessibility | 3 | EP-012 | Ready |

**Total committed:** 24 points

---

## Stretch Scope (3 points)

| ID | Title | Points | Notes |
|----|-------|--------|-------|
| [ST-1310](../../epics/EP-013/stories/ST-1310-url-safe-encoding.md) | URL safe encoding guardrails | 3 | If ST-1304 completes early |

---

## Out of Scope

| Item | Reason | Deferred To |
|------|--------|-------------|
| ST-1302 ShoppingRun REST endpoints | Blocked by ST-1301 (8 pts) | S17 |
| ST-1305 Share/Export UI buttons | Blocked by ST-1303 | S17 |
| ST-1306 Marketplace link-out buttons | Blocked by ST-1304 | S17 |
| ST-1307 ShoppingRun creation UI | Blocked by ST-1302 | S18 |
| ST-1308 ShoppingRun checklist UI | Blocked by ST-1307 | S18 |
| Mobile browser support | Out of EP-012 scope | LATER |
| Marketplace API integrations | Requires partnerships | OUT |

---

## Dependencies

| Dependency | Owner | Status |
|------------|-------|--------|
| EP-012 Voice Input Core (S15) | Delivered | DONE |
| ADR-014 ShoppingRun design | Architecture | DONE |
| ADR-015 Link-out encoding | Architecture | DONE |
| Shopping Marketplaces OpenAPI | Contract | DONE |
| Existing ShoppingList/Item entities | Backend | DONE |

**No blocking dependencies for S16.**

---

## Risks (ROAM-lite)

| Risk | Impact | Likelihood | Strategy | Owner |
|------|--------|------------|----------|-------|
| Migration V025 conflicts with parallel work | Medium | Low | **Resolve** - coordinate migration sequence | Dev |
| CSV escaping edge cases (Unicode, quotes) | Low | Medium | **Mitigate** - use RFC 4180 compliant library | Dev |
| Cross-browser MediaRecorder differences | Medium | Medium | **Mitigate** - Chrome primary, graceful degradation | Dev |
| ST-1310 does not fit in sprint | Low | Medium | **Accept** - marked as stretch | PO |

---

## Capacity Notes
- Sprint duration: 2 weeks
- Focus split: ~60% EP-013 (foundation), ~40% EP-012 (polish)
- Buffer: stretch story (3 pts) for unexpected blockers

---

## Definition of Done (Sprint Level)
- [ ] All committed stories meet DoD
- [ ] Tests pass (unit + integration)
- [ ] No critical lint errors in changed files
- [ ] Demo scenarios documented and executable
- [ ] Retro conducted

---

## Artifacts
- [Scope Details](./scope.md)
- [Demo Plan](./demo.md)
- [Retro Template](./retro.md)
