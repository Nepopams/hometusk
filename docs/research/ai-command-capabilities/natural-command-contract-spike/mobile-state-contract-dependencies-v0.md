# Mobile State Contract Dependencies v0

Status: Draft only - no mobile implementation approval

## Decision

No mobile implementation in this initiative.

Mobile AI UX remains blocked until backend contracts are accepted and
implemented.

## Required States

| State | Backend dependency | Required fields |
| --- | --- | --- |
| Executed card | Existing `executed`; future richer action summary optional | `commandId`, `status`, affected entity ids/counts, summary |
| Clarify card | Existing `needs_input` | `question`, `requiredFields`, `suggestions`, `commandId` |
| Rejected card | Existing `rejected` | `errorCode`, `reason`, `commandId` |
| Confirmation card | Future `needs_confirmation` | `confirmationId`, `summary`, `reasons`, `riskLabels`, `proposedActions`, `expiresAt` |
| Degraded card | Existing `executed_degraded` | `degradedReason`, fallback summary |
| Pending confirmation timeline item | Future command/confirmation read model | status, timestamps, actor, expiry, short summary |

## Confirmation Card Requirements

Mobile/web will need:

- stable `confirmationId`;
- command id;
- user-safe summary;
- reason/risk labels;
- proposed actions with displayable fields;
- expiry timestamp;
- approve/cancel affordance;
- stale/expired handling;
- idempotent retry behavior;
- no raw provider payload;
- no raw audio.

## Blocked Until Backend Contract

- Mobile confirmation card.
- Mobile answer/status card.
- Pending confirmation persistence.
- Timeline of pending confirmations.
- Approval/cancel API calls.
- Direct provider trace display.

## Client Boundary

Mobile/web must call HomeTusk only. They must not call AI Platform directly and
must not store provider credentials or prompts.
