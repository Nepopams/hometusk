# PLAN Findings - Confirmation Approval Lifecycle Backend Contract

**Date:** 2026-06-16
**Mode:** Read-only exploration completed before Gate C.

## Findings

- The existing draft delta already proposes:
  - `POST /api/v1/commands/{commandId}/confirmations/{confirmationId}/approve`
  - `POST /api/v1/commands/{commandId}/confirmations/{confirmationId}/cancel`
- The current runtime has `CommandConfirmation` but lacks approval actor,
  cancellation actor, expiry processing, and execution result persistence.
- The safest first authorization policy is initiator-only approval/cancel.
  Broader household approval policy is product scope and should not be guessed.
- Repeated approve/cancel can be idempotent by terminal confirmation state:
  - repeated `EXECUTED` approval returns stored execution result and does not
    execute again;
  - repeated `CANCELLED` cancel returns cancelled and does not mutate;
  - opposite terminal transitions return conflict.
- Approval should lazily expire stale confirmations before execution.
- Approval must re-run guardrails against stored proposed actions before action
  execution.
- A pessimistic lock on confirmation load reduces duplicate execution risk.

## Gate Recommendations

- Contract gate: GO for additive approve/cancel endpoints under existing
  Commands API.
- ADR gate: GO to update ADR-022; no new ADR needed because this is the planned
  lifecycle continuation of the same data model.
- Diagram gate: GO to update the existing sequence diagram.
- Security gate: GO with initiator-only auth and integration tests.
- Gate C: GO for limited APPLY.

## Held Scope

- Expiry scheduler.
- Approval policy broader than original initiator.
- Client UI.
- Editing proposed actions before approval.
- Production rollout/config.

## Verification Required

- Provider confirm still creates pending state without mutation.
- Approve executes supported action once.
- Repeated approve does not duplicate mutation.
- Cancel does not mutate and is repeat-safe.
- Non-initiator cannot approve/cancel.
- Expired approval does not mutate and marks confirmation expired.
