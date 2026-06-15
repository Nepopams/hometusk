# ST-3601 Codex APPLY Prompt

Implement only the approved behavior-preserving mobile refactor scope:

- keep `clients/mobile/App.tsx` as a thin entrypoint;
- create `clients/mobile/src/app/AppShell.tsx`, app types, surfaces, and read-model helpers;
- extract auth, household, home, tasks, shopping, command, notifications, and shared UI/helper modules;
- preserve existing command/task/shopping/push/deep-link/session behavior;
- do not add dependencies;
- do not change backend/API/AI Platform contracts;
- do not implement natural command, mobile voice, generic assistant, direct AI Platform calls, or visual redesign;
- update mobile README and planning artifacts;
- apply only formatting-only backend Spotless cleanup needed for CI.

Verification:

- `cd clients/mobile && npm run typecheck`
- backend Spotless/build checks
- source review against initiative exit criteria.
