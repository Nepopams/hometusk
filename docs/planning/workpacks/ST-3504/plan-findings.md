# PLAN Findings: ST-3504 - Tasks and Shopping Mobile Mutations

## Mode

Read-only PLAN completed on 2026-06-14 before APPLY.

## Findings

1. `POST /api/v1/commands` supports `create_task` and `complete_task` with idempotency/correlation headers.
2. Web task completion already uses `executeCommand` with `type=complete_task`; mobile should follow the same boundary.
3. Shopping item add/update/delete endpoints already exist and are household-scoped.
4. `clients/mobile/src/api/client.ts` already has `executeCommand`; only shopping mutation methods are missing.
5. ST-3504 does not require contract/backend changes.

## Approved Implementation Plan

1. Add shopping mutation DTOs and client methods.
2. Add compact task create and complete controls in the Tasks surface.
3. Add shopping add/purchase/delete controls in the Shopping surface.
4. Refresh read models after successful mutation.
5. Show generic household-scoped error messages.

## Gate C Recommendation

GO. The change uses existing backend contracts and preserves the task command boundary.
