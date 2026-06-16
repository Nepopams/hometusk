# Confirmation Lifecycle v0

Status: Draft only

## Lifecycle States

| State | Meaning |
| --- | --- |
| `CREATED` | Provider/HomeTusk proposed a confirmable plan |
| `PENDING_CONFIRMATION` | Proposal is visible and awaits approval/cancel/expiry |
| `CONFIRMED` | Authorized actor approved the proposal |
| `CANCELLED` | Authorized actor cancelled the proposal |
| `EXPIRED` | Confirmation expired before approval |
| `REJECTED` | HomeTusk rejected the proposal before execution |
| `EXECUTED` | Approved proposal executed successfully |
| `FAILED` | Execution failed after approval |

## Recommended Ownership

Use a future explicit confirmation/pending-command model.

Do not use only `DecisionLog` as the source of truth for pending confirmation
state. `DecisionLog` is audit evidence; it is not a workflow state store.

## Candidate Persistence Model

Future entity direction:

```text
CommandConfirmation
  id
  commandId
  householdId
  initiatorId
  providerConfirmationId
  providerDecisionId
  providerTraceId
  schemaVersion
  decisionVersion
  status
  summary
  reasons
  riskLabels
  proposedActionsJson
  expiresAt
  approvedBy
  approvedAt
  cancelledBy
  cancelledAt
  expiryProcessedAt
  createdAt
  updatedAt
```

## State Transitions

```text
CREATED -> PENDING_CONFIRMATION
PENDING_CONFIRMATION -> CONFIRMED
PENDING_CONFIRMATION -> CANCELLED
PENDING_CONFIRMATION -> EXPIRED
PENDING_CONFIRMATION -> REJECTED
CONFIRMED -> EXECUTED
CONFIRMED -> FAILED
```

Invalid transitions:

- `EXPIRED -> EXECUTED`
- `CANCELLED -> EXECUTED`
- `REJECTED -> EXECUTED`
- `EXECUTED -> CONFIRMED`

## Approval Flow

1. Load confirmation by HomeTusk `confirmationId`.
2. Check household membership and approval policy.
3. Check status is `PENDING_CONFIRMATION`.
4. Check not expired.
5. Revalidate proposed actions as current HomeTusk domain proposals.
6. Execute actions transactionally where possible.
7. Mark command/confirmation result.
8. Record DecisionLog / confirmation event evidence.
9. Return `executed` or controlled failure.

## Cancel Flow

1. Load confirmation.
2. Check household membership and cancel policy.
3. If already terminal, return idempotent result.
4. Mark `CANCELLED`.
5. Record actor and timestamp.
6. Return controlled cancelled response.

## Expiry Flow

- Expiry may be lazy on read/approve or processed by a scheduled job.
- Expired confirmations must not execute.
- Expiry must be auditable.

## Relationship to Command

Recommended future command status additions:

- `PENDING_CONFIRMATION`
- possibly `CONFIRMED`

The implementation may instead keep `Command` at a broader processing status
and use `CommandConfirmation.status` as the detailed lifecycle. That choice
should be made in the future implementation workpack/ADR if needed.
