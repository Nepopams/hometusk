# ADR-022: Pending Command Confirmation State

**Status**: accepted
**Date**: 2026-06-16
**Supersedes**: none

## Context

HomeTusk now supports AI Platform `2.1.0` provider decisions. Provider `confirm`
means the planner understood a proposed action plan, but HomeTusk must not mutate
domain state until an authorized user explicitly approves it.

The prior contract spike decided that `DecisionLog` is audit evidence, not a
workflow state store. The backend contract implementation therefore needs a
HomeTusk-owned source of truth for pending confirmations before public
`needs_confirmation` can be returned safely.

## Decision

Add an explicit `command_confirmations` persistence model owned by HomeTusk.

The first implementation slice supports:

- HomeTusk-owned `confirmationId`;
- original `commandId`, `householdId`, and `initiatorId`;
- provider confirmation/decision/trace/version metadata;
- status `PENDING_CONFIRMATION`;
- user-safe summary, reasons, risk labels, expiry, and proposed actions;
- `DecisionLog` entry that records the mapped `needs_confirmation` outcome and
  raw provider payload;
- no domain mutation before explicit approval.

The approval lifecycle continuation supports:

- initiator-only approve/cancel endpoints under the existing Commands API;
- lazy expiry on approval attempt;
- guardrails revalidation before executing stored proposed actions;
- terminal-state idempotency for repeated approve/cancel calls;
- execution result persistence for stable repeated approval response;
- DecisionLog entries for approval, cancellation, expiry, and guardrail
  rejection during approval.

Broader approval policies, proposal editing, mobile/web UI, production rollout,
and an expiry scheduler remain out of scope.

## Consequences

Positive:

- `needs_confirmation` has a durable HomeTusk-owned state instead of relying on
  provider ids or `DecisionLog`.
- Mobile/web can later integrate against a stable backend confirmation id.
- Provider `confirm` no longer has to be flattened into unsupported rejection
  when the proposed actions are supported.
- No-mutation-before-approval can be tested directly.
- Approval execution remains HomeTusk-owned: provider `confirm` is never treated
  as execution authority.
- Repeated approval/cancel calls can be handled without duplicate mutation.

Negative:

- A new database table and status vocabulary are introduced.
- Approval policy is intentionally narrow: only the original command initiator
  can approve or cancel in this slice.
- Expiry processing is lazy on approval attempt; no scheduler is introduced in
  this slice.

## Alternatives Considered

### Store pending confirmation only in DecisionLog

Rejected. `DecisionLog` is append-only audit evidence and does not model current
workflow state, actor policy, expiry, or future idempotent approval.

### Keep provider `confirm` as `AI_CONFIRMATION_UNSUPPORTED`

Rejected for this initiative. That behavior was the safe interim posture, but
it blocks the backend contract foundation required before Mobile AI Command UX.

### Implement approve/cancel in the first pending-state slice

Deferred. Approval execution needs its own contract, idempotency, stale proposal,
and authorization decisions. Combining it with pending-state creation would make
Gate C too broad.

### Allow any household member to approve

Deferred. Household-wide approval may be a valid product policy later, but this
slice keeps authorization strict and auditable by allowing only the original
initiator to approve or cancel.
