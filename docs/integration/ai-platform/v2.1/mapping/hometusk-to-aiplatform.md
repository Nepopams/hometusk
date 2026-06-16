# HomeTusk to AI Platform v2.1 Mapping

## Endpoint

HomeTusk calls the configured decision endpoint. The default UAT/runtime path is:

```text
POST /v1/decide
```

The path is configured by `aiplatform.decision-path`.

## Request Envelope

HomeTusk sends the upstream snake_case command envelope:

| HomeTusk source | AI Platform field |
| --- | --- |
| `DecisionContext.commandId` | `command_id` |
| `DecisionContext.requesterId` | `user_id` |
| Current instant | `timestamp` |
| Extracted command text | `text` |
| Adapter policy | `capabilities` |
| Household snapshot | `context.household` |
| Deterministic defaults | `context.defaults` |

## Capabilities

Default HomeTusk capabilities:

```text
start_job
propose_create_task
propose_add_shopping_item
clarify
reject
```

HomeTusk does not advertise `confirm` until a HomeTusk
`needs_confirmation` contract exists.

## Context Minimization

HomeTusk sends only the minimum household context needed by the provider:

- member id/display name/role/workload score when available;
- zone id/name;
- shopping list id/name;
- default assignee id;
- default list id.

HomeTusk must not send secrets, raw auth data, emails, or direct mobile/web AI
credentials to AI Platform.
