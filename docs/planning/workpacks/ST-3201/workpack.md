# Workpack: ST-3201 - Shopping Category/Source Controls + Grouping

## Sources of Truth
- Scope anchor: `docs/planning/initiatives/INIT-2026Q3-shopping-categories.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Epic: `docs/planning/epics/EP-032/epic.md`
- Story: `docs/planning/epics/EP-032/stories/ST-3201-shopping-category-source-ui.md`
- Backend blocker: `docs/planning/workpacks/ST-3101/workpack.md`
- Backend contract: `docs/contracts/http/commands.openapi.yaml`
- Frontend route: `clients/web/src/routes/ShoppingDetail.tsx`
- Frontend shopping hook: `clients/web/src/hooks/useShoppingItems.ts`
- Frontend API client/types: `clients/web/src/lib/api.ts`, `clients/web/src/types/api.ts`
- Frontend translations: `clients/web/src/i18n/translations.ts`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

## Status
**COMPLETED - HUMAN GATE D APPROVED**. Backend category/source implementation, ST-3101 Human Gate D, ST-3201 PLAN findings, Human Gate C, frontend implementation, review GO, Human Gate D, and automated frontend verification are recorded. Live browser verification is accepted as deferred debt TD-ST-3201-001 because the local backend health check timed out.

## Outcome
Shopping list users can add, edit, view, group, and filter optional category/source metadata in the web UI while simple uncategorised lists remain fast and uncluttered.

## Acceptance Criteria
- [x] AC-1: Add item with category/source sends those fields and displays badges.
- [x] AC-2: Add item with name only remains valid and renders without empty badges.
- [x] AC-3: Edit category/source sends a metadata-only PATCH and preserves purchase status.
- [x] AC-4: Group by category renders metadata groups plus an Uncategorised bucket.
- [x] AC-5: Group by source renders source groups plus a No source bucket.
- [x] AC-6: Grouping/filtering preserves unpurchased and purchased sections.
- [x] AC-7: Add/edit optimistic updates roll back on API failure.
- [ ] AC-8: Desktop/mobile layout has no overlapping controls or text overflow. CSS implementation is complete; live browser verification is deferred as TD-ST-3201-001.

## Non-goals
- Backend API, migration, or contract implementation.
- Household source presets.
- Price/budget summaries.
- AI category suggestions.
- Multi-store run planning.
- Editing upstream AI Platform contracts.

## Files changed during APPLY
- `clients/web/src/types/api.ts` - category/source types, filters, add/update request shapes.
- `clients/web/src/lib/api.ts` - category/source query params and partial PATCH payload.
- `clients/web/src/lib/shoppingMetadata.ts` - category taxonomy, payload normalization, grouping helpers.
- `clients/web/src/hooks/useShoppingItems.ts` - add/edit optimistic metadata handling and rollback.
- `clients/web/src/routes/ShoppingDetail.tsx` - controls, badges, edit flow, grouping/filtering.
- `clients/web/src/routes/ShoppingDetail.css` - responsive layout and badge/control styling.
- `clients/web/src/i18n/translations.ts` - shopping metadata and grouping labels.
- `clients/web/src/lib/shoppingMetadata.test.ts` - focused helper tests for payload and grouping semantics.

## Readiness Evidence Collected
- `ShoppingDetail.tsx` currently submits only `{ name }` from `handleAddItem`.
- `ShoppingDetail.tsx` renders flat `unpurchasedItems` and `purchasedItems`.
- `renderItem(item)` currently displays name, quantity/unit, linked task, marketplace links, purchase toggle, and delete button.
- `useShoppingItems.ts` currently supports optimistic add, toggle purchased, and delete, but no metadata edit operation.
- `api.ts` currently sends only the `purchased` list filter and PATCHes only `{ purchased }`.
- `types/api.ts` currently has no category/source fields on shopping items, filters, or add request.
- `translations.ts` has core shopping keys, but no category/source/grouping labels.
- Frontend `package.json` exposes `build`, `lint`, and `test` scripts using TypeScript/Vite/Vitest.
- ST-3101 review gate is GO and recorded at `docs/planning/workpacks/ST-3101/review-gate.md`.
- ST-3101 Human Gate D is approved and recorded at `docs/planning/workpacks/ST-3101/gate-d.md`.
- Frontend baseline on 2026-06-13: `npm run build`, `npm run lint`, and `npm run test` all pass.
- ST-3201 Gate C packet is recorded at `docs/planning/workpacks/ST-3201/gate-c-plan.md`.
- Human Gate C approved ST-3201 on 2026-06-13.
- Contract alignment audit confirms ST-3201 can consume ST-3101 without backend or OpenAPI edits.
- Metadata-only PATCH must omit `purchased`; explicit `category: null` clears category; blank or null `source` clears source.
- APPLY implementation evidence on 2026-06-13:
  - Frontend types/API/hook consume category/source and preserve purchase-only toggle payloads.
  - Shopping detail UI supports optional add details, metadata edit modal, badges, grouping, filtering, and clear filters.
  - Grouping stays inside the existing unpurchased and purchased sections.
  - Start-trip count/disable behavior remains list-level when category/source filters are active.
  - `clients/web/src/lib/shoppingMetadata.test.ts` covers category/source payloads, metadata-only PATCH shape, null clearing, grouping buckets, and purchased separation.
- Automated verification after APPLY on 2026-06-13:
  - `cd clients/web && npm run build` passed.
  - `cd clients/web && npm run lint` passed.
  - `cd clients/web && npm run test` passed: 2 files, 27 tests.
- Local live-stack verification on 2026-06-13:
  - `hometusk-postgres` and `hometusk-keycloak` were healthy.
  - Backend health check `http://localhost:8080/actuator/health` timed out.
  - Per human direction, local stack bring-up is tracked as TD-ST-3201-001 and live browser verification is deferred.
