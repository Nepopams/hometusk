# Gate C - Confirmation Approval Lifecycle Backend Contract

**Date:** 2026-06-16
**Decision:** GO for limited APPLY

## Approved Scope

- Add accepted public Commands API approve/cancel endpoints.
- Add lifecycle persistence fields for approval, cancellation, expiry, and
  execution result.
- Enforce initiator-only approval/cancel.
- Implement lazy expiry on approve.
- Re-run guardrails before approved execution.
- Execute supported stored proposed actions through existing `ActionExecutor`.
- Record DecisionLog lifecycle evidence.
- Add integration tests for approve/cancel/idempotency/auth/expiry.

## Held Scope

- Expiry scheduler.
- Mobile/web UI.
- `answered`.
- Editing proposed actions.
- Broader household approval policies.
- Production rollout/config.
- AI Platform repo writes or upstream snapshot changes.

## Evidence

- Parent Gate D recorded LIMITED-GO only because approve/cancel lifecycle was
  missing.
- Draft lifecycle artifact defines approval/cancel/expiry semantics.
- Existing `command_confirmations` table is the correct HomeTusk-owned state
  anchor.
- Current command pipeline already has guardrails and action execution helpers.

## Rationale

This slice completes the backend confirmation loop while preserving HomeTusk's
execution authority. It keeps policy narrow and testable: only the original
initiator may approve/cancel, and provider proposals are revalidated before
mutation.

## Approved Files

See `workpack.md`.

## STOP Conditions

Stop and record HOLD if implementation requires mobile/web UI, AI Platform
writes, direct client-to-provider calls, mutation before approval, or
authorization beyond current user/initiator data.
