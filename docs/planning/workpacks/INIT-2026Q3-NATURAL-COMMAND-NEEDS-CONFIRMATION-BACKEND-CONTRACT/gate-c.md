# Gate C - INIT-2026Q3 Natural Command + Needs Confirmation Backend Contract

**Date:** 2026-06-16
**Decision:** GO for limited APPLY

## Approved Scope

Approved:

- Add accepted backend/API contract docs for `natural_command` and
  `needs_confirmation`.
- Add ADR and sequence diagram for pending confirmation state.
- Add backend `NATURAL_COMMAND` command type and schema validation.
- Add safe non-mutating degraded fallback for `natural_command`.
- Add persistent pending confirmation state.
- Map supported provider `confirm` responses to `needs_confirmation`.
- Run guardrails as proposal before creating pending confirmation.
- Record DecisionLog evidence with raw provider payload and HomeTusk
  confirmation id.
- Add/update tests for no mutation, validation, traceability, and compatibility.

Held:

- approve/cancel endpoint implementation;
- approval idempotency and stale execution rules;
- expiry scheduler;
- mobile/web UI;
- `answered`;
- production rollout/config changes.

## Evidence

- Prior contract spike closed with Gate D GO and LIMITED-GO for this backend
  implementation initiative.
- Current code lacks `natural_command`, `needs_confirmation`, and pending
  confirmation state.
- Draft contract package explicitly requires HomeTusk-owned confirmation id,
  no mutation before approval, and `DecisionLog` as audit evidence rather than
  state store.
- Existing provider `confirm` tests already prove current non-mutation behavior
  and can be updated to expect pending confirmation instead of unsupported
  rejection.

## Rationale

This slice gives downstream mobile/web work a real backend contract state to
integrate against without taking on approval execution and idempotency in the
same APPLY. It preserves the core safety invariant: provider `confirm` creates a
pending proposal, not a mutation.

## Allowed Files

See `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-NEEDS-CONFIRMATION-BACKEND-CONTRACT/plan-findings.md`.

## Gate C Addendum - GO

During APPLY, three small file-list deviations were required to complete the
approved behavior without expanding scope:

- `services/backend/src/main/java/com/hometusk/commands/domain/Command.java` -
  adds the `NEEDS_CONFIRMATION` command lifecycle transition.
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandRequest.java` -
  accepts the approved public `natural_command` type string.
- `services/backend/src/main/java/com/hometusk/commands/domain/CommandConfirmationStatusConverter.java` -
  persists the approved confirmation status enum using the existing lowercase
  enum converter pattern.

**Decision:** GO.

**Rationale:** The deviations are implementation-local and necessary for the
already-approved contract foundation. They do not add approve/cancel endpoints,
mobile/web UI, AI Platform writes, production rollout/config, or mutation before
explicit approval.

## STOP Conditions

Stop and record HOLD if implementation requires:

- mobile/web changes;
- AI Platform repo writes;
- executing provider `confirm` before explicit approval;
- breaking existing structured command clients;
- storing pending confirmation only in `DecisionLog`.
