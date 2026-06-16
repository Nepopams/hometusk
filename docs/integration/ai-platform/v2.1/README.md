# AI Platform Integration Package v2.1

HomeTusk intake package for AI Platform provider contract `2.1.0`.

## Source Metadata

| Field | Value |
| --- | --- |
| Provider repository | `C:/Users/user/Documents/projects/VR_AI_Platform` |
| Provider revision inspected | `5c2eb8c5fbdd75e5bc8d0a9d56333ee756354bb1` |
| Provider contract version | `2.1.0` |
| Provider schemas | `contracts/schemas/command.schema.json`, `contracts/schemas/decision.schema.json` |
| Provider handoff | `docs/planning/workpacks/ST-056/hometusk-handoff.md` |

## Structure

```text
v2.1/
  README.md
  AGENTS.md
  upstream/
    README.md
    contracts/
      VERSION
      schemas/
        command.schema.json
        decision.schema.json
  mapping/
    hometusk-to-aiplatform.md
    aiplatform-to-hometusk.md
  examples/
    decision-execute-create-task.json
    decision-execute-shopping-items.json
    decision-clarify.json
    decision-reject.json
    decision-confirm.json
    decision-invalid-unknown-action.json
  compatibility.md
```

## Supported Provider Outcomes

| Provider outcome | HomeTusk mapping | Execution |
| --- | --- | --- |
| `execute` / `start_job` | `DecisionResult.StartJob` | Yes, after HomeTusk validation and guardrails. |
| `execute` / `propose_create_task` | Internal `create_task` action | Yes, after HomeTusk validation and guardrails. |
| `execute` / `propose_add_shopping_item` | Internal `add_shopping_item` action | Yes, after HomeTusk validation and guardrails. |
| `clarify` | `DecisionResult.Clarify` / `needs_input` | No mutation. |
| `reject` | `DecisionResult.Reject` / `rejected` | No mutation. |
| `confirm` | `DecisionResult.Reject` / `AI_CONFIRMATION_UNSUPPORTED` | No mutation. |

## Capability Policy

HomeTusk sends by default:

- `start_job`
- `propose_create_task`
- `propose_add_shopping_item`
- `clarify`
- `reject`

HomeTusk does not send by default:

- `confirm`

Provider `confirm` may still arrive due to provider-side policy or drift. In
that case HomeTusk rejects safely and stores the raw provider response in
`DecisionLog.rawDecisionPayload`.
