# Codex PLAN Prompt — ST-3201 Shopping Category/Source Controls + Grouping

## Mode
PLAN only. Read-only.

Do not edit, create, delete, move, format, or generate tracked files during PLAN.
Do not implement frontend code.
Do not generate `prompt-apply.md`.

## Objective
Produce a decision-complete implementation plan for ST-3201:

Shopping list users can add, edit, view, group, and filter optional category/source metadata in the web UI while simple uncategorised lists remain fast and uncluttered.

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT‑2026Q3‑shopping‑categories.md`
- Epic: `docs/planning/epics/EP-032/epic.md`
- Story: `docs/planning/epics/EP-032/stories/ST-3201-shopping-category-source-ui.md`
- Workpack: `docs/planning/workpacks/ST-3201/workpack.md`
- Checklist: `docs/planning/workpacks/ST-3201/checklist.md`
- Backend foundation: `docs/planning/workpacks/ST-3101/workpack.md`
- ST-3101 Gate D: `docs/planning/workpacks/ST-3101/gate-d.md`
- Backend contract: `docs/contracts/http/commands.openapi.yaml`
- Frontend route: `clients/web/src/routes/ShoppingDetail.tsx`
- Frontend CSS: `clients/web/src/routes/ShoppingDetail.css`
- Frontend hook: `clients/web/src/hooks/useShoppingItems.ts`
- Frontend API client: `clients/web/src/lib/api.ts`
- Frontend types: `clients/web/src/types/api.ts`
- Frontend translations: `clients/web/src/i18n/translations.ts`
- Frontend package scripts: `clients/web/package.json`

## Acceptance Criteria
- AC-1: Add item with category/source sends those fields and displays badges.
- AC-2: Add item with name only remains valid and renders without empty badges.
- AC-3: Edit category/source sends a metadata-only PATCH and preserves purchase status.
- AC-4: Group by category renders metadata groups plus an Uncategorised bucket.
- AC-5: Group by source renders source groups plus a No source bucket.
- AC-6: Grouping/filtering preserves unpurchased and purchased sections.
- AC-7: Add/edit optimistic updates roll back on API failure.
- AC-8: Desktop/mobile layout has no overlapping controls or text overflow.

## In Scope
- Frontend type alignment for category/source and partial PATCH.
- API client support for category/source filters and metadata updates.
- Hook support for add/edit optimistic metadata behavior and rollback.
- Shopping detail route controls, badges, grouping/filtering, and edit flow.
- Responsive CSS and i18n labels.
- Focused tests or explicit manual verification plan if component harness is insufficient.

## Out of Scope
- Backend API, migration, or contract edits.
- New category taxonomy beyond ST-3101 values.
- Household source presets.
- Price/budget summaries.
- AI category suggestions.
- Multi-store shopping run filtering.

## Required PLAN Output
Record findings in the response only, with:
- current-state findings with exact files/functions;
- implementation steps grouped by commit-sized chunks;
- final file list to change;
- tests to add/update;
- verification commands;
- browser verification plan for desktop and mobile;
- risks and rollback;
- STOP-THE-LINE conditions;
- Gate C recommendation.

## Invariants
- Preserve quick name-only add flow.
- Metadata-only PATCH must not include or alter `purchased`.
- Existing purchase toggle must keep sending purchase-only updates.
- Grouping must happen inside existing unpurchased and purchased sections.
- Render source text as React text only; never inject HTML.
- Keep controls keyboard reachable and labelled.
- Avoid overlapping text/controls on mobile.

## Verification Candidates
- `cd clients/web && npm run build`
- `cd clients/web && npm run lint`
- `cd clients/web && npm run test`
- Browser verification of shopping detail page at desktop and mobile widths after APPLY.

## STOP-THE-LINE
- Backend contract does not match the implemented ST-3101 DTOs.
- Existing frontend add/toggle/delete behavior cannot be preserved without broader refactor.
- UI requires a new backend endpoint or contract change.
- Test harness cannot cover critical payload/grouping behavior and no manual verification path is available.
- Responsive layout cannot be made safe within the approved files.
