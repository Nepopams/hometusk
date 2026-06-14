# ST-3401 Checklist - Minimal Household Dashboard Home

## Gate C / PLAN
- [x] Existing dashboard route and navigation are confirmed.
- [x] Existing household-scoped hooks are confirmed sufficient.
- [x] No new API contract is required.
- [x] Allowed files are listed in the workpack.
- [x] Gate C decision is recorded.

## APPLY
- [x] Sidebar exposes Home/Dashboard link.
- [x] Dashboard shows tasks summary card.
- [x] Dashboard shows shopping lists summary card.
- [x] Dashboard shows routines summary card.
- [x] Dashboard shows members summary card.
- [x] Empty states and CTAs use existing flows.
- [x] Error/loading states are handled without full-page failure when only one section fails.
- [x] Responsive layout verified.

## Verification
- [x] `npm run build` passes in `clients/web`.
- [x] `npm run lint` passes in `clients/web`.
- [x] Browser desktop check passes.
- [x] Browser mobile check passes.

## Review / Gate D
- [x] Review gate has no must-fix findings.
- [x] Workpack evidence is updated.
- [x] Story/epic status is updated.
- [x] Roadmap/initiative status is updated if the NOW slice is closed.
- [x] Gate D decision is recorded.
