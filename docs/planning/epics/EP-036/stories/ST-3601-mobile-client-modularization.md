# ST-3601 - Behavior-Preserving Mobile Client Modularization

## Status: DONE

Gate A/B/C/D were delegated by the user goal on 2026-06-15. APPLY, verification, review gate, and Gate D are complete.

## User Value

Users should see no behavior change. The value is engineering readiness: the mobile command surface can evolve without changing unrelated auth, household, shopping, push, or app shell code.

## Acceptance Criteria

- [x] `clients/mobile/App.tsx` is reduced to app entrypoint composition.
- [x] `clients/mobile/src/app/AppShell.tsx` owns root lifecycle and state orchestration.
- [x] Command feature modules exist under `clients/mobile/src/features/command/`.
- [x] Command request building is isolated from UI rendering.
- [x] Continuation parsing is isolated from UI rendering.
- [x] Auth/session bootstrap is isolated behind `features/auth/authSessionController.ts`.
- [x] Household selection persistence is isolated behind `features/households/selectedHouseholdStore.ts`.
- [x] Task and shopping mutations are behind feature mutation helpers.
- [x] Push registration is behind `features/notifications/pushRegistrationController.ts`.
- [x] Shared UI primitives live under `clients/mobile/src/shared/ui/`.
- [x] No new dependencies are introduced.
- [x] `cd clients/mobile && npm run typecheck` passes.
- [x] Backend CI Spotless failure is fixed with formatting-only backend changes.
- [x] Gate D review evidence is recorded.

## Out of Scope

- Backend endpoint changes.
- REST or AI Platform contract changes.
- New command semantics.
- Natural command implementation.
- Mobile voice input.
- Generic assistant chat.
- Direct AI Platform calls from mobile.
- Navigation or state framework migration.
- Visual redesign.
- Push provider change.
- Offline mutation sync.

## Gate Notes

- Gate A/B: delegated GO on 2026-06-15.
- Artifact gate: GO; no ADR, diagram, or contract update required.
- Gate C: delegated GO for the workpack scope.
- Gate D: GO, recorded in `docs/planning/workpacks/ST-3601/gate-d.md`.
