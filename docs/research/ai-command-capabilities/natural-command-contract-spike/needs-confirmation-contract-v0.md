# Needs Confirmation Contract v0

Status: Draft only

## Purpose

`needs_confirmation` represents a proposed HomeTusk action plan that is
understood but must not mutate state until an authorized household member
explicitly approves it.

## Response Shape

```json
{
  "commandId": "123e4567-e89b-12d3-a456-426614174010",
  "correlationId": "123e4567-e89b-12d3-a456-426614174099",
  "status": "needs_confirmation",
  "confirmation": {
    "confirmationId": "123e4567-e89b-12d3-a456-426614174020",
    "providerConfirmationId": "conf-1",
    "summary": "Create a task for another household member.",
    "reasons": ["Non-requester assignment requires confirmation."],
    "riskLabels": ["non_requester_assignment"],
    "expiresAt": "2026-06-16T09:10:00Z",
    "proposedActions": [
      {
        "type": "create_task",
        "title": "Clean kitchen",
        "assigneeId": "123e4567-e89b-12d3-a456-426614174002",
        "zoneId": null,
        "dueDate": null
      }
    ]
  },
  "trace": {
    "providerDecisionId": "55555555-5555-4555-8555-555555555555",
    "providerTraceId": "trace-confirm",
    "schemaVersion": "2.1.0",
    "decisionVersion": "mvp1-graph-0.1"
  },
  "executionMs": 123,
  "initiatorId": "123e4567-e89b-12d3-a456-426614174001"
}
```

## Required Semantics

- No domain mutation before explicit approval.
- Proposed actions are displayable proposals, not executed facts.
- HomeTusk mints and owns the public `confirmationId`.
- Provider `confirmation_id` is retained as `providerConfirmationId` for audit.
- Expiry is enforced by HomeTusk.
- Approval and cancel are idempotent.
- Household authorization is checked on approval and cancel.
- Proposed actions are revalidated before execution.
- Stale confirmations cannot execute.

## Future Approve / Cancel Proposals

Draft-only endpoint direction:

```text
POST /api/v1/commands/{commandId}/confirmations/{confirmationId}/approve
POST /api/v1/commands/{commandId}/confirmations/{confirmationId}/cancel
```

Alternative if implementation wants confirmation as a first-class resource:

```text
POST /api/v1/command-confirmations/{confirmationId}/approve
POST /api/v1/command-confirmations/{confirmationId}/cancel
```

The future implementation workpack must choose one accepted contract.

## Approval Rules

- Initiator may approve if still a household member.
- A different household member may approve only if future product policy allows it.
- Admin override is not approved by this spike.
- Approval must re-check membership, target entity existence, zone/list scope, deadline validity, and unsupported side effects.
- Reusing approval idempotency key after success returns the saved result.
- Reusing approval idempotency key with a different approval payload returns conflict.

## Stale / Expired Handling

- Expired confirmations return a controlled error and no mutation.
- Expired confirmations should create an audit event.
- If underlying entities change, HomeTusk revalidates and may reject or require re-planning.
- Provider proposed actions are not trusted after expiry.

## Public Error Codes

Draft codes:

- `CONFIRMATION_EXPIRED`
- `CONFIRMATION_CANCELLED`
- `CONFIRMATION_ALREADY_APPROVED`
- `CONFIRMATION_NOT_FOUND`
- `CONFIRMATION_FORBIDDEN`
- `CONFIRMATION_STALE`
- `CONFIRMATION_ACTION_UNSUPPORTED`
- `CONFIRMATION_VALIDATION_FAILED`
