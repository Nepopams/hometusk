# Epic: EP-013 — Shopping Marketplaces

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q2-shopping-marketplaces.md`
- Product Goal: `docs/planning/strategy/product-goal.md`
- Existing Shopping ADR: `docs/architecture/decisions/008-stage5-task-shopping-linkage.md`
- Contract: `docs/contracts/http/shopping-marketplaces.openapi.yaml`
- ADR-014: `docs/adr/014-shopping-run-entity-design.md`
- ADR-015: `docs/adr/015-marketplace-linkout-encoding.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**In Progress** — Contract and ADRs approved 2026-02-03, Sprint S16 planned (foundation).

## Initiative Alignment
This epic implements INIT-2026Q2-shopping-marketplaces:
- Share/export shopping list
- Marketplace link-outs (search URLs)
- ShoppingRun (shopping trip snapshot)
- Task-shopping navigation

**Product Goal Pillar:** Fairness & Transparency (making the "action loop" from list to purchase visible and executable)

---

## Epic Goal
Enable HomeTusk users to:
1. **Share/export** shopping lists (text, CSV)
2. **Link out** to marketplace search from each item
3. **Create shopping runs** (snapshot of list for a store trip)
4. **Track progress** within a run (checklist)
5. **Navigate** between tasks and related shopping items

---

## Non-Goals (Out of Scope)

| Item | Reason |
|------|--------|
| Marketplace API integrations | Requires partnerships, OAuth — deferred |
| Cart/checkout within HomeTusk | Out of scope for MVP |
| Price tracking/comparison | Requires product catalog — deferred |
| AI-powered product matching | Requires ML pipeline — deferred |
| Multi-store run planning | Complexity — post-MVP |

---

## Decomposition Strategy
**By ordered steps / dependency-driven**:
1. Backend foundation (entity + migration + repository)
2. Backend endpoints (REST API)
3. Web UI (depends on backend)
4. Integration/navigation (last)

---

## Dependencies

| Dependency | Status |
|------------|--------|
| ShoppingItem entity | EXISTS (ADR-008) |
| ShoppingList entity | EXISTS |
| ShoppingController | EXISTS |
| Task-Shopping linkage | EXISTS (V012 migration) |
| Web Shopping pages | EXISTS (ShoppingLists.tsx, ShoppingDetail.tsx) |

---

## Stories

| ID | Title | Priority | Ready | Sprint | Flags |
|----|-------|----------|-------|--------|-------|
| [ST-1301](./stories/ST-1301-shopping-run-entity.md) | ShoppingRun entity + repository | P0 | READY | S16 | contract_impact=yes, adr_needed=lite |
| [ST-1302](./stories/ST-1302-shopping-run-endpoints.md) | ShoppingRun REST endpoints | P0 | NOT READY | S17 | contract_impact=yes |
| [ST-1303](./stories/ST-1303-export-shopping-list.md) | Export shopping list (text/CSV) | P1 | READY | S16 | contract_impact=yes |
| [ST-1304](./stories/ST-1304-marketplace-linkouts.md) | Marketplace link-out templates + config | P1 | READY | S16 | adr_needed=lite |
| [ST-1305](./stories/ST-1305-share-export-ui.md) | Share/Export UI buttons | P2 | NOT READY | S17 | — |
| [ST-1306](./stories/ST-1306-marketplace-buttons-ui.md) | Marketplace link-out buttons | P2 | NOT READY | S17 | — |
| [ST-1307](./stories/ST-1307-shopping-run-create-ui.md) | ShoppingRun creation UI | P1 | NOT READY | S18 | — |
| [ST-1308](./stories/ST-1308-shopping-run-checklist-ui.md) | ShoppingRun checklist UI | P1 | NOT READY | S18 | — |
| [ST-1309](./stories/ST-1309-task-shopping-navigation.md) | Task-shopping navigation surfaces | P2 | READY | S16 | — |
| [ST-1310](./stories/ST-1310-url-safe-encoding.md) | URL safe encoding guardrails | P0 | NOT READY | S16 (stretch) | security_sensitive=yes |

---

## Exit Criteria

1. User can share shopping list via text/clipboard
2. User can export shopping list to CSV
3. User can click "Open in Ozon/YandexMarket" for each item
4. User can create shopping run (snapshot)
5. User can mark items purchased within run
6. User can close run with summary (purchased/not purchased)
7. User can navigate from task detail to linked shopping items
8. User can navigate from shopping item to linked task
9. All link-out URLs are safely encoded (no XSS)
10. Household boundary enforced on all new endpoints

---

## Flags Summary

| Flag | Stories Affected |
|------|------------------|
| contract_impact | ST-1301, ST-1302, ST-1303 |
| adr_needed | ST-1301 (lite), ST-1304 (lite) |
| security_sensitive | ST-1310 |
| diagrams_needed | none |

---

## Readiness Report

### Ready Stories (Contract/ADRs approved 2026-02-03)
- **ST-1301**: ShoppingRun entity + repository — ADR-014, contract ready
- **ST-1303**: Export shopping list — Contract ready
- **ST-1304**: Marketplace link-out templates — ADR-015 ready
- **ST-1309**: Task-shopping navigation surfaces — No dependencies

### Not Ready Stories (Blockers)

| Story | Blocker | Deferred To |
|-------|---------|-------------|
| ST-1302 | Blocked by ST-1301 (entity must exist first) | S17 |
| ST-1305 | Blocked by ST-1303 (backend must exist first) | S17 |
| ST-1306 | Blocked by ST-1304 (templates must exist first) | S17 |
| ST-1307 | Blocked by ST-1302 (endpoints must exist first) | S18 |
| ST-1308 | Blocked by ST-1307 (creation UI must exist first) | S18 |
| ST-1310 | Blocked by ST-1304 (templates define encoding context) | S16 (stretch) |

### Artifacts Completed

**Contract-Owner** (Done 2026-02-03):
- `docs/contracts/http/shopping-marketplaces.openapi.yaml`
  - ShoppingRun CRUD endpoints
  - Export endpoint with format parameter
  - Marketplace templates endpoint

**ADR-Designer** (Done 2026-02-03):
- `docs/adr/014-shopping-run-entity-design.md` — Entity structure, snapshot strategy, sync behavior
- `docs/adr/015-marketplace-linkout-encoding.md` — URL encoding, XSS prevention, config structure

---

## Sprint Batching (Approved)

**Sprint S16 (Foundation)** — Current:
- ST-1301: ShoppingRun entity + repository (5 pts)
- ST-1303: Export shopping list (3 pts)
- ST-1304: Marketplace templates (5 pts)
- ST-1309: Task-shopping navigation (3 pts)
- ST-1310: URL encoding (3 pts, stretch)
- Also: EP-012 polish (ST-1206, ST-1207, ST-1208)

**Sprint S17 (Endpoints + UI)**:
- ST-1302: REST endpoints (8 pts)
- ST-1305: Share/Export UI (3 pts)
- ST-1306: Marketplace buttons UI (3 pts)

**Sprint S18 (Shopping Run UX)**:
- ST-1307: ShoppingRun creation UI
- ST-1308: ShoppingRun checklist UI
