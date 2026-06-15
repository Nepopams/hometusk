# Story: ST-3504 - Tasks and Shopping Mobile Mutations

## Status: DONE

**Epic:** EP-035 | **Priority:** P0 | **Points:** 5

## Description

Add the first mobile write flows for tasks and shopping using existing HomeTusk backend contracts.

## Acceptance Criteria

1. User can complete an open task through the existing command pipeline or approved backend endpoint.
2. User can create a task where the backend contract supports it.
3. User can add a shopping item.
4. User can mark a shopping item purchased.
5. User can delete a shopping item.
6. User can see task-shopping linkage in task detail or item detail.
7. Boundary errors do not leak cross-household data.

## Out of Scope

- Full offline-first mutation sync.
- Bulk edit.
- New mobile-specific domain model.

## Flags

- contract_impact: maybe, only if existing contracts are insufficient.
- security_sensitive: medium.
- traceability_critical: medium.

## Planning

- Workpack: `docs/planning/workpacks/ST-3504/`
- Gate C: delegated GO on 2026-06-14.
- Gate D: delegated GO on 2026-06-14.

## Evidence

- Review gate: `docs/planning/workpacks/ST-3504/review-gate.md`
- Gate D: `docs/planning/workpacks/ST-3504/gate-d.md`
