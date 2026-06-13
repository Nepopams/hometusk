# Epic: EP-034 - Household Dashboard Home & Navigation

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q3‑household‑dashboard.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Product goal: `docs/planning/strategy/product-goal.md`
- Existing household route: `clients/web/src/routes/index.tsx`
- Existing dashboard route: `clients/web/src/routes/Dashboard.tsx`
- Existing dashboard styles: `clients/web/src/routes/Dashboard.css`
- Existing household navigation: `clients/web/src/components/Layout/Sidebar.tsx`
- Existing household data hooks: `clients/web/src/hooks/useTasks.ts`, `clients/web/src/hooks/useShoppingLists.ts`, `clients/web/src/hooks/useRoutines.ts`, `clients/web/src/hooks/useMembers.ts`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

## Status
**DONE.** Gate A/B, Gate C, APPLY, review gate, and Gate D are complete as of 2026-06-13 for the minimal NOW dashboard slice.

## Initiative Alignment
This epic implements the NOW outcome for `INIT-2026Q3‑household‑dashboard`: make `/households/{householdId}` a visible household home with summary cards, empty states, and explicit navigation into tasks, shopping, routines, and members.

## Epic Goal
Give household members a single operational home page that summarizes the current household without introducing a new backend contract in the first slice.

## Decomposition Strategy
The minimal slice uses existing household-scoped endpoints client-side:
1. Replace the current dashboard's local-only shopping widget with backend-backed household summaries.
2. Surface tasks, shopping lists, routines, and members in one responsive page.
3. Add an explicit Home/Dashboard entry to household navigation.
4. Keep dedicated dashboard API aggregation, personalization, analytics widgets, and notification feed out of scope.

## Stories

| ID | Title | Priority | Status |
|----|-------|----------|--------|
| [ST-3401](./stories/ST-3401-household-dashboard-minimal.md) | Minimal household dashboard home | P0 | DONE |

## Exit Criteria
1. `/households/{householdId}` is reachable via household navigation.
2. Dashboard displays summary cards for tasks, shopping lists, routines, and members.
3. Summary cards use existing household-scoped APIs and link to detailed pages.
4. Empty, loading, and error states are present for a new or partially loaded household.
5. The page is responsive on desktop and mobile.
6. No new OpenAPI contract is required for the NOW slice.
7. Workpack evidence, checks, and review gate are recorded before closing.

## Flags Summary

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | Uses existing household-scoped endpoints from the web client. |
| data_impact | no | No schema or persistence change. |
| adr_needed | no | No new architecture decision for client-side aggregation. |
| diagrams_needed | no | No service or command-flow change. |
| security_sensitive | medium | Page must not fetch outside the selected household. |
| traceability_critical | low | No command pipeline behavior changes. |

## Gate Notes
- Gate A/B: GO by delegated user objective on 2026-06-13.
- Artifact gate: GO without contract/ADR/diagram updates because the slice avoids a new dashboard endpoint.
- Gate C: GO, recorded in `docs/planning/workpacks/ST-3401/gate-c.md`.
- Gate D: GO, recorded in `docs/planning/workpacks/ST-3401/gate-d.md`.
