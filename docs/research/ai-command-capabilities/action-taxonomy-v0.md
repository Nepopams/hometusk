# Action Taxonomy v0

Status: Proposed

## Sources of Truth

- `services/backend/src/main/java/com/hometusk/commands/pipeline/ActionExecutor.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/guardrails/**`
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandRequest.java`
- `docs/contracts/http/commands.openapi.yaml`
- `../../VR_AI_Platform/contracts/schemas/decision.schema.json`
- `../../VR_AI_Platform/routers/v2.py`
- `../../VR_AI_Platform/routers/partial_trust_acceptance.py`

## Proposed Actions

| Action | Current HomeTusk support | Current AI Platform support | Mutation class | Reversibility | Auto-execute v0 | Required context | HomeTusk validation |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `create_task` | Yes | Yes via `propose_create_task` | Additive | Easy/moderate | Yes in narrow corridor | household, requester, optional assignee, zone, deadline | schema, assignee membership, zone household scope, future deadline, workload guardrail |
| `complete_task` | Yes when task id is known | No provider action | Mutating | Moderate | No for natural text until task matching exists | household, exact task id, task status | task belongs to household, not done, not cancelled |
| `add_shopping_items` | Partial as repeated singular `add_shopping_item` | Yes for one or more proposed shopping items in V2 | Additive | Easy | Yes in narrow corridor | household, shopping list/default list, item names | item name/category/source validation, list/task household scope through service |
| `link_task_shopping` | Partial implicit linkage when task and items are created in same decision | Not proven as explicit plan | Mutating linkage | Easy | No | task id, item ids, household | same-household task and shopping items |
| `reschedule_task` | No command action | No | Mutating | Moderate | No | exact task, new due/schedule time, timezone | task scope, future time, permissions |
| `answer_status` | No command action | No | Read-only | N/A | No until answer contract exists | household read models | no mutation, grounded read authorization |
| `assign_or_reassign_task` | Partial through `create_task.assigneeId` only | Default assignee only | Mutating | Moderate | Confirm if inferred | members, workload, consent/policy | assignee membership, workload/fairness policy |
| `create_multi_action_plan` | Partial action list execution | V2 can emit multiple shopping actions | Mixed | Mixed | Confirm or clarify | all entities in plan | per-action plus plan-level limits |

## Accepted v0 Trust Corridor

The first trusted corridor should be intentionally narrow:

```text
execute:
  - create_task
  - add_shopping_items

clarify:
  - ambiguous task title
  - missing shopping item
  - missing or ambiguous list/source when needed
  - ambiguous assignee/zone/deadline

confirm:
  - multi-action task + shopping linkage
  - inferred assignment to non-requester
  - broad/batch planning
  - reschedule

reject:
  - unsafe or impossible actions
  - unsupported action types
  - cross-household or unverifiable references

answer:
  - blocked until read-only answer contract exists
```

## Field-Level Requirements

### `create_task`

Required:

- `title`

Optional:

- `description`
- `assigneeId`
- `zoneId`
- `deadline`

Rules:

- If assignee is absent, HomeTusk may default to requester or zone owner through
  deterministic rules.
- If AI infers a non-requester assignee from natural text, require confirmation
  until membership/fairness semantics are accepted.
- Deadline must be future or absent.

### `add_shopping_items`

Required per item:

- `name`

Optional:

- `quantity`
- `unit`
- `category`
- `source`
- `listId`
- `linkedTaskId`

Rules:

- The current provider schema supports `name`, `quantity`, `unit`, and
  `list_id`; it does not yet support HomeTusk `category`, `source`, or
  `linkedTaskId`.
- Multi-item extraction is acceptable only when each item is a separate
  proposed action and all items pass guardrails.

### `link_task_shopping`

Required:

- exact task id or a task created in the same accepted plan;
- item ids or items created in the same accepted plan.

Rules:

- Do not auto-link from vague language unless the scenario catalog accepts the
  interpretation.
- If both task and shopping items are created by one decision, explicit linkage
  should be present in the planned action or target contract, not only inferred
  post-execution.

### `reschedule_task`

Required:

- exact task reference;
- normalized target date/time or schedule window;
- timezone.

Rules:

- Clarify if multiple tasks match.
- Confirm if the new date/time was inferred from vague language such as
  "weekend" or "evening".

### `answer_status`

Required:

- read model query type;
- household scope;
- fields allowed for response.

Rules:

- Must not mutate.
- Must cite or structure the data source in the response.
- Must not reveal cross-household information.

## Out-of-Scope Actions for v0

- generic assistant chat;
- recurring routine generation from natural language;
- destructive task deletion;
- marketplace purchasing;
- autonomous assignment optimization;
- calendar integration actions;
- cross-household coordination.
