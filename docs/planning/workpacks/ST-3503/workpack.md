# Workpack: ST-3503 - Household Home Read Models

## Sources of Truth

- Scope anchor: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.md`
- Execution index: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.execution.md`
- Epic: `docs/planning/epics/EP-035/epic.md`
- Story: `docs/planning/epics/EP-035/stories/ST-3503-household-home-read-models.md`
- Auth/session foundation: `docs/planning/workpacks/ST-3502/`
- REST contract: `docs/contracts/http/commands.openapi.yaml`
- Mobile app: `clients/mobile/`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

## Status

**DONE / GATE D GO.** Gate B is delegated GO after ST-3502 Gate D. Artifact gate is GO with no contract change because all required read endpoints already exist.

## Outcome

Authenticated mobile users can select a household, persist that selection as non-sensitive app memory, and view members, zones, tasks, shopping lists/items, and notifications for the selected household.

## Acceptance Criteria

- [x] AC-1: User sees households from `/api/v1/users/me`.
- [x] AC-2: User can select and persist the active household as non-sensitive local state.
- [x] AC-3: User can view members, zones, tasks, shopping lists, shopping items, and notifications for the selected household.
- [x] AC-4: Empty, loading, and error states are present.
- [x] AC-5: All reads are scoped to the selected household ID.
- [x] AC-6: No new backend aggregation endpoint is introduced.

## Non-goals

- Offline mutation queue.
- Push delivery.
- New backend aggregation endpoint.
- Task/shopping mutations; these remain ST-3504.

## Files to change

- `clients/mobile/App.tsx` - household selector and read model surfaces.
- `clients/mobile/src/api/client.ts` - household read API methods.
- `clients/mobile/src/api/types.ts` - DTOs for household read models.
- `clients/mobile/src/storage/localAppMemory.ts` - selected household storage is reused.
- `clients/mobile/README.md` - read model runbook note.
- `docs/planning/epics/EP-035/stories/ST-3503-household-home-read-models.md` - status/evidence.
- `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.execution.md` - status after closure.
- `docs/planning/workpacks/ST-3503/**` - evidence and gates.

Forbidden:

- `docs/integration/ai-platform/v1/upstream/**`
- New backend aggregation endpoint
- Direct mobile-to-AI-Platform code
- Sensitive token storage in AsyncStorage/plain storage
- `infra/uat/nginx/Dockerfile`

## Implementation Plan

1. Add mobile API types and client methods for existing household read endpoints.
2. Restore selected household from AsyncStorage only after validating it belongs to `/users/me`.
3. Load members, zones, tasks, shopping lists, shopping items, and notifications with the selected household ID.
4. Render loading, error, empty, and populated states in the existing mobile shell.
5. Keep task/shopping writes out of scope.

## Contract Impact

No contract change. ST-3503 consumes existing documented endpoints:

- `GET /api/v1/users/me`
- `GET /api/v1/households/{householdId}/members`
- `GET /api/v1/households/{householdId}/zones`
- `GET /api/v1/households/{householdId}/tasks`
- `GET /api/v1/households/{householdId}/shopping-lists`
- `GET /api/v1/households/{householdId}/shopping-lists/{listId}/items`
- `GET /api/v1/households/{householdId}/notifications`

## Verification Commands

- `cd clients/mobile && npm run typecheck`
- `cd clients/mobile && npx expo start --help`
- `rg -n "households/\\$\\{householdId\\}|readSelectedHouseholdId|writeSelectedHouseholdId|AsyncStorage|SecureStore" clients/mobile/src clients/mobile/App.tsx`

## Evidence

- Mobile typecheck: `cd clients/mobile && npm run typecheck` passed.
- Expo CLI smoke: `cd clients/mobile && npx expo start --help` passed.
- Source review: `rg -n "households/\\$\\{householdId\\}|selectedHouseholdId|readSelectedHouseholdId|writeSelectedHouseholdId|AsyncStorage|SecureStore|accessToken|refreshToken" clients/mobile/src clients/mobile/App.tsx clients/mobile/README.md clients/mobile/AGENTS.md` reviewed. Selected household uses AsyncStorage helper; access/refresh tokens remain in SecureStore/session API code.
- No contract/backend changes were required for ST-3503.

## Risks

- Loading shopping items for many lists can fan out requests. Mitigation: acceptable for MVP read model; no aggregation endpoint is added in ST-3503.
- Stored household ID may belong to an old account. Mitigation: validate against `/users/me` households before use.

## Rollback

- Revert ST-3503 mobile read methods, UI wiring, and workpack status changes.
- ST-3502 auth/session remains usable.

## Prompt Pack

- PLAN: `docs/planning/workpacks/ST-3503/prompt-plan.md`
- PLAN findings: `docs/planning/workpacks/ST-3503/plan-findings.md`
- Gate C: `docs/planning/workpacks/ST-3503/gate-c.md`
- APPLY: `docs/planning/workpacks/ST-3503/prompt-apply.md`
- Review gate: `docs/planning/workpacks/ST-3503/review-gate.md`
- Gate D: `docs/planning/workpacks/ST-3503/gate-d.md`
