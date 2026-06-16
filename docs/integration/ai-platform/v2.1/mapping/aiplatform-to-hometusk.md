# AI Platform v2.1 to HomeTusk Mapping

## Response Envelope

AI Platform `2.1.0` returns:

```text
decision_id
command_id
status
action
decision_outcome optional
confidence
payload
explanation
trace_id
schema_version
decision_version
created_at
```

HomeTusk validates the raw provider JSON against the `2.1.0` schema before
mapping to an internal `DecisionResult`.

## Decision Mapping

| Provider action | Provider outcome | HomeTusk result | Mutation |
| --- | --- | --- | --- |
| `start_job` | `execute` | `DecisionResult.StartJob` | Only after HomeTusk guardrails and domain validation. |
| `propose_create_task` | `execute` | `DecisionResult.StartJob` with `create_task` | Only after HomeTusk guardrails and domain validation. |
| `propose_add_shopping_item` | `execute` | `DecisionResult.StartJob` with `add_shopping_item` | Only after HomeTusk guardrails and domain validation. |
| `clarify` | `clarify` | `DecisionResult.Clarify` | No mutation. |
| `reject` | `reject` | `DecisionResult.Reject` | No mutation. |
| `confirm` | `confirm` | `DecisionResult.Reject` with `AI_CONFIRMATION_UNSUPPORTED` | No mutation. |

## Reject

Provider `reject` maps to public HomeTusk `status=rejected`.

HomeTusk response limitations:

- public response carries `errorCode` and `reason`;
- provider `trace_id`, `schema_version`, `decision_version`, `payload.code`,
  `payload.reason`, `payload.ui_message`, and `details` remain auditable through
  `DecisionLog.rawDecisionPayload`.

## Confirm

Provider `confirm` is schema-supported but not HomeTusk-runtime-supported in
this initiative.

Required behavior:

- never execute `payload.proposed_actions`;
- return public `status=rejected`;
- use `errorCode=AI_CONFIRMATION_UNSUPPORTED`;
- preserve raw provider response in `DecisionLog.rawDecisionPayload`;
- defer `needs_confirmation` to a future HomeTusk contract initiative.

## Unknown or Invalid Provider Output

- Unknown action values fail schema validation and map to `AI_RESPONSE_INVALID`.
- Malformed or invalid provider responses reject safely.
- Malformed non-JSON provider bodies are stored in `DecisionLog.rawDecisionPayload`
  as a JSON wrapper with `raw_payload_format=invalid_json`, because the
  database column stores JSON.
- Provider timeout/unavailability uses the existing degraded/fallback policy.
