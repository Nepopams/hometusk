# PLAN Findings: ST-3505 - Mobile Command Chat and Controlled Outcomes

## Mode

Read-only PLAN completed on 2026-06-14 before APPLY.

## Findings

1. The command contract is structured; it does not accept arbitrary raw assistant chat text.
2. `POST /api/v1/commands` supports `create_task` and `complete_task` with `source=mobile`, idempotency, and correlation headers.
3. `/commands/{commandId}/continue` exists for commands in `needs_input`.
4. `clients/mobile/src/storage/localAppMemory.ts` already has recent command hint storage for non-sensitive history.
5. No contract/backend change is required for ST-3505.

## Approved Implementation Plan

1. Add mobile `continueCommand` API method and response fields.
2. Implement a deterministic mobile text command shell that maps text to existing structured commands.
3. Render controlled outcomes and continuation UI.
4. Persist recent command hints only in AsyncStorage local app memory.

## Gate C Recommendation

GO. The change preserves HomeTusk's command boundary and does not introduce generic assistant behavior.
