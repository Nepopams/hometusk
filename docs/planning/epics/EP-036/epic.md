# Epic: EP-036 - Mobile Client Refactor Foundation

## Sources of Truth

- Initiative: `docs/planning/initiatives/INIT-2026Q3-mobile-client-refactor-foundation.md`
- Execution index: `docs/planning/initiatives/INIT-2026Q3-mobile-client-refactor-foundation.execution.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Native Mobile MVP baseline: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.md`
- Mobile app: `clients/mobile/`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

## Status

**DONE.** Gate A/B/C/D were delegated GO on 2026-06-15. ST-3601 is complete.

## Initiative Alignment

This epic implements the behavior-preserving refactor foundation after Native Mobile MVP. It prepares the mobile client for future AI-command and voice work without adding new command semantics, backend endpoints, contracts, or direct AI Platform calls.

## Epic Goal

Reduce the mobile app monolith by moving app orchestration, feature surfaces, command-specific logic, reusable UI primitives, storage helpers, and side-effect helpers into focused modules while preserving the current installed-app behavior.

## Stories

| ID | Title | Priority | Status |
|----|-------|----------|--------|
| [ST-3601](./stories/ST-3601-mobile-client-modularization.md) | Behavior-preserving mobile client modularization | P0 | DONE |

## Exit Criteria

1. `clients/mobile/App.tsx` is a thin entrypoint.
2. `clients/mobile/src/app/AppShell.tsx` owns root app orchestration.
3. Command feature is extracted under `clients/mobile/src/features/command/`.
4. Auth, household, home, tasks, shopping, notifications, and shared UI responsibilities no longer live in the root entrypoint.
5. Command request builder and continuation parser are pure helpers isolated from UI rendering.
6. Existing command/task/shopping/push/deep-link/session behavior is preserved.
7. AsyncStorage remains non-sensitive only.
8. `cd clients/mobile && npm run typecheck` passes.
9. No backend/API/AI Platform contract changes are introduced.
10. No new AI-command semantics are introduced.

## Gate Notes

- Gate A: GO by delegated user objective on 2026-06-15.
- Gate B: GO for ST-3601 as the committed refactor slice.
- Artifact gate: GO with no ADR/contract/diagram updates required.
- Gate C: GO, recorded in `docs/planning/workpacks/ST-3601/gate-c.md`.
- Gate D: GO, recorded in `docs/planning/workpacks/ST-3601/gate-d.md`.
