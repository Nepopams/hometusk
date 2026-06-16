# Guardrails Policy v0

Status: Draft only

## Trust Corridor

| Request class | Draft outcome | Rationale |
| --- | --- | --- |
| Clear create task for requester/default assignee | auto-execute candidate | Existing domain rules can validate title, household, assignee, zone, deadline |
| Clear multi-item shopping addition | auto-execute candidate | Provider can emit repeated singular `propose_add_shopping_item`; HomeTusk validates each item |
| Missing/ambiguous entity | clarify | Do not guess household state |
| Missing date context | clarify | `referenceInstant`, `timezone`, and `locale` are required |
| Non-requester assignment | confirmation required | Affects another member's workload/social contract |
| Task-shopping linkage | confirmation required | Mixed plan can create multiple linked mutations |
| Reschedule | confirmation required | Changes existing commitment |
| Completion of another user's task | confirmation required or clarify | Risk of wrong-object mutation |
| Batch planning | confirmation required or reject | Broad workload effects |
| Broad workload redistribution | reject by default | Outside narrow v0 corridor |
| Unsupported action | reject | No prompt-only workaround |
| Cross-household reference | reject | Household boundary invariant |
| Unverifiable entity | clarify or reject | No silent entity creation/matching |
| Payment/device/external side effect | reject | Outside HomeTusk command authority |
| Read-only answer/status query | blocked | Separate `answered` contract required |

## Required Example Coverage

| User text | Draft outcome | Notes |
| --- | --- | --- |
| `я помыл посуду закрой` | clarify or confirmation | Natural completion is blocked until exact task matching and approval policy exist |
| `я вынес мусор вместо Пети` | confirmation | Completion for another member affects audit/workload and must not auto-execute |
| `в среду надо встретить газовщика` | execute or clarify | Requires locale/timezone/reference instant to normalize Wednesday |
| `к ужину купи молоко и курицу` | execute or clarify | Shopping addition may execute if list/defaults/date phrase are grounded |
| `назначь всем по 20 задач сегодня ночью` | reject | Unsafe/broad workload redistribution and possible quiet-hours issue |

## Date / Time Policy

For date expressions, all are mandatory:

- `payload.referenceInstant`
- `payload.timezone`
- `payload.locale`

If any are missing, invalid, or insufficient for normalization:

```text
clarify, do not guess
```

HomeTusk must store the normalized instant/date used for execution or proposal
audit. Provider confidence is not permission to infer missing clock context.

## Guardrail Interaction

Future implementation should distinguish:

- pre-execution guardrails for auto-execute;
- pre-confirmation guardrails for proposal creation;
- post-approval revalidation guardrails before execution.

Confirmation is not a bypass. Approved proposals still must pass domain
invariants at execution time.

## No Prompt-Only Rules

Business invariants must be enforced in HomeTusk code:

- assignee belongs to household;
- zone exists in household;
- deadline is in the future or absent;
- initiator has permission;
- linked shopping items/tasks belong to the same household;
- confirmation actor is authorized.
