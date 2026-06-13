# Gate C Plan Packet: ST-3201 - Shopping Category/Source Controls + Grouping

## Status
**HUMAN GATE C APPROVED**. Codex APPLY may proceed within the approved scope.

## Gate C Recommendation
GO for Codex APPLY.

## Scope Approved For APPLY
- Frontend type alignment for shopping `category` and `source`.
- API client support for category/source list filters and partial item PATCH payloads.
- Shopping items hook support for optimistic add/edit metadata behavior and rollback.
- Shopping detail route controls, metadata badges, edit flow, grouping/filtering, and error feedback.
- Responsive CSS for dense shopping rows, long names, long sources, badges, and controls.
- i18n labels for shopping metadata controls and buckets.
- Focused tests for grouping, payload semantics, purchased separation, and optimistic rollback where feasible with the current Vitest node harness.

## Out Of Scope
- Backend API, DTO, migration, repository, or service changes.
- OpenAPI contract edits.
- AI category suggestions.
- Household source presets.
- Price/budget summaries.
- Multi-store shopping run planning.
- Upstream AI Platform contract edits.

## PLAN Findings
- `clients/web/src/types/api.ts` has no shopping category/source frontend types yet.
- `clients/web/src/lib/api.ts` sends only the `purchased` filter and PATCHes only `{ purchased }`.
- `clients/web/src/hooks/useShoppingItems.ts` already has optimistic add/toggle/delete patterns, but no metadata edit operation.
- `clients/web/src/hooks/useShoppingItems.ts` currently depends only on `filters.purchased`; ST-3201 must include `filters.category` and `filters.source`.
- `clients/web/src/routes/ShoppingDetail.tsx` submits `{ name }` only and renders flat unpurchased/purchased sections.
- `clients/web/src/routes/ShoppingDetail.css` uses compact flex rows; badge and control additions must be checked on narrow mobile widths.
- `clients/web/src/i18n/translations.ts` has base shopping keys, but no metadata/grouping labels.
- Existing frontend test harness is Vitest in `node`; pure helper/API payload tests fit the current harness better than full component DOM tests unless a new test dependency is explicitly approved.

## Backend Contract Alignment
- `GET /households/{householdId}/shopping-lists/{listId}/items` supports optional `purchased`, `category`, and `source` query filters.
- `POST /households/{householdId}/shopping-lists/{listId}/items` accepts optional `category` and `source`.
- `PATCH /households/{householdId}/shopping-items/{itemId}` supports partial updates for `purchased`, `category`, and `source`.
- Metadata-only PATCH can omit `purchased` and preserve purchase state.
- Explicit `category: null` clears category; omitted `category` means no category update.
- Blank `source` is normalized server-side to `null`; omitted `source` means no source update.
- Supported categories are `groceries`, `cleaning`, `personal_care`, `diy`, `electronics`, and `other`.

## Verification Evidence Before Gate C
- `cd clients/web && npm run build` passes.
- `cd clients/web && npm run lint` passes.
- `cd clients/web && npm run test` passes: 1 test file, 19 tests.
- `git diff --check` passes with line-ending warnings only.
- Backend ST-3101 review gate is GO and Human Gate D is recorded.

## Required Verification After APPLY
- `cd clients/web && npm run build`
- `cd clients/web && npm run lint`
- `cd clients/web && npm run test`
- Browser verification of shopping detail on desktop viewport.
- Browser verification of shopping detail on mobile viewport.
- Manual checks for name-only add, add with metadata, metadata edit, clearing metadata, grouping by category/source, long source text, and rollback/error state.

## STOP-THE-LINE Conditions
- APPLY requires backend or contract changes.
- Metadata-only PATCH cannot preserve purchase status.
- Existing quick add/toggle/delete behavior regresses.
- Source text requires unsafe rendering or HTML injection.
- Responsive shopping rows cannot be made safe within the approved frontend files.
- Critical payload/grouping behavior cannot be verified by tests or manual browser evidence.

## Human Gate C Decision
- Approved.
- User approval phrase: `Утверждаю Gate C для ST-3201`.
- Approval date: 2026-06-13.
