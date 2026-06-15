# Workpack: ST-3601 - Behavior-Preserving Mobile Client Modularization

## Sources of Truth

- Scope anchor: `docs/planning/initiatives/INIT-2026Q3-mobile-client-refactor-foundation.md`
- Execution index: `docs/planning/initiatives/INIT-2026Q3-mobile-client-refactor-foundation.execution.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Epic: `docs/planning/epics/EP-036/epic.md`
- Story: `docs/planning/epics/EP-036/stories/ST-3601-mobile-client-modularization.md`
- Mobile app: `clients/mobile/`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

## Status

**DONE - GATE D GO.** Delegated Gate A/B/C, APPLY, verification, review gate, and Gate D are complete as of 2026-06-15.

## Outcome

The mobile client keeps the Native Mobile MVP behavior while moving feature responsibilities out of the root `App.tsx` into focused `src/app`, `src/features`, and `src/shared` modules.

## Acceptance Criteria

- [x] `clients/mobile/App.tsx` is a thin app entrypoint.
- [x] `clients/mobile/src/app/AppShell.tsx` owns app orchestration.
- [x] Command surface, composer, continuation UI, outcome card, request builder, continuation parser, outcome formatting, and recent-command storage wrapper live under `clients/mobile/src/features/command/`.
- [x] Auth/session, household selection, task mutations, shopping mutations, push registration, and shared API error formatting are separated from UI rendering where practical.
- [x] Shared UI primitives live under `clients/mobile/src/shared/ui/`.
- [x] No visual redesign.
- [x] No new mobile dependencies.
- [x] No backend/API/AI Platform contract changes.
- [x] `cd clients/mobile && npm run typecheck` passes.
- [x] Backend Spotless/CI formatting issue is fixed.

## Implementation Evidence

- `clients/mobile/App.tsx` is a thin entrypoint importing `AppShell`.
- `clients/mobile/src/app/` contains `AppShell`, surface metadata, app types, and read-model defaults.
- `clients/mobile/src/features/command/` contains command UI modules, pure command request building, continuation parsing, outcome formatting, and recent-command storage wrapper.
- `clients/mobile/src/features/auth/` contains `AuthScreen` and secure-session controller extraction.
- `clients/mobile/src/features/households/`, `home/`, `tasks/`, `shopping/`, and `notifications/` contain their feature UI or side-effect helpers.
- `clients/mobile/src/shared/` contains reusable UI primitives, formatters, and API error formatting.
- `clients/mobile/README.md` documents the new source layout.
- Spotless formatting was applied to the already-merged backend mobile device files that caused CI failure.

## Files to Change

- `clients/mobile/App.tsx`
- `clients/mobile/src/app/**`
- `clients/mobile/src/features/**`
- `clients/mobile/src/shared/**`
- `clients/mobile/README.md`
- `docs/planning/strategy/roadmap.md`
- `docs/planning/initiatives/INIT-2026Q3-mobile-client-refactor-foundation.md`
- `docs/planning/initiatives/INIT-2026Q3-mobile-client-refactor-foundation.execution.md`
- `docs/planning/epics/EP-036/**`
- `docs/planning/workpacks/ST-3601/**`

Formatting-only CI fix allowed:

- `services/backend/src/main/java/com/hometusk/mobile/api/MobileDeviceController.java`
- `services/backend/src/main/java/com/hometusk/mobile/service/MobileDeviceService.java`
- `services/backend/src/test/java/com/hometusk/integration/MobileDeviceControllerIntegrationTest.java`

Forbidden:

- `docs/integration/ai-platform/v1/upstream/**`
- REST/OpenAPI contract changes
- Backend runtime behavior changes
- New mobile runtime dependencies

## Implementation Plan

1. Move root app composition into `src/app/AppShell.tsx` and keep `App.tsx` as the Expo entrypoint.
2. Extract app-level types, surface metadata, and read-model factory.
3. Extract auth/session, household selection, notification push registration, task/shopping mutation helpers.
4. Extract command feature modules and pure command helpers.
5. Extract shared UI primitives, formatters, and API error formatting.
6. Update mobile README with module layout.
7. Run mobile typecheck.
8. Apply Spotless formatting fix for the already-merged backend mobile files that currently fail CI.

## Verification Commands

- `cd clients/mobile && npm install`
- `cd clients/mobile && npm run typecheck`
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain spotlessApply`
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain build --no-daemon`

## Verification Evidence

- `cd clients/mobile && npm install` - passed; npm audit still reports 10 existing moderate vulnerabilities.
- `cd clients/mobile && npm run typecheck` - passed.
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain spotlessApply` - passed.
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain build --no-daemon` - passed, including `spotlessJavaCheck`.
- `git diff --check` - passed with line-ending warnings only.

## Risks

- Behavior drift in command parsing. Mitigation: keep `buildCommandRequestFromText` behavior identical and isolate it for focused review.
- Auth/session regression. Mitigation: controller extraction preserves SecureStore flow and refresh semantics.
- Push/deep-link regression. Mitigation: keep routing in AppShell lifecycle and extract only helpers/registration side effects.
- Over-splitting. Mitigation: no new framework, no new dependencies, and one shell component remains responsible for orchestration.

## Rollback

- Revert `clients/mobile/App.tsx`, `clients/mobile/src/app/**`, `clients/mobile/src/features/**`, `clients/mobile/src/shared/**`, and docs added for EP-036/ST-3601.
- Backend Spotless changes are formatting-only and can be reverted independently if needed.

## Prompt Pack

- PLAN: `docs/planning/workpacks/ST-3601/prompt-plan.md`
- PLAN findings: `docs/planning/workpacks/ST-3601/plan-findings.md`
- Gate C: `docs/planning/workpacks/ST-3601/gate-c.md`
- APPLY: `docs/planning/workpacks/ST-3601/prompt-apply.md`
- Review gate: `docs/planning/workpacks/ST-3601/review-gate.md`
- Gate D: `docs/planning/workpacks/ST-3601/gate-d.md`
