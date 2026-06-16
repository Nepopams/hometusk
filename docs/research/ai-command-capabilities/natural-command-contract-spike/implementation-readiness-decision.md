# Implementation Readiness Decision

Status: Completed

Date: 2026-06-16

## Decision

**LIMITED-GO**

Start a separate backend contract implementation initiative for HomeTusk
`natural_command` and `needs_confirmation` foundations.

Do not start mobile UX, `answered`, broad planner actions, direct mobile/web AI
Platform calls, production rollout, or AI Platform repo changes from this spike.

## Rationale

Positive evidence:

- AI Platform provider handoff reports 50 evaluated scenarios, 50 schema-valid decisions, 50 outcome matches, 0 unsupported auto-execute, 0 cross-household references, and 0 blocker failure scenarios.
- Provider `reject` is first-class under `2.1.0`.
- Provider `confirm` is schema-supported and non-executing.
- HomeTusk AI Platform intake already safely maps provider `confirm` to `AI_CONFIRMATION_UNSUPPORTED` until this contract exists.
- Draft HomeTusk request, response, confirmation, lifecycle, mapping, guardrails, traceability, mobile dependency, and OpenAPI delta artifacts now exist.

Limits:

- Provider eval still has non-blocker mismatch buckets: `wrong_intent=30`, `item_boundary_loss=2`.
- HomeTusk has no pending confirmation source of truth yet.
- Current public Commands API does not accept `natural_command`.
- Current public response model does not include `needs_confirmation`.
- Current mobile API types do not include confirmation states.
- `answer` remains blocked.

## Approved Next Direction

Open a new HomeTusk initiative:

```text
HomeTusk natural_command + needs_confirmation backend contract implementation
```

Recommended first implementation slices:

1. Public contract acceptance for `type=natural_command` request and response status vocabulary.
2. Backend model for pending confirmation state.
3. Provider `confirm` mapping from `AI_CONFIRMATION_UNSUPPORTED` to `needs_confirmation` only after pending state exists.
4. Approval/cancel contract and idempotency.
5. DecisionLog and confirmation audit events.
6. Narrow auto-execute corridor tests for clear create-task and shopping-addition scenarios.

## Non-Goals For Next Initiative Unless Separately Gated

- Mobile/web UI.
- `answered` response.
- Natural completion auto-execute.
- Natural reschedule auto-execute.
- Broad workload redistribution.
- External payment/device/order side effects.
- Direct mobile/web AI Platform calls.
- Production rollout/config.
- AI Platform repo writes.

## Gate D Outcome

Docs-only spike closure: **GO**.

Runtime/product/mobile readiness: **LIMITED-GO** as above.
