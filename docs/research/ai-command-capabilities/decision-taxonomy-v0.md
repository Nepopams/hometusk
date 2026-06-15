# Decision Taxonomy v0

Status: Proposed

## Sources of Truth

- `docs/planning/initiatives/INIT-2026Q3-ai-command-capability-audit.md`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/DecisionResult.java`
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandResponse.java`
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandNeedsInputResponse.java`
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandRejectedResponse.java`
- `docs/contracts/http/commands.openapi.yaml`
- `../../VR_AI_Platform/contracts/schemas/decision.schema.json`

## Canonical Outcomes

| Canonical outcome | Meaning | User-visible HomeTusk status today | Current support | Notes |
| --- | --- | --- | --- | --- |
| `execute` | Safe to execute one or more validated domain actions now. | `executed`, `executed_degraded` | Partial | Supported through `DecisionResult.StartJob`; degraded source uses fallback. |
| `clarify` | More information is required before a decision can be made or executed. | `needs_input` | Supported | Supported by AI Platform `clarify` and HomeTusk guardrails. |
| `confirm` | Proposed action is understood but requires explicit user confirmation before mutation. | none | Missing | Needed for risky or broad mutations. |
| `reject` | Request cannot be safely or meaningfully handled. | `rejected` | Supported | HomeTusk supports reject; provider currently represents provider-side errors as `status=error`. |
| `answer` | Request is informational and should return a grounded status/explanation without mutation. | none | Missing | Needed for household status/query style commands. |

## Mapping to Current HomeTusk

| Current HomeTusk internal type | Canonical outcome | Mapping quality |
| --- | --- | --- |
| `DecisionResult.StartJob` | `execute` | Good for narrow trusted actions. |
| `DecisionResult.Clarify` | `clarify` | Good. |
| `DecisionResult.Reject` | `reject` | Good. |
| `CommandResponse.scheduled` | `execute` later | Partial; schedule acceptance is command lifecycle, not planner outcome. |
| `CommandDegradedResponse` | `execute` with degraded mode | Good for deterministic fallback, but must be explicit in audit. |

## Mapping to Current AI Platform

| Provider status/action | Canonical outcome | Mapping quality |
| --- | --- | --- |
| `status=ok`, `action=start_job` | `execute` | Good when proposed actions are present and supported. |
| `action=propose_create_task` | `execute` or future `confirm` | Currently mapped to immediate `execute`; future planner may need confirmation. |
| `action=propose_add_shopping_item` | `execute` | Good for additive shopping item action. |
| `status=clarify`, `action=clarify` | `clarify` | Good. |
| `status=error` | `reject` | Acceptable as provider error, but not a clean product taxonomy. |

## Execution Permission Rule

Confidence is not execution permission.

Execution is allowed only when all are true:

1. The decision response is schema-valid.
2. The canonical outcome is `execute`.
3. Every proposed action is in the accepted action taxonomy.
4. The action is allowed in the current trust corridor.
5. HomeTusk guardrails pass or modify into a safe executable action.
6. Domain services validate household membership, entity scope, deadlines, and
   task/list ownership.
7. DecisionLog records source, confidence, raw provider payload, and guardrail
   outcome.

## Confirm Policy

`confirm` should be introduced before broad mutations or high-ambiguity plans.

Initial confirm triggers:

- destructive or hard-to-reverse actions;
- batch actions above one task plus one shopping item;
- assignment to someone other than requester when inferred by AI;
- deadline/time normalization with high ambiguity;
- reschedule operations;
- workload/fairness decisions that materially rebalance assignments;
- any action outside the approved narrow corridor.

## Answer Policy

`answer` should not mutate domain state.

Initial answer examples:

- "what is due today?"
- "who has the most open tasks?"
- "what should we do before guests?"

`answer` requires grounded read models and explicit cited context in the response.
It should never invent household state that was not in HomeTusk context.

## Reject Policy

Reject when:

- request is unsafe, impossible, abusive, or outside domain;
- requested action would violate household boundaries;
- provider emits unsupported action type;
- schema validation fails;
- HomeTusk cannot verify required context;
- user asks for direct AI execution outside HomeTusk.

## Open Gaps

| Gap | Required before |
| --- | --- |
| No `confirm` in provider or HomeTusk response contract. | Risky mutations and Mobile AI UX. |
| No `answer` in provider or HomeTusk response contract. | Status/query commands. |
| Provider `reject` is not first-class action. | Clean cross-repo taxonomy. |
| No accepted mapping for `scheduled` versus planner outcome. | Natural scheduling commands. |
| No canonical audit payload for alternatives. | Explainability and replay. |
