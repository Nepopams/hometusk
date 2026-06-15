# Story: ST-3503 - Household Home Read Models

## Status: DONE

**Epic:** EP-035 | **Priority:** P0 | **Points:** 5

## Description

Let an authenticated mobile user select a household and view the core read models needed for daily use: members, zones, tasks, shopping lists/items, and notification list state.

## Acceptance Criteria

1. User sees their households from `/api/v1/users/me`.
2. User can select and persist the active household as non-sensitive local state.
3. User can view members, zones, tasks, shopping lists, and shopping items for the selected household.
4. Empty, loading, and error states are present.
5. All reads stay scoped to the selected household ID.
6. Existing notification list endpoint can be displayed or explicitly deferred if it does not fit the first screen.

## Out of Scope

- Offline mutation queue.
- Push delivery.
- New backend aggregation endpoint.

## Flags

- contract_impact: no expected.
- security_sensitive: medium.
- traceability_critical: low.

## Planning

- Workpack: `docs/planning/workpacks/ST-3503/`
- Gate C: delegated GO on 2026-06-14.
- Gate D: delegated GO on 2026-06-14.

## Evidence

- Review gate: `docs/planning/workpacks/ST-3503/review-gate.md`
- Gate D: `docs/planning/workpacks/ST-3503/gate-d.md`
