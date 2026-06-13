# Story: ST-3401 - Minimal Household Dashboard Home

## Status: DONE
**Epic:** EP-034 | **Priority:** P0 | **Points:** 3

Completed on 2026-06-13. Gate C and Gate D were delegated by the user goal and recorded in the ST-3401 workpack.

## Description
Turn the existing `/households/{householdId}` route into an accessible household home page with summaries for tasks, shopping lists, routines, and members using existing household-scoped frontend data hooks.

## User Value
Household members can land in one place, understand what needs attention, and navigate to the right workflow without hunting through separate sections.

## In Scope
- Add an explicit Home/Dashboard link to household navigation.
- Replace localStorage-only dashboard shopping data with backend-backed shopping list summaries.
- Show task counts for overdue, due today, upcoming, and done/open context.
- Show shopping list count and unpurchased item totals with links to shopping lists.
- Show routine count and active/paused split with links to routines.
- Show member count and admin/member split with links to members.
- Provide empty states and CTAs for new households.
- Keep the dashboard responsive on desktop and mobile.

## Out of Scope
- New `GET /api/v1/households/{id}/dashboard` endpoint.
- Backend code, database migrations, or OpenAPI updates.
- Dashboard personalization, analytics widgets, notification feed, charts, or caching.
- Redesigning the detailed tasks, shopping, routines, or members pages.
- Command pipeline changes.

## Acceptance Criteria

### AC-1: Dashboard Is Discoverable
Given a household member is inside a household route
When they open the household navigation
Then they see a Home/Dashboard link to `/households/{householdId}`.

### AC-2: Dashboard Shows Household Summary
Given household data loads successfully
When the member opens `/households/{householdId}`
Then they see cards for tasks, shopping lists, routines, and members
And each card shows meaningful counts and links to the detailed page.

### AC-3: Empty Household Is Useful
Given the household has no tasks, shopping lists, routines, or extra members
When the dashboard renders
Then it shows clear empty states and CTAs to create tasks, shopping lists, routines, or invites through existing flows.

### AC-4: Existing Household Boundaries Are Preserved
Given a household is selected
When the dashboard fetches data
Then it only uses existing APIs scoped by that selected `householdId`.

### AC-5: Responsive Layout Does Not Overlap
Given the dashboard is viewed on desktop and mobile widths
When the page renders summary cards and CTAs
Then text and controls remain readable without overlapping.

## Test Strategy
- TypeScript build for route/type correctness.
- ESLint for frontend conventions.
- Browser verification for desktop and mobile layout after APPLY.
- Manual evidence that all dashboard fetches use current-household hooks only.

## Flags
- contract_impact: no.
- data_impact: no.
- adr_needed: no.
- diagrams_needed: no.
- security_sensitive: medium, because the page aggregates household-scoped data.
- traceability_critical: low.

## Dependencies
- Existing `/households/:householdId` route.
- Existing hooks: `useTasks`, `useShoppingLists`, `useRoutines`, `useMembers`.
- Existing household navigation shell.

## Gate Notes
- Gate C: GO, recorded in `docs/planning/workpacks/ST-3401/gate-c.md`.
- Gate D: GO, recorded in `docs/planning/workpacks/ST-3401/gate-d.md`.
