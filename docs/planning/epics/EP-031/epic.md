# Epic: EP-031 — Category & Source Model + API Foundation

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT‑2026Q3‑shopping‑categories.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Product Goal: `docs/planning/strategy/product-goal.md`
- Existing shopping contract: `docs/contracts/http/commands.openapi.yaml`
- Shopping runs/export contract: `docs/contracts/http/shopping-marketplaces.openapi.yaml`
- Existing shopping implementation: `services/backend/src/main/java/com/hometusk/shopping/**`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**COMPLETED — HUMAN GATE D APPROVED** — ST-3101 backend foundation is implemented, verified, reviewed, and accepted. EP-032/ST-3201 is also complete through Human Gate D and UAT verification; the NOW scope of the shopping categories initiative is closed.

## Initiative Alignment
This epic implements the NOW foundation for `INIT‑2026Q3‑shopping‑categories`:
- optional category and source metadata on shopping items;
- backward-compatible API request/response fields;
- category/source filters for list items;
- category/source propagation into exports and shopping run snapshots.

**Product Goal Pillar:** Fairness & Transparency, with Reliability as a Feature through explicit contracts, household-scoped validation, and migration safety.

---

## Epic Goal
Make shopping items structured enough for users and later UI work to group, filter, and plan shopping by category or source/store without breaking existing flat lists.

---

## Non-Goals

| Item | Reason |
|------|--------|
| Household store presets | Deferred to INIT NEXT scope |
| Price/budget tracking | Deferred to analytics/source presets |
| AI auto-categorisation | Deferred to smart suggestions |
| Multi-store run planning | Deferred to LATER scope |
| Redesign of shopping run semantics | Runs remain snapshots; they only carry extra metadata |

---

## Decomposition Strategy
Dependency-driven:
1. Contract + backend data/API foundation.
2. Web add/edit/group/filter UI.
3. Optional follow-up hardening for analytics/presets after adoption evidence.

---

## Dependencies

| Dependency | Status |
|------------|--------|
| ShoppingItem entity and REST endpoints | EXISTS |
| ShoppingDetail UI and `useShoppingItems` hook | EXISTS |
| Shopping runs/export contracts | EXISTS |
| Flyway migration chain through V025 | EXISTS |
| Category taxonomy decision | DECIDED for NOW: `groceries`, `cleaning`, `personal_care`, `diy`, `electronics`, `other` |

---

## Stories

| ID | Title | Priority | Status |
|----|-------|----------|--------|
| [ST-3101](./stories/ST-3101-shopping-category-source-foundation.md) | Shopping category/source backend + contract foundation | P0 | COMPLETED - HUMAN GATE D APPROVED |
| [ST-3201](../EP-032/stories/ST-3201-shopping-category-source-ui.md) | Shopping UI category/source controls + grouping | P0 | COMPLETED IN EP-032 - HUMAN GATE D + UAT VERIFIED |
| ST-3301 | Source presets and category/source analytics | P2 | DEFERRED |

---

## Exit Criteria

1. Existing shopping items continue to work with `category=null` and `source=null`.
2. API accepts and returns optional category/source fields.
3. API filters shopping list items by category and source without cross-household leakage.
4. Shopping item updates can change category/source without unintentionally changing purchase status.
5. Shopping runs snapshot category/source and exports include the metadata.
6. Contract docs and service catalog are updated.
7. Backend integration tests cover happy path, filters, partial updates, invalid category, and household boundary.

---

## Flags Summary

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | yes | `commands.openapi.yaml`, `shopping-marketplaces.openapi.yaml` |
| data_impact | yes | `shopping_items` and `shopping_run_items` nullable columns |
| adr_needed | no | Field-level extension; no new architecture decision |
| diagrams_needed | no | No new service boundary or flow |
| security_sensitive | yes | Household-scoped item/list/run access must remain enforced |
| traceability_critical | medium | Direct REST plus command-created shopping items; existing DecisionLog flow must remain intact |
