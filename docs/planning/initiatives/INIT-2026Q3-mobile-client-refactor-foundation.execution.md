# INIT-2026Q3-mobile-client-refactor-foundation - Execution Index

**Initiative:** Mobile Client Refactor Foundation
**Status:** DONE; delegated Gate A/B/C/D GO; ST-3601 complete
**Last Updated:** 2026-06-15

---

## Sources of Truth

- Initiative: `docs/planning/initiatives/INIT-2026Q3-mobile-client-refactor-foundation.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Product goal: `docs/planning/strategy/product-goal.md`
- Workflow: `docs/CODEX-WORKFLOW.md`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Epic: `docs/planning/epics/EP-036/epic.md`
- Workpack: `docs/planning/workpacks/ST-3601/workpack.md`
- Mobile app: `clients/mobile/`

---

## Gate Decisions

### Gate A - Roadmap / Initiative Scope

- Decision: GO
- Decider: Codex, under delegated human-gate authority from user goal
- Date: 2026-06-15
- Decision detail: Move the initiative into NOW as a parallel engineering enablement track beside Voice Command Chat MVP and Social Auth carry-over.

### Gate B - Execution Commitment

- Decision: GO
- Decider: Codex, under delegated human-gate authority from user goal
- Date: 2026-06-15
- Committed slice: ST-3601 behavior-preserving mobile client refactor foundation.

### Artifact Gate

- Decision: GO without ADR/contract/diagram changes.
- Reason: Scope is module decomposition only. No backend endpoint, contract, AI Platform, persistence, or runtime architecture boundary changes are introduced.

### Gate C - APPLY Approval

- Decision: GO
- Decider: Codex, under delegated human-gate authority from user goal
- Date: 2026-06-15
- Scope: `clients/mobile/App.tsx`, `clients/mobile/src/app/**`, `clients/mobile/src/features/**`, `clients/mobile/src/shared/**`, mobile README, planning artifacts, and CI formatting-only backend cleanup if required.

---

## Epic Decomposition

### EP-036 - Mobile Client Refactor Foundation

| ID | Title | Priority | Status | Workpack |
|----|-------|----------|--------|----------|
| ST-3601 | Behavior-preserving mobile client modularization | P0 | DONE | `docs/planning/workpacks/ST-3601/` |

---

## Scope Rules

- Preserve current mobile behavior.
- Keep HomeTusk backend as source of truth.
- Keep command execution through `/api/v1/commands`.
- Do not call AI Platform directly from mobile.
- Do not add mobile voice, natural_command, generic assistant behavior, or a navigation/state framework migration.
- Do not change backend/API/AI Platform contracts.
- Keep AsyncStorage/plain storage non-sensitive only.

---

## Verification Plan

- `cd clients/mobile && npm run typecheck`
- Backend CI formatting/build check after applying Spotless to the already-merged mobile backend files.
- Manual source review for:
  - command builder and continuation parser isolation;
  - SecureStore-only sensitive token boundary;
  - unchanged command/task/shopping/push/deep-link call paths.

---

## Current Evidence

- `clients/mobile/App.tsx` is a thin entrypoint to `src/app/AppShell.tsx`.
- Command UI, request building, continuation parsing, outcome formatting, and recent-command storage are under `clients/mobile/src/features/command/`.
- Auth/session, household selection, home, tasks, shopping, notifications, shared UI, shared formatters, and shared API error formatting are split into focused modules.
- No new runtime dependency was added.
- `cd clients/mobile && npm run typecheck` passed.
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain build --no-daemon` passed, including `spotlessJavaCheck`.
- Review gate result: GO; recorded at `docs/planning/workpacks/ST-3601/review-gate.md`.
