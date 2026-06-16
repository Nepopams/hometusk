# Provider Confirm Mapping v0

Status: Draft only

## Current Behavior

Current HomeTusk behavior for AI Platform `2.1.0`:

```text
provider action=confirm
provider decision_outcome=confirm
-> schema-valid provider response
-> HomeTusk maps to DecisionResult.Reject
-> public status=rejected
-> errorCode=AI_CONFIRMATION_UNSUPPORTED
-> no mutation
-> raw provider payload preserved in DecisionLog.rawDecisionPayload
```

This remains correct until HomeTusk implements a first-class confirmation
contract.

## Future Behavior

Future mapping after contract implementation:

```text
provider action=confirm
provider decision_outcome=confirm
-> validate provider schema
-> validate proposed actions are supported
-> map provider proposed actions into HomeTusk proposed actions
-> run guardrail pre-check as proposal
-> create HomeTusk pending confirmation
-> write DecisionLog with raw provider payload and confirmation id
-> return status=needs_confirmation
-> no domain mutation
```

## Required Provider Fields

From AI Platform `2.1.0`:

- `decision_id`
- `command_id`
- `action=confirm`
- `decision_outcome=confirm`
- `confidence`
- `payload.confirmation_id`
- `payload.summary`
- `payload.reasons`
- `payload.proposed_actions`
- `payload.expires_at`
- `trace_id`
- `schema_version`
- `decision_version`
- `created_at`

## Fields HomeTusk Must Add

- HomeTusk `confirmationId`.
- Household id.
- Initiator id.
- Approval/cancel actor ids.
- HomeTusk expiry enforcement.
- Risk labels if provider does not provide stable labels.
- Supported/unsupported action validation.
- Guardrail pre-check result.
- Idempotency/correlation metadata.

## Unsupported Proposed Actions

If provider `confirm` includes unsupported proposed actions:

- do not create executable pending confirmation;
- return `rejected` or `needs_input` according to accepted future policy;
- preserve raw provider payload;
- record `CONFIRMATION_ACTION_UNSUPPORTED` or equivalent stable code;
- no mutation.

## Expiry Ownership

Provider may suggest `payload.expires_at`, but HomeTusk owns public expiry
semantics. Future implementation must decide:

- maximum TTL;
- minimum TTL;
- whether provider expiry can be shortened but not lengthened;
- clock source;
- behavior when provider expiry is absent or invalid.

## Confirmation ID Ownership

- Provider `confirmation_id` is not public authority.
- HomeTusk public `confirmationId` is the id used by clients.
- Provider id is stored for traceability only.
