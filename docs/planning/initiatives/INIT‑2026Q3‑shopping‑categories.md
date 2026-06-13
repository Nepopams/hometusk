Initiative: INIT-2026Q3-shopping-categories - Categorised & Sourced Shopping Lists

## Status
Closed - NOW scope fully accepted and verified on UAT on 2026-06-13. EP-031/ST-3101 and EP-032/ST-3201 are complete through Human Gate D; the previous live-browser deferred verification item TD-ST-3201-001 is resolved for initiative closure by UAT evidence.

## Sources of Truth
- Roadmap: `docs/planning/strategy/roadmap.md`
- Backend epic: `docs/planning/epics/EP-031/epic.md`
- Backend story/workpack: `docs/planning/epics/EP-031/stories/ST-3101-shopping-category-source-foundation.md`, `docs/planning/workpacks/ST-3101/workpack.md`
- UI epic: `docs/planning/epics/EP-032/epic.md`
- UI story/workpack: `docs/planning/epics/EP-032/stories/ST-3201-shopping-category-source-ui.md`, `docs/planning/workpacks/ST-3201/workpack.md`
- Gate C packet: `docs/planning/workpacks/ST-3201/gate-c-plan.md`
- API contract: `docs/contracts/http/commands.openapi.yaml`

## Problem / Opportunity
Shopping lists in HomeTusk currently collect items, but without category and source metadata large lists are hard to scan, store planning is manual, and later analytics or recommendations have no stable structure to build on.

## Outcome
Users can add optional category and source metadata to shopping items, see compact badges, and group/filter lists by category or source while legacy uncategorised lists remain fast and uncluttered.

## Scope (Now / Next / Later)

### NOW - Category and Source fields
- Add optional `category` and `source` fields to shopping items and shopping run snapshots.
- Keep legacy items valid with null metadata.
- Expose category/source through API, OpenAPI, DTOs, CSV/text exports, and web UI.
- Provide a small category taxonomy: `groceries`, `cleaning`, `personal_care`, `diy`, `electronics`, `other`.
- Keep `source` as household-local free text, trimmed and rendered as text only.

### NEXT - Source presets and analytics
- Household-level store/source presets.
- Per-category and per-source summaries.
- Optional cost/budget extensions.
- AI-assisted category suggestions after the deterministic data model and UI are stable.

### LATER - Multi-store runs and delegation
- Store-aware shopping runs.
- Delegation by source/category.
- Multi-store route or trip planning.

## In Scope
- Backend persistence, DTOs, validation, filters, migration, and export propagation.
- Frontend controls for add/edit category/source.
- Badges and grouping/filtering on the shopping detail page.
- Contract and service-catalog updates for implemented behavior.
- Focused tests and browser verification.

## Out of Scope
- AI category suggestions in the first NOW slice.
- Store presets and analytics in the first NOW slice.
- Price scraping or budgeting.
- Multi-household source sharing.
- Changing shopping run semantics beyond carrying metadata snapshots.

## Success Metrics
- At least 60% of newly created shopping items have a category assigned within three weeks of release.
- At least 30% of shopping list sessions use grouping/filtering.
- No more than 5% of shopping API calls fail due to invalid category/source validation.
- Users report improved clarity and faster trip planning compared with the baseline list.

## Constraints / Guardrails
- Category/source are optional and backward-compatible.
- Business rules stay in code; UI is not the source of truth.
- Source is user-generated text and must be rendered safely.
- Filtering/grouping must preserve unpurchased and purchased sections.
- Backend contract changes require artifact gate approval; ST-3201 is expected to consume ST-3101 without new contract edits.

## Epic Candidates

| ID | Title | Scope | Status |
|----|-------|-------|--------|
| EP-031 | Category & Source Model + Migration | NOW | Completed - Human Gate D approved (ST-3101) |
| EP-032 | Shopping UI: Category & Source | NOW | Completed - Human Gate D approved (ST-3201) |
| EP-033 | Household Store Presets & Analytics | NEXT | Proposed |
| EP-034 | Multi-Store Runs & Delegation | LATER | Proposed |

## Exit Criteria (NOW)
- New category and source columns exist in `shopping_items` and shopping run snapshot tables and default to null.
- API accepts, returns, validates, and filters category/source fields.
- OpenAPI and service catalog reflect implemented behavior.
- Web UI lets users add/edit category/source and displays badges only when metadata exists.
- Lists can be grouped and filtered by category/source while preserving unpurchased/purchased sections.
- Desktop and mobile CSS is implemented for no overlapping controls or text overflow; category/source shopping flow is verified on UAT.
- Backend and frontend verification evidence is recorded.

## Current Gate
No open gate and no open closure debt for the NOW scope. NEXT/LATER candidates remain proposed and are not part of this closure.

## Implementation Evidence
- ST-3101 backend/model/contract/export work is complete through Human Gate D.
- ST-3201 frontend APPLY is complete: types, API client, shopping hook, shopping detail UI, CSS, i18n, and helper tests were updated.
- Frontend checks passed on 2026-06-13: `npm run build`, `npm run lint`, and `npm run test` in `clients/web`.
- Vitest result after ST-3201 APPLY: 2 files, 27 tests.
- ST-3201 review gate is GO and recorded at `docs/planning/workpacks/ST-3201/review-gate.md`.
- ST-3201 Human Gate D is approved and recorded at `docs/planning/workpacks/ST-3201/gate-d.md`.
- UAT verification accepted by human on 2026-06-13: category/source shopping flow is considered closed and verified on UAT.

## Tech Debt / Deferred Verification
- None blocking initiative closure.
- TD-ST-3201-001 is resolved for initiative verification by UAT acceptance on 2026-06-13. A reliable one-command local stack and seed path may still be useful as general local-dev hygiene, but it is no longer a deferred verification item for this initiative.
