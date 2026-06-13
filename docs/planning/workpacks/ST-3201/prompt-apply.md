# Codex APPLY Prompt - ST-3201 Shopping Category/Source Controls + Grouping

## Mode
APPLY. Human Gate C is approved.

Implement only the approved ST-3201 frontend scope. Keep diffs small and stop if implementation requires backend, contract, ADR, diagram, or upstream AI Platform changes.

## Objective
Shopping list users can add, edit, view, group, and filter optional category/source metadata in the web UI while simple uncategorised lists remain fast and uncluttered.

## Approved Files
- `clients/web/src/types/api.ts`
- `clients/web/src/lib/api.ts`
- `clients/web/src/lib/shoppingMetadata.ts`
- `clients/web/src/hooks/useShoppingItems.ts`
- `clients/web/src/routes/ShoppingDetail.tsx`
- `clients/web/src/routes/ShoppingDetail.css`
- `clients/web/src/i18n/translations.ts`
- `clients/web/src/**/*.test.*`
- `docs/planning/workpacks/ST-3201/workpack.md`
- `docs/planning/workpacks/ST-3201/checklist.md`
- `docs/planning/workpacks/ST-3201/gate-c-plan.md`
- `docs/planning/workpacks/ST-3201/prompt-apply.md`
- `docs/planning/epics/EP-032/epic.md`
- `docs/planning/epics/EP-032/stories/ST-3201-shopping-category-source-ui.md`
- `docs/planning/initiatives/INIT‑2026Q3‑shopping‑categories.md`

## Forbidden Files
- Backend Java/Kotlin/resources/test files.
- `docs/contracts/**`
- `docs/integration/ai-platform/v1/upstream/**`
- ADRs and diagrams.
- Unrelated runbooks or planning files outside ST-3201 status/evidence updates.

## Acceptance Criteria
- AC-1: Add item with category/source sends those fields and displays badges.
- AC-2: Add item with name only remains valid and renders without empty badges.
- AC-3: Edit category/source sends a metadata-only PATCH and preserves purchase status.
- AC-4: Group by category renders metadata groups plus an Uncategorised bucket.
- AC-5: Group by source renders source groups plus a No source bucket.
- AC-6: Grouping/filtering preserves unpurchased and purchased sections.
- AC-7: Add/edit optimistic updates roll back on API failure.
- AC-8: Desktop/mobile layout has no overlapping controls or text overflow.

## Invariants
- Preserve the quick name-only add flow.
- Metadata-only PATCH must not include or alter `purchased`.
- Existing purchased toggle must keep sending purchase-only updates.
- Grouping must happen inside existing unpurchased and purchased sections.
- Render source text as React text only; never inject HTML.
- Keep controls keyboard reachable and labelled.
- Avoid overlapping text/controls on mobile.

## Implementation Notes
- Use ST-3101 category values: `groceries`, `cleaning`, `personal_care`, `diy`, `electronics`, `other`.
- Treat omitted metadata fields as no update.
- Send explicit `category: null` when clearing category.
- Normalize blank source to `null` before metadata PATCH.
- Include `filters.category` and `filters.source` in the shopping items fetch dependency surface.
- Prefer pure helper tests under the current Vitest node harness unless a stronger existing harness is available.

## Verification
- `cd clients/web && npm run build`
- `cd clients/web && npm run lint`
- `cd clients/web && npm run test`
- Browser verification on desktop and mobile viewports when the local stack is available; if not, record the verification gap and tech debt.
- Manual checks for name-only add, add with metadata, metadata edit, clearing metadata, grouping by category/source, long source text, and error rollback.

## STOP-THE-LINE
- Backend or OpenAPI changes are needed.
- Metadata-only PATCH cannot preserve purchase state.
- Existing quick add/toggle/delete behavior regresses.
- Source text requires unsafe rendering.
- Responsive layout cannot be made safe within approved files.
