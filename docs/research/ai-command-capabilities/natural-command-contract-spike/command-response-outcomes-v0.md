# Command Response Outcomes v0

Status: Draft only

## Current Public Outcomes

Current accepted `CommandResponse` variants:

| Status | Meaning |
| --- | --- |
| `executed` | Command executed successfully |
| `scheduled` | Command accepted for later execution |
| `needs_input` | Clarification required |
| `rejected` | Command rejected by AI, validation, or guardrails |
| `executed_degraded` | Deterministic fallback executed because AI path degraded |

## Future Target Outcomes

| Status | Decision | Notes |
| --- | --- | --- |
| `executed` | keep | Narrow auto-execute corridor only |
| `scheduled` | keep | Scheduling remains separate from confirmation |
| `needs_input` | keep | Clarification only; do not overload for confirmation |
| `needs_confirmation` | add later | First-class response for explicit approval before mutation |
| `rejected` | keep | Unsupported, unsafe, impossible, invalid, or ungrounded outcomes |
| `executed_degraded` | keep | Deterministic fallback only |
| `answered` | blocked | Separate read-only answer contract required |

## Key Decision

`needs_confirmation` should be a new first-class public response status.

Do not reuse `needs_input` for confirmation because:

- clarification asks for missing information;
- confirmation asks for approval of a known proposed plan;
- mobile/web need different UI, expiry, approval/cancel, and audit behavior;
- `needs_input` continuation semantics do not express no-mutation approval.

## Draft `needs_confirmation` Required Fields

- `commandId`
- `correlationId`
- `status=needs_confirmation`
- `confirmation.confirmationId`
- `confirmation.summary`
- `confirmation.reasons`
- `confirmation.riskLabels`
- `confirmation.expiresAt`
- `confirmation.proposedActions`
- `trace.providerDecisionId`
- `trace.providerTraceId`
- `trace.schemaVersion`
- `executionMs`
- `initiatorId`

## Rejection Semantics

`rejected` should remain the safe public outcome for:

- unsupported action;
- unsafe/impossible request;
- cross-household reference;
- unverifiable entity;
- provider invalid output;
- provider `confirm` before HomeTusk confirmation runtime is implemented;
- `answer` attempts before answer contract exists.

Provider details remain auditable through `DecisionLog.rawDecisionPayload`.
Public `rejected` should expose only user-safe `errorCode` and `reason`.

## Degraded Semantics

`executed_degraded` is allowed only for deterministic fallback behavior that
HomeTusk can safely execute without AI. Degraded mode must not invent a pending
confirmation or execute provider-proposed risky actions.
