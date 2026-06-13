# Epic: EP-032 - Shopping UI: Category & Source

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q3-shopping-categories.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Backend foundation epic: `docs/planning/epics/EP-031/epic.md`
- Backend foundation story: `docs/planning/epics/EP-031/stories/ST-3101-shopping-category-source-foundation.md`
- Frontend route: `clients/web/src/routes/ShoppingDetail.tsx`
- Frontend shopping hook: `clients/web/src/hooks/useShoppingItems.ts`
- Frontend API client/types: `clients/web/src/lib/api.ts`, `clients/web/src/types/api.ts`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**COMPLETED - HUMAN GATE D APPROVED - UAT VERIFIED**. The UI slice is implemented, ST-3101 Gate D is approved, ST-3201 Human Gate C is approved, review gate is GO, Human Gate D is approved, automated frontend verification is recorded, and UAT verification was accepted on 2026-06-13.

## Initiative Alignment
This epic implements the NOW UI outcome for `INIT-2026Q3-shopping-categories`:
- add/edit controls for optional item category and source;
- compact badges for category/source metadata;
- list grouping/filtering by category or source while preserving purchased/unpurchased sections;
- clean backward-compatible display for existing uncategorised items.

## Epic Goal
Make shopping lists easier to scan and plan by surfacing category/source metadata in the web UI without slowing down the default "add item" flow.

## Non-Goals

| Item | Reason |
|------|--------|
| Backend category/source implementation | Covered by ST-3101 |
| Household source presets | Deferred to EP-033 |
| Price/budget summaries | Deferred to EP-033 |
| AI category suggestions | Deferred to NEXT/LATER scope |
| Multi-store shopping runs | Deferred to EP-034 |

## Current Frontend Evidence

| Area | Current state | Impact for ST-3201 |
|------|---------------|--------------------|
| Add item form | `ShoppingDetail.tsx` submits only `{ name }` from `newItemName`. | Add optional details controls without making the fast path heavier. |
| Item rendering | `renderItem(item)` shows name, quantity/unit, task link, marketplace links, delete, and purchase checkbox. | Add metadata badges and edit affordance inside the existing item row. |
| Item grouping | Route derives flat `unpurchasedItems` and `purchasedItems` arrays. | Introduce grouping inside those two sections, not across purchase state. |
| API client | `getShoppingItems` only sends `purchased`; `updateShoppingItem` only PATCHes `{ purchased }`. | Extend filters and PATCH payload shape after ST-3101 lands. |
| Types | `ShoppingItem`, filters, and add request have no category/source fields. | Align frontend types to the ST-3101 contract. |
| Tests | Existing frontend shopping coverage is limited to `marketplaceUrl.test.ts`. | Add focused UI/helper tests where possible. |

## Decomposition Strategy
Dependency-driven:
1. Wait for ST-3101 implementation evidence and contract verification.
2. Update web types/API/hook to consume category/source safely.
3. Add low-friction UI controls and metadata display.
4. Add grouping/filtering and responsive/i18n polish.

## Dependencies

| Dependency | Status |
|------------|--------|
| ST-3101 backend implementation | COMPLETED - Human Gate D approved |
| `ShoppingItemCategory` taxonomy | DECIDED in ST-3101 |
| Partial PATCH category/source semantics | CONFIRMED in ST-3101 |
| Frontend shopping route/hook | EXISTS |
| Frontend test runner | EXISTS (`vitest`) |

## Stories

| ID | Title | Priority | Status |
|----|-------|----------|--------|
| [ST-3201](./stories/ST-3201-shopping-category-source-ui.md) | Shopping category/source controls + grouping | P0 | COMPLETED - HUMAN GATE D APPROVED |

## Exit Criteria

1. Users can add a shopping item with optional category/source.
2. Users can edit category/source on an existing item without changing purchase status.
3. Category/source badges render only when metadata exists.
4. Users can group or filter list items by category/source without losing purchased/unpurchased separation.
5. Legacy items with null metadata remain clean and usable.
6. Mobile layout CSS is implemented for no overlapping controls or text overflow; category/source shopping flow is verified on UAT.
7. Frontend types, API client, hook, route, i18n, CSS, and tests match the implemented backend contract.

## Implementation Evidence
- `clients/web/src/types/api.ts`, `clients/web/src/lib/api.ts`, `clients/web/src/hooks/useShoppingItems.ts`, `clients/web/src/routes/ShoppingDetail.tsx`, `clients/web/src/routes/ShoppingDetail.css`, `clients/web/src/i18n/translations.ts`, and `clients/web/src/lib/shoppingMetadata.ts` were updated for ST-3201.
- `clients/web/src/lib/shoppingMetadata.test.ts` covers payload normalization, metadata-only PATCH shape, grouping buckets, and purchased separation.
- `cd clients/web && npm run build` passed on 2026-06-13.
- `cd clients/web && npm run lint` passed on 2026-06-13.
- `cd clients/web && npm run test` passed on 2026-06-13: 2 files, 27 tests.
- Review gate is GO and recorded at `docs/planning/workpacks/ST-3201/review-gate.md`.
- Human Gate D is approved and recorded at `docs/planning/workpacks/ST-3201/gate-d.md`.
- UAT verification accepted by human on 2026-06-13 for initiative closure.

## Tech Debt / Deferred Verification
- None blocking EP-032 closure.
- TD-ST-3201-001 is resolved for EP-032 verification by UAT acceptance on 2026-06-13. A reliable one-command local stack remains optional local-dev hygiene outside this epic's closure.

## Flags Summary

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no new contract | Consumes ST-3101 contract only |
| data_impact | no | No new persistence beyond ST-3101 |
| adr_needed | no | UI extension within existing shopping route |
| diagrams_needed | no | No new service boundary |
| security_sensitive | medium | Source is user text; render as text, do not inject HTML |
| traceability_critical | low | Direct shopping REST UI only |
