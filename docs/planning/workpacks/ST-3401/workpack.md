# Workpack: ST-3401 - Minimal Household Dashboard Home

## Sources of Truth
- Scope anchor: `docs/planning/initiatives/INIT-2026Q3‑household‑dashboard.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Epic: `docs/planning/epics/EP-034/epic.md`
- Story: `docs/planning/epics/EP-034/stories/ST-3401-household-dashboard-minimal.md`
- Existing route map: `clients/web/src/routes/index.tsx`
- Dashboard route/styles: `clients/web/src/routes/Dashboard.tsx`, `clients/web/src/routes/Dashboard.css`
- Household navigation: `clients/web/src/components/Layout/Sidebar.tsx`
- Existing data hooks: `clients/web/src/hooks/useTasks.ts`, `clients/web/src/hooks/useShoppingLists.ts`, `clients/web/src/hooks/useRoutines.ts`, `clients/web/src/hooks/useMembers.ts`
- Translations: `clients/web/src/i18n/translations.ts`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

## Status
**DONE - GATE D GO.** Gate A/B, artifact gate, Gate C, APPLY, verification, review gate, and Gate D are complete as of 2026-06-13.

## Outcome
The household root route becomes a useful dashboard/home page with backend-backed summaries and explicit navigation, closing the NOW slice without a new backend endpoint.

## Acceptance Criteria
- [x] AC-1: Household navigation exposes a Home/Dashboard link to `/households/{householdId}`.
- [x] AC-2: Dashboard displays tasks, shopping lists, routines, and members summary cards with links.
- [x] AC-3: Empty households show useful prompts and CTAs through existing flows.
- [x] AC-4: Dashboard fetches only through existing current-household scoped hooks.
- [x] AC-5: Desktop and mobile layouts render without overlapping text or controls.

## Implementation Evidence
- `clients/web/src/components/Layout/Sidebar.tsx` adds a `NavLink end` Home entry for the household root route.
- `clients/web/src/routes/Dashboard.tsx` now uses `useTasks`, `useShoppingLists`, `useRoutines`, and `useMembers` for current-household summaries.
- `clients/web/src/routes/Dashboard.tsx` removes the localStorage shopping list widget from the household home.
- `clients/web/src/routes/Dashboard.css` defines a responsive two-column desktop / one-column mobile dashboard grid.
- `clients/web/src/i18n/translations.ts` adds dashboard/Home labels and missing Russian labels for shopping/routine summary text.
- Browser mock-preview verification used `VITE_AUTH_PROVIDER=dev` with a local mock API:
  - desktop: 4 dashboard cards, 3 quick actions, Home nav present, no console errors, no horizontal overflow at 1280px;
  - mobile 390px: 4 dashboard cards, card width within viewport, mobile nav hidden, no horizontal overflow.

## Non-goals
- New dashboard backend endpoint.
- OpenAPI, schema, migration, service catalog, ADR, or diagram updates.
- Analytics widgets, personalization, notification feed, caching, or charts.
- Changes to detailed tasks/shopping/routines/members workflows.

## Files to change
- `clients/web/src/routes/Dashboard.tsx` - replace localStorage shopping widget with household summary cards using existing hooks.
- `clients/web/src/routes/Dashboard.css` - responsive dashboard card grid, summary lists, empty/error/loading states.
- `clients/web/src/components/Layout/Sidebar.tsx` - add explicit Home/Dashboard navigation link.
- `clients/web/src/i18n/translations.ts` - add dashboard/home navigation labels and summary copy.
- `docs/planning/epics/EP-034/epic.md` - update status/evidence after review.
- `docs/planning/epics/EP-034/stories/ST-3401-household-dashboard-minimal.md` - update status/evidence after review.
- `docs/planning/workpacks/ST-3401/workpack.md` - record implementation evidence.
- `docs/planning/workpacks/ST-3401/checklist.md` - track acceptance/DoD checks.
- `docs/planning/workpacks/ST-3401/plan-findings.md` - record read-only PLAN findings.
- `docs/planning/workpacks/ST-3401/gate-c.md` - delegated Gate C decision.
- `docs/planning/workpacks/ST-3401/prompt-apply.md` - APPLY prompt after Gate C.
- `docs/planning/workpacks/ST-3401/review-gate.md` - read-only review result.
- `docs/planning/workpacks/ST-3401/gate-d.md` - delegated Gate D decision if review passes.

## Implementation plan
1. Confirm the existing dashboard route, hooks, translations, and navigation surfaces.
2. Compute dashboard summaries client-side from `tasks`, `shoppingLists`, `routines`, and `members`.
3. Replace the current localStorage shopping panel with four household summary cards.
4. Add Home/Dashboard to the sidebar with correct active route behavior.
5. Add small translation keys for labels, empty states, and counts.
6. Run web build/lint and browser verification on desktop/mobile.
7. Record review evidence and close Gate D if checks pass.

## Contract impact
No contract change. The selected NOW slice uses existing household-scoped endpoints:
- `/households/{householdId}/tasks`
- `/households/{householdId}/shopping-lists`
- `/households/{householdId}/routines`
- `/households/{householdId}/members`

## Docs updates
- [ ] Story/workpack evidence updated after APPLY.
- [ ] No OpenAPI update expected.
- [ ] No ADR/diagram expected.
- [x] Roadmap/initiative closure note updated after Gate D GO.

## Tests
- [x] `npm run build` in `clients/web` passed. Vite reported the existing large chunk warning.
- [x] `npm run lint` in `clients/web` passed.
- [x] Browser verification for desktop and mobile dashboard layout passed against a local mock API.

## DoD checklist
- [x] Existing household route remains protected by `ProtectedRoute requireHousehold`.
- [x] Dashboard data stays scoped to the selected household ID.
- [x] Empty states and error states are present.
- [x] Navigation link works and active state is clear.
- [x] Text and controls do not overlap on mobile.
- [x] Review gate completed before Gate D.

## Risks
- Multiple client-side fetches can be less efficient than a dedicated dashboard endpoint. Mitigation: keep this as the minimal NOW slice and defer backend aggregation until performance requires it.
- Dashboard can become too dense on mobile. Mitigation: use a simple responsive card grid and short summary rows.
- Existing dashboard localStorage shopping data will no longer be shown. Mitigation: backend shopping lists are the source of truth after shopping manual flow closure.

## Rollback
- Revert `Dashboard.tsx`, `Dashboard.css`, `Sidebar.tsx`, and translation changes.
- Planning artifacts can be reverted or moved back to READY if runtime rollback is needed.

## Prompt Pack
- PLAN findings: `docs/planning/workpacks/ST-3401/plan-findings.md`
- Gate C: `docs/planning/workpacks/ST-3401/gate-c.md`
- APPLY: `docs/planning/workpacks/ST-3401/prompt-apply.md`
- Review gate: `docs/planning/workpacks/ST-3401/review-gate.md`
- Gate D: `docs/planning/workpacks/ST-3401/gate-d.md`
