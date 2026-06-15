# PLAN Findings: ST-3501 - Mobile Stack ADR and App Foundation

## Mode

Read-only PLAN completed on 2026-06-14 before runtime scaffold APPLY.

## Sources Read

- `docs/planning/workpacks/ST-3501/workpack.md`
- `docs/planning/epics/EP-035/epic.md`
- `docs/planning/epics/EP-035/stories/ST-3501-mobile-stack-app-foundation.md`
- `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.md`
- `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.execution.md`
- `docs/adr/020-native-mobile-client-stack.md`
- `docs/diagrams/sequence-mobile-push-deep-link.md`
- `docs/contracts/http/mobile-devices.openapi.yaml`
- `docs/architecture/service-catalog.md`
- `clients/web/src/lib/api.ts`
- `clients/web/src/types/api.ts`
- `clients/web/package.json`

## Findings

1. There is no existing `clients/mobile` directory. ST-3501 can create it without overwriting a mobile client.
2. `clients/web` is a React + TypeScript client with hand-written API/types. ST-3501 should mirror the boundary style, but not copy browser-only session assumptions.
3. `npx create-expo-app@latest --help` works and supports `--template blank-typescript`, `--yes`, and `--no-agents-md`.
4. Existing notification list/read endpoints are already available for later mobile reads:
   - `GET /api/v1/households/{householdId}/notifications`
   - `POST /api/v1/notifications/{notificationId}/read`
5. Mobile device registration endpoints are not present in runtime code. They remain ST-3506 scope and must not be implemented in ST-3501.
6. ADR-020 selects React Native + Expo + TypeScript, Expo Push Service for MVP, secure storage for sensitive session material, and non-sensitive local app memory only.

## Files To Change In APPLY

- `clients/mobile/**`
- `docs/planning/workpacks/ST-3501/checklist.md`
- `docs/planning/workpacks/ST-3501/workpack.md`
- `docs/planning/workpacks/ST-3501/gate-c.md`
- `docs/planning/workpacks/ST-3501/prompt-apply.md`

Already prepared before APPLY:

- `docs/adr/020-native-mobile-client-stack.md`
- `docs/diagrams/sequence-mobile-push-deep-link.md`
- `docs/contracts/http/mobile-devices.openapi.yaml`
- planning, index, and service catalog docs

## Implementation Plan

1. Generate `clients/mobile` with Expo blank TypeScript using `create-expo-app`.
2. Remove generator-created agent/legacy instruction files if any conflict with HomeTusk repository instructions.
3. Add a HomeTusk-specific app shell with Home, Tasks, Shopping, and Command surfaces.
4. Add explicit boundaries:
   - `src/config/env.ts` for API base configuration;
   - `src/storage/secureSessionStore.ts` for sensitive session token storage;
   - `src/storage/localAppMemory.ts` for non-sensitive local state;
   - `src/api/client.ts` for HomeTusk backend calls only.
5. Add `clients/mobile/README.md` and `clients/mobile/AGENTS.md`.
6. Run `npm install`, `npm run typecheck`, and `npx expo start --help`.
7. Update checklist/workpack evidence.

## Stop-The-Line Conditions

- Stop if generator would overwrite an existing mobile client.
- Stop if scaffold requires Firebase/Supabase, direct AI Platform calls, PWA, or Capacitor.
- Stop if secure storage cannot be represented for token material.
- Stop if typecheck cannot run because dependency installation fails; record the environment blocker.
- Stop if ST-3501 requires backend runtime or migration changes.

## Verification Commands

- `cd clients/mobile && npm install`
- `cd clients/mobile && npm run typecheck`
- `cd clients/mobile && npx expo start --help`

## Gate C Recommendation

GO. ST-3501 scope is bounded, additive, and has no backend runtime changes. Proceed to APPLY with the approved files and stop conditions above.
