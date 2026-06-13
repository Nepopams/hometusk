# Review Gate - ST-3201 Shopping Category/Source Controls + Grouping

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-3201/workpack.md`
- Checklist: `docs/planning/workpacks/ST-3201/checklist.md`
- Story: `docs/planning/epics/EP-032/stories/ST-3201-shopping-category-source-ui.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q3-shopping-categories.md`
- Backend contract: `docs/contracts/http/commands.openapi.yaml`

## Review Result
GO for Human Gate D.

## Date
2026-06-13

## Must-fix
None.

## Should-fix / Accepted Debt
- TD-ST-3201-001: live desktop/mobile browser verification is deferred because the local backend health check timed out while Postgres and Keycloak were healthy.
- Optional hardening follow-up: narrow the internal metadata edit type so `updateMetadata` cannot accept `purchased` at compile time. The current caller already sends metadata-only payloads through `buildShoppingItemMetadataUpdate`.

## Evidence Reviewed
- Frontend types include shopping category/source fields, filters, add request, update request, and shopping run snapshot metadata.
- API client sends category/source filters and PATCHes a structured partial update payload.
- Purchased toggle sends `{ purchased }` only.
- Metadata edit sends category/source through `buildShoppingItemMetadataUpdate` and does not include `purchased`.
- Grouping happens inside existing unpurchased and purchased sections.
- User-provided source text is rendered as React text, not injected HTML.
- Start-trip count/disable behavior remains list-level when category/source filters are active.
- Planning artifacts record live browser verification as deferred debt rather than a completed check.

## Commands
- `cd clients/web && npm run build` - passed.
- `cd clients/web && npm run lint` - passed.
- `cd clients/web && npm run test` - passed, 2 files, 27 tests.
- `git diff --check -- clients/web/src docs/planning/workpacks/ST-3201 docs/planning/epics/EP-032 docs/planning/initiatives/INIT-2026Q3-shopping-categories.md docs/planning/strategy/roadmap.md` - passed with CRLF warnings only.

## Recommendation
Proceed to Human Gate D with TD-ST-3201-001 explicitly accepted as deferred verification-enablement debt.
