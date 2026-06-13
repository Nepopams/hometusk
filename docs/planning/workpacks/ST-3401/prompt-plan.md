# Codex PLAN Prompt: ST-3401 - Minimal Household Dashboard Home

## Mode
Read-only PLAN. Do not edit, create, delete, move, format, or generate tracked files.

## Objective
Produce a decision-complete implementation plan for ST-3401, the minimal household dashboard home.

## Sources to Read
- `docs/planning/workpacks/ST-3401/workpack.md`
- `docs/planning/epics/EP-034/epic.md`
- `docs/planning/epics/EP-034/stories/ST-3401-household-dashboard-minimal.md`
- `docs/planning/initiatives/INIT-2026Q3‑household‑dashboard.md`
- `clients/web/src/routes/Dashboard.tsx`
- `clients/web/src/routes/Dashboard.css`
- `clients/web/src/components/Layout/Sidebar.tsx`
- `clients/web/src/routes/index.tsx`
- `clients/web/src/hooks/useTasks.ts`
- `clients/web/src/hooks/useShoppingLists.ts`
- `clients/web/src/hooks/useRoutines.ts`
- `clients/web/src/hooks/useMembers.ts`
- `clients/web/src/i18n/translations.ts`
- `clients/web/package.json`

## Constraints
- Do not add a new backend endpoint.
- Do not edit OpenAPI, schemas, ADRs, diagrams, or upstream snapshots.
- Preserve `ProtectedRoute requireHousehold`.
- Use only the current household ID for data fetching.
- Keep detailed workflow pages unchanged.

## Required Output
- Files to change.
- Exact implementation steps.
- Risks and stop-the-line conditions.
- Verification commands.
- Gate C recommendation.