- Review/Gate evidence on 2026-06-13:
  - Review gate result: GO, recorded at `docs/planning/workpacks/ST-3201/review-gate.md`.
  - Human Gate D approved ST-3201, recorded at `docs/planning/workpacks/ST-3201/gate-d.md`.

## Implementation Completed

### Frontend contract alignment
1. Add frontend category/source type definitions matching ST-3101.
2. Extend item filters and add/update request shapes.
3. Update `getShoppingItems`, `addShoppingItem`, and `updateShoppingItem` to support category/source without changing existing purchased toggles.

### UI controls and metadata edit
1. Add optional category/source details to the add item flow.
2. Add an item metadata edit affordance that PATCHes category/source only.
3. Add badges for existing metadata and keep legacy rows clean.
4. Reuse the existing error/rollback pattern from `useShoppingItems`.

### Grouping/filtering and polish
1. Add no grouping/category/source controls.
2. Group within unpurchased and purchased sections.
3. Add i18n labels and responsive CSS for badges, controls, and long source values.
4. Add tests for grouping helpers and purchased separation.
5. Preserve list-level start-trip behavior when category/source filters are active.

Verification:
- `cd clients/web && npm run build`
- `cd clients/web && npm run lint`
- `cd clients/web && npm run test`
- Browser screenshots/checks for desktop and mobile are deferred to TD-ST-3201-001.

## Contract impact
- No new contract edits expected.
- ST-3201 consumes the ST-3101 contract.
- If implementation discovers contract drift, stop and return to artifact gate before editing code.

## Docs updates
- [ ] No ADR expected.
- [ ] No diagram expected.
- [x] Update this workpack with ST-3101 evidence before PLAN.
- [x] Record ST-3201 PLAN and Gate C packet.
- [x] Add implementation evidence after APPLY.
- [x] Record review gate and Human Gate D.

## Verification commands after APPLY
- `cd clients/web && npm run build`
- `cd clients/web && npm run lint`
- `cd clients/web && npm run test`
- Browser verification of the shopping detail page at desktop and mobile widths is deferred to TD-ST-3201-001.

## DoD checklist
- [x] ST-3101 completed and reviewed.
- [x] Human Gate C approved for ST-3201.
- [x] Review gate completed with GO.
- [x] Human Gate D approved for ST-3201.
- [x] Frontend contract alignment matches backend DTOs.
- [x] Metadata edit cannot change purchase status accidentally.
- [x] User-generated source text is rendered as text only.
- [x] Grouping/filtering preserves existing purchase sections.
- [ ] Desktop/mobile UI verified live (deferred to TD-ST-3201-001).
- [x] Build, lint, and tests pass.

## Tech Debt / Deferred Verification
- TD-ST-3201-001: Provide a reliable one-command local stack for live browser verification, including Postgres, Keycloak, backend, web env, dev-token login, and seeded household/shopping-list data. This is a verification-enablement debt item, not a product behavior dependency for the implemented frontend slice.

## Risks
- UI may make quick add feel slower - keep metadata optional and collapsible/secondary.
- Partial PATCH drift could alter purchased state - preserve existing toggle path and add regression coverage.
- Source is free text - render as plain text and clamp/wrap long values.
- Grouping could obscure purchased items - group inside each existing section.
- Frontend may race optimistic edits and toggles - use per-item operation keys and rollback from current hook pattern.

## Rollback
- Revert frontend commits for ST-3201.
- No database rollback required.
- Backend ST-3101 can remain deployed because UI fields are optional and backward-compatible.

## Prompt Pack
- PLAN: `docs/planning/workpacks/ST-3201/prompt-plan.md`
- Gate C packet: `docs/planning/workpacks/ST-3201/gate-c-plan.md`
- APPLY: `docs/planning/workpacks/ST-3201/prompt-apply.md`
- REVIEW: separate read-only Codex review gate after APPLY.
