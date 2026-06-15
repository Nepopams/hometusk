# Decision and Action Taxonomy Accepted v0

Status: Accepted for narrow Domain Planner v1 artifact gate

Date: 2026-06-15

Recommendation: **LIMITED-GO**

## Sources of Truth

- `docs/research/ai-command-capabilities/decision-taxonomy-v0.md`
- `docs/research/ai-command-capabilities/action-taxonomy-v0.md`
- `docs/research/ai-command-capabilities/target-architecture-v0.md`
- `docs/research/ai-command-capabilities/recommendation.md`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/contracts/external/ai-platform.decision.openapi.yaml`
- `docs/integration/ai-platform/v1/upstream/contracts/schemas/decision.schema.json`
- `docs/planning/initiatives/INIT-2026Q3-ai-command-artifact-gate.md`

## Canonical Decision Outcomes

| Outcome | Meaning | Current HomeTusk status | Current HomeTusk support | Current AI Platform mapping |
| --- | --- | --- | --- | --- |
| `execute` | Safe to execute validated domain action now. | `executed`, `executed_degraded`, partial `scheduled` | Partial | `status=ok`, `action=start_job`, `propose_create_task`, or `propose_add_shopping_item` mapped through HomeTusk. |
| `clarify` | More input is required before decision or execution. | `needs_input` | Supported | `status=clarify`, `action=clarify`. |
| `confirm` | Plan is understood but needs explicit user approval before mutation. | none | Missing | Not first-class in current upstream schema. |
| `reject` | Request must not be handled or cannot be handled safely. | `rejected` | Supported | Current upstream has `status=error`; docs mention `reject`, but action enum does not include it. |
| `answer` | Read-only grounded answer with no mutation. | none | Missing | Not first-class in current upstream schema. |

## Mapping to Current HomeTusk

| HomeTusk runtime shape | Accepted canonical outcome | Notes |
| --- | --- | --- |
| `DecisionResult.StartJob` | `execute` | Valid only after schema validation, accepted action mapping, guardrails, and domain validation. |
| `DecisionResult.Clarify` | `clarify` | Preferred over guessing when context is missing or ambiguous. |
| `DecisionResult.Reject` | `reject` | Required for unsupported, unsafe, unverifiable, or cross-household cases. |
| `CommandResponse.scheduled` | `execute_later` under `execute` family | Scheduling is command lifecycle, not planner permission. Natural scheduling remains outside auto-execute until policy is accepted. |
| `CommandDegradedResponse` | `execute` with degraded source | Must remain explicit in audit and user-visible state. |

## Mapping to Current AI Platform

| AI Platform status/action | Accepted canonical outcome | Mapping quality | Gate note |
| --- | --- | --- | --- |
| `status=ok`, `action=start_job` | `execute` | Good only when proposed actions are supported and allowed. | HomeTusk must validate every nested action. |
| `action=propose_create_task` | `execute` in v0, future `confirm` for risky cases | Partial | Safe only for clear task creation in the narrow corridor. |
| `action=propose_add_shopping_item` | `execute` in v0 | Partial/good | Multi-item output is represented as repeated proposed actions. |
| `status=clarify`, `action=clarify` | `clarify` | Good | Use when missing fields or ambiguity blocks safe execution. |
| `status=error` | `reject` | Acceptable but semantically rough | Provider should add clean reject mapping in future planner work. |

## Execution Permission Rule

Confidence is not permission.

Execution is allowed only when all conditions hold:

1. The provider response is schema-valid.
2. The mapped canonical outcome is `execute`.
3. Every proposed action is in the accepted action taxonomy.
4. Every proposed action is in the current trust corridor.
5. HomeTusk guardrails pass or deterministically convert the outcome to
   `clarify` or `reject`.
6. Domain services validate household membership, entity scope, future deadline
   rules, permissions, and ownership.
7. `DecisionLog` stores decision source, confidence, provider ids, trace ids,
   raw provider payload, validation/guardrail evidence, and degraded state where
   applicable.

## Accepted v0 Trust Corridor

### Auto-execute

- `create_task`
- `add_shopping_items`

Auto-execute requires clear item/task boundaries and grounded household context.

### Clarify

- Ambiguous task title.
- Ambiguous shopping list/source/default list.
- Ambiguous zone, member, or household entity.
- Ambiguous or unsupported date/time normalization.
- Missing default shopping list.
- Incomplete household context.

### Confirm

- Inferred non-requester assignment.
- Task plus shopping linkage.
- Reschedule.
- Batch planning.
- Broad workload redistribution.
- Any domain action outside the narrow v0 auto-execute corridor.

### Reject

- Unsafe or impossible request.
- Unsupported action.
- Cross-household reference.
- Unverifiable entity.
- Direct AI execution bypassing HomeTusk.
- Bulk assignment that violates safety/workload policy.

### Answer

- Design-only until a read-only answer contract exists.
- Must not mutate.
- Must cite or structure the HomeTusk read model used as source.

## Accepted Action Taxonomy

| Action | v0 status | Auto-execute | Required context | HomeTusk validation |
| --- | --- | --- | --- | --- |
| `create_task` | Accepted narrow action | Yes, when title and optional attributes are grounded. | household, requester, optional assignee, zone, deadline | Schema, membership, zone scope, future deadline, permissions, workload guardrail. |
| `add_shopping_items` | Accepted narrow action | Yes, when item boundaries and target list/default are grounded. | household, default or named list, item names | Item validation, list scope, optional task/list linkage blocked unless explicit and confirmed. |
| `complete_task` | Existing structured command only | No for natural text v0 | exact task id | Household task scope, current status, permissions. |
| `link_task_shopping` | Future action | No | exact task and item ids or accepted same-plan references | Same-household linkage and confirmation. |
| `reschedule_task` | Future action | No | exact task, target date/time, timezone | Task scope, future time, confirmation on inferred windows. |
| `answer_status` | Future read-only action | No mutation | household read model | Authz, no mutation, grounded answer payload. |
| `assign_or_reassign_task` | Future/partial | Confirm if inferred or non-requester | member, task or new task, policy | Membership, permission, workload/fairness rules. |
| `create_multi_action_plan` | Future planning action | No | full plan context | Plan-level limits, per-action validation, confirmation. |

## Current Support Status

| Capability | Current status | Accepted gate posture |
| --- | --- | --- |
| Task creation | Supported, narrow | Eligible for auto-execute when clear and valid. |
| Multi-item shopping addition | Partially supported via provider proposed actions and HomeTusk internal action execution | Eligible for auto-execute when item boundaries and list are clear. |
| Named non-requester assignment | Partially represented | Confirm before mutation. |
| Task-shopping linkage | Implicit/partial | Confirm; do not auto-link from vague language. |
| Reschedule | Missing | Clarify or confirm only after future contract/action support. |
| Status answer | Missing | Blocked until `answer`/`answered` contract exists. |
| Confirmation | Missing | Required before risky broad UX. |

## Non-Negotiable Rules

- HomeTusk is source of truth and execution authority.
- AI Platform proposes decisions and actions only.
- Unsupported provider output degrades to `clarify` or `reject`.
- Schema validity is necessary but not sufficient.
- Business invariants remain in code, not prompts.
- Mobile/web do not call AI Platform directly.
- Degraded deterministic behavior must remain available.
