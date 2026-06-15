# PLAN Findings: ST-3503 - Household Home Read Models

## Mode

Read-only PLAN completed on 2026-06-14 before APPLY.

## Findings

1. `/api/v1/users/me` already returns household summaries and is loaded after ST-3502 auth.
2. Existing contract/backend endpoints cover members, zones, tasks, shopping lists, shopping items, and notifications.
3. `clients/web/src/lib/api.ts` already consumes the same endpoints and confirms query/path semantics.
4. `clients/mobile/src/storage/localAppMemory.ts` already has selected-household AsyncStorage helpers for non-sensitive state.
5. No backend aggregation endpoint is needed for ST-3503; shopping item reads can fan out per list for MVP.
6. Task/shopping mutations remain out of scope for ST-3504.

## Approved Implementation Plan

1. Add mobile DTOs and API methods for existing household read endpoints.
2. Validate persisted household ID against current `/users/me` households before using it.
3. Load read models scoped to the selected household ID.
4. Render household selector plus loading, error, empty, and populated states.
5. Verify mobile typecheck, Expo CLI smoke, and source review.

## Gate C Recommendation

GO. The change is read-only from a backend perspective, uses existing contracts, and advances the Native Mobile MVP without expanding service boundaries.
