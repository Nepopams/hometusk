# HomeTusk Contract Gap Analysis

Status: Initial baseline, 2026-06-15

## Sources of Truth

- `docs/contracts/http/commands.openapi.yaml`
- `docs/contracts/external/ai-platform.decision.openapi.yaml`
- `docs/integration/ai-platform/v1/README.md`
- `docs/integration/ai-platform/v1/mapping/hometusk-to-upstream.md`
- `docs/integration/ai-platform/v1/mapping/hometusk-to-aiplatform.md`
- `docs/integration/ai-platform/v1/contracts/schemas/command.schema.json`
- `docs/integration/ai-platform/v1/contracts/schemas/decision.schema.json`
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandRequest.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionRequest.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionResponseMapper.java`
- `clients/mobile/src/api/types.ts`

## Gap Summary

| ID | Gap | Severity | Blocks |
| --- | --- | --- | --- |
| H-GAP-001 | No public `natural_command` request type/contract. | High | Mobile AI Command UX |
| H-GAP-002 | Public `CommandRequest.type` only supports `create_task` and `complete_task`. | High | natural multi-domain commands |
| H-GAP-003 | No HomeTusk response contracts for `confirm` or `answer`. | High | trustworthy AI UX |
| H-GAP-004 | HomeTusk wrapper schemas drift from runtime upstream envelope. | High | contract governance |
| H-GAP-005 | Legacy mapping doc still describes `/decision`. | Medium | integration clarity |
| H-GAP-006 | Runtime maps `add_shopping_item`, but public command type does not expose it. | Medium | shopping natural command |
| H-GAP-007 | Shopping item AI mapping omits `category`, `source`, and `linkedTaskId`. | Medium | shopping scenarios |
| H-GAP-008 | No explicit task-shopping linkage action contract. | High | linked planning |
| H-GAP-009 | No read-only answer payload contract. | Medium | status/query commands |
| H-GAP-010 | Mobile API types are command-shell-only. | Medium | mobile AI result cards |
| H-GAP-011 | No machine-readable HomeTusk golden scenarios. | High | Gate C/Domain Planner evidence |
| H-GAP-012 | Integration README says unsupported types degrade to Clarify/Reject, but mapper rejects unknown action. | Low/medium | exact UX expectation |

## Detailed Gaps

### H-GAP-001: Missing `natural_command`

Current public command request shape expects a structured command type and
payload. Natural language text is indirectly treated as a task title by web and
mobile clients.

Required decision:

- Add a future `natural_command` type, or add a separate endpoint/contract for
  natural commands.
- Ensure mobile and web still call HomeTusk, not AI Platform directly.

### H-GAP-002: Narrow Command Types

Runtime `CommandRequest.getCommandType()` supports only:

- `create_task`
- `complete_task`

This blocks first-class natural shopping commands and multi-action planning at
the public API boundary.

### H-GAP-003: Missing `confirm` and `answer`

Current response types:

- `executed`
- `executed_degraded`
- `scheduled`
- `needs_input`
- `rejected`

Required future response types:

- `needs_confirmation`
- `answered`

These must be contract-first before mobile UI work.

### H-GAP-004: Wrapper Schema Drift

HomeTusk integration wrapper schemas still show legacy camelCase shapes, while
runtime sends the upstream snake_case envelope through `AiDecisionRequest`.

Required:

- Decide whether wrapper schemas remain supported or are superseded.
- Update or deprecate stale wrapper schema docs through contract governance.

### H-GAP-005: Legacy Endpoint Mapping

`docs/integration/ai-platform/v1/mapping/hometusk-to-aiplatform.md` still
documents `/decision`, while current runtime and external contract use
`/v1/decide`.

Required:

- Mark the legacy mapping as superseded or update it to current UAT/default
  path.

### H-GAP-006: Runtime/Public Action Mismatch

HomeTusk can execute `add_shopping_item` from AI Platform decisions, but public
`CommandRequest.type` does not include `add_shopping_item`.

Required:

- Clarify whether shopping actions are only planner-proposed internal actions or
  also user-facing command types.

### H-GAP-007: Shopping Metadata Mapping

Runtime maps only:

- `name`
- `quantity`
- `unit`
- `list_id`

HomeTusk shopping supports:

- `category`
- `source`
- `linkedTaskId`

Required:

- Add provider/HomeTusk mapping for source/list/category only after taxonomy
  approval.

### H-GAP-008: Missing Linkage Action

HomeTusk can link shopping items to a task after execution if the same decision
created both. This is not an explicit action taxonomy or contract.

Required:

- Introduce explicit linkage planning or require confirmation for mixed plans.

### H-GAP-009: Missing Answer Payload

There is no structured response for read-only answers.

Required:

- Define answer payload with:
  - summary;
  - source read model;
  - referenced entities;
  - no mutation;
  - household scope.

### H-GAP-010: Mobile AI Types

Mobile `CommandRequest` and `CommandResponse` types mirror current structured
command shell. They do not model proposed actions, confirmation, answer cards,
or timeline.

Required:

- Do not build Mobile AI Command Center until the backend contract is accepted.

### H-GAP-011: Golden Scenarios

This research pack starts the scenario catalog, but executable fixtures do not
yet exist in HomeTusk.

Required:

- Add machine-readable fixtures before Domain Planner acceptance.

### H-GAP-012: Clarify vs Reject for Unsupported Types

Docs say unsupported upstream types degrade safely to Clarify or Reject.
Runtime unknown action currently returns `DecisionResult.Reject`.

Required:

- Make the expected UX decision explicit per unsupported type.

## Minimum Contract Work Before Implementation

Before HomeTusk implementation beyond audit:

1. Update or supersede stale AI Platform wrapper schemas/mapping docs.
2. Define accepted decision taxonomy in HomeTusk-owned contract docs.
3. Define first trust corridor action taxonomy.
4. Decide `natural_command` endpoint/type shape.
5. Add `confirm` and `answer` response contracts only when implementation is
   approved.
6. Add machine-readable golden scenarios.

## Compatibility Posture

The audit itself has no contract impact.

Future work will have contract impact if it adds:

- `natural_command`;
- `needs_confirmation`;
- `answered`;
- new AI Platform actions;
- new request/response fields for proposed actions, answers, or confirmation.
