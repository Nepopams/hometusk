# ST-3601 PLAN Findings

## Scope Decision

Proceed with a behavior-preserving mobile refactor only. No backend endpoint, REST/OpenAPI contract, AI Platform contract, navigation framework, or product behavior changes are required.

## Findings

- `clients/mobile/App.tsx` mixed app shell, auth/session bootstrap, household persistence, read-model loading, push/deep-link routing, task/shopping mutations, command parsing, command UI, shared UI primitives, and formatters.
- Command logic is the highest-value extraction target because future voice/AI-command work should be able to change `src/features/command/**` without touching auth/shopping/push/household code.
- The current mobile package has no test runner beyond TypeScript. Adding Vitest/Jest only for this refactor would introduce new dependency and setup churn, so pure helper verification remains source-review plus `npm run typecheck`.
- GitHub CI failure observed after Native Mobile MVP merge is Spotless formatting in backend mobile files, not a behavioral test failure.

## Approved APPLY Scope

- Mobile module extraction under `clients/mobile/src/app/**`, `clients/mobile/src/features/**`, and `clients/mobile/src/shared/**`.
- Mobile README structure update.
- Planning artifacts for INIT/EP/ST/workpack.
- Formatting-only backend Spotless correction for CI.

## Guardrails

- Preserve deterministic command behavior:
  - plain text creates a task;
  - `done`/`complete` matches open task by id prefix or title substring;
  - `key=value` continuation input becomes an object;
  - non-pair continuation input becomes `{ clarification }`.
- Preserve SecureStore for sensitive tokens.
- Preserve AsyncStorage only for non-sensitive selected household, device registration id, and recent command hints.
- Preserve API calls and headers.
