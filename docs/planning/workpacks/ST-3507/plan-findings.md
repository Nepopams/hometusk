# PLAN Findings: ST-3507 - Push Receive, Deep Links, and Release Smoke Path

## Findings

1. `clients/mobile` currently has Expo 56, SecureStore, and AsyncStorage, but no notification/linking packages.
2. ST-3506 provides the needed backend registration boundary: `POST/PATCH/DELETE /api/v1/mobile/devices`.
3. Existing mobile navigation is a compact tab surface, not a route stack. The smallest safe implementation is target handoff into existing `home`, `tasks`, and `command` surfaces plus a status banner.
4. Existing backend invite accept contract can support invite deep links without new backend work.
5. Push token acquisition requires Expo/EAS project id in a dev build; code must handle missing project id without crashing or logging tokens.

## Decision

Use Expo Push Service for MVP registration, `expo-linking` for URL parsing, and notification response data payloads with safe target keys only. Do not add a new navigator, direct AI calls, or new backend contracts.

## Allowed Files

- `clients/mobile/package.json`
- `clients/mobile/package-lock.json`
- `clients/mobile/app.json`
- `clients/mobile/App.tsx`
- `clients/mobile/README.md`
- `clients/mobile/src/api/client.ts`
- `clients/mobile/src/api/types.ts`
- `clients/mobile/src/storage/localAppMemory.ts`
- `clients/mobile/src/notifications/**`
- `docs/planning/**`

## Verification

- Mobile typecheck.
- Expo CLI smoke.
- Dependency tree review.
- Source review for no push token logging and no direct AI Platform calls.
