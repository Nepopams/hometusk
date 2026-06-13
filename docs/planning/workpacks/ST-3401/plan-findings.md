# PLAN Findings: ST-3401 - Minimal Household Dashboard Home

## Result
GO for APPLY.

## Findings
- `/households/:householdId` already routes to `clients/web/src/routes/Dashboard.tsx`; no router change is needed.
- The current dashboard still mixes backend tasks with localStorage shopping data, so it does not satisfy the initiative exit criteria.
- Existing hooks are sufficient for the minimal NOW slice:
  - `useTasks(activeHouseholdId, {})`
  - `useShoppingLists(activeHouseholdId)`
  - `useRoutines(activeHouseholdId)`
  - `useMembers(activeHouseholdId)`
- `Sidebar.tsx` currently links directly to tasks/routines/shopping/etc. and lacks an explicit household home link.
- A `NavLink` to `/households/{householdId}` must use `end` so it is not active for every nested route.
- No backend endpoint, OpenAPI update, ADR, diagram, service catalog update, or migration is required for this slice.

## Files to Change
- `clients/web/src/routes/Dashboard.tsx`
- `clients/web/src/routes/Dashboard.css`
- `clients/web/src/components/Layout/Sidebar.tsx`
- `clients/web/src/i18n/translations.ts`
- `docs/planning/epics/EP-034/epic.md`
- `docs/planning/epics/EP-034/stories/ST-3401-household-dashboard-minimal.md`
- `docs/planning/workpacks/ST-3401/workpack.md`
- `docs/planning/workpacks/ST-3401/checklist.md`
- `docs/planning/workpacks/ST-3401/review-gate.md`
- `docs/planning/workpacks/ST-3401/gate-d.md`

## Implementation Plan
1. Replace `Dashboard.tsx` localStorage shopping widget with summary sections based on existing hooks.
2. Compute task overdue/due-today/upcoming/open/done counts client-side.
3. Compute shopping list count and unpurchased total from backend shopping list summaries.
4. Compute routine active/paused totals and member admin/member totals.
5. Render four responsive cards with loading/error/empty states and links to existing detailed pages.
6. Add a Home/Dashboard `NavLink` with `end` to `Sidebar.tsx`.
7. Add English and Russian translation keys; other partial locales fall back to English through `mergeTranslations`.
8. Run `npm run build`, `npm run lint`, and browser desktop/mobile checks.

## Risks
- Client-side aggregation performs several requests. Acceptable for NOW; dedicated endpoint remains deferred.
- Partial endpoint failures should not blank the whole dashboard unless authorization fails.
- Mobile layout must avoid dense text and nested card clutter.

## Stop-the-line Conditions
- Existing hooks cannot load household-scoped data without new contracts.
- Dashboard needs cross-household data or service-boundary changes.
- Build or lint requires broad unrelated refactors.

## Gate C Recommendation
GO. The plan is source-backed, small, and limited to the approved workpack files.
