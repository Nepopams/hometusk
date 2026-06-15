# Natural Command Contract v0 Draft

Status: Draft only

Date: 2026-06-15

This document is not an OpenAPI change, not a JSON Schema change, and not
runtime approval. Future implementation requires contract governance, Gate C,
and a separate HomeTusk workpack.

## Sources of Truth

- `docs/contracts/http/commands.openapi.yaml`
- `docs/contracts/external/ai-platform.decision.openapi.yaml`
- `docs/integration/ai-platform/v1/upstream/contracts/schemas/command.schema.json`
- `docs/integration/ai-platform/v1/upstream/contracts/schemas/decision.schema.json`
- `docs/research/ai-command-capabilities/target-architecture-v0.md`
- `docs/research/ai-command-capabilities/domain-planner-v1-gate/decision-action-taxonomy-accepted-v0.md`

## Explicit Non-Approval

- Do not add `natural_command` to the public API from this draft.
- Do not add `needs_confirmation` or `answered` response variants from this
  draft.
- Do not modify OpenAPI, wrapper schemas, upstream snapshots, backend DTOs, or
  mobile API types in this initiative.
- Mobile and web must still call HomeTusk, not AI Platform.

## Future HomeTusk-Facing Request Shape

The preferred direction is a future command submission variant under HomeTusk
control, either as a new `type: natural_command` in `POST /api/v1/commands` or
as a separate contract-governed endpoint. That decision is intentionally left
open for future contract governance.

Draft shape:

```yaml
type: natural_command
householdId: string
text: string
source:
  inputMode: typed | voice_transcript | imported
  asrTraceId: string | null
locale: string
timezone: string
referenceInstant: string
clientContext:
  platform: web | ios | android
  appVersion: string | null
  commandSurface: string | null
idempotency:
  idempotencyKey: string
audit:
  correlationId: string
```

Required future rules:

- `text` is user-reviewed text, not raw audio.
- `locale`, `timezone`, and `referenceInstant` are required for date/time
  interpretation.
- `source.inputMode` distinguishes typed commands from voice transcript drafts.
- `asrTraceId` may link to ASR audit, but ASR never grants execution
  permission.
- `Idempotency-Key` and `X-Correlation-ID` behavior remains compatible with the
  existing Commands API.

## Future Provider-Facing Decision Request Direction

HomeTusk should continue adapting its public request into a privacy-minimized
provider request. Direction:

```yaml
command_id: string
user_id: string
timestamp: string
text: string
locale: string
timezone: string
reference_instant: string
capabilities:
  - create_task
  - add_shopping_items
  - clarify
  - reject
context:
  household:
    household_id: string
    members:
      - user_id: string
        display_name: string
        role: string | null
        workload_score: number | null
    zones:
      - zone_id: string
        name: string
    shopping_lists:
      - list_id: string
        name: string
  defaults:
    default_assignee_id: string | null
    default_list_id: string | null
  policies:
    max_open_tasks_per_user: integer | null
    quiet_hours: string | null
audit:
  correlation_id: string
  prompt_version: string | null
  planner_version_requested: string | null
```

## Future Provider-Facing Decision Shape

Direction:

```yaml
decision_id: string
command_id: string
decision_outcome: execute | clarify | confirm | reject | answer
confidence: number
actions:
  - action_type: create_task | add_shopping_items | complete_task | link_task_shopping | reschedule_task | answer_status
    action_id: string
    parameters: object
clarify:
  question: string
  missing_fields: string[]
  options: object | null
confirm:
  summary: string
  reasons: string[]
  proposed_actions: object[]
answer:
  summary: string
  referenced_entities: object[]
  source_read_model: string
reject:
  reason: string
  error_code: string
audit:
  trace_id: string
  schema_version: string
  decision_version: string
  planner_version: string
  prompt_version: string | null
  alternatives: object[]
created_at: string
```

## Context Snapshot Rules

HomeTusk may send only the minimum context needed for the allowed capabilities:

- household id;
- requester id;
- member ids, display names, roles, and approved workload score;
- zones;
- shopping lists;
- deterministic defaults;
- bounded policy facts needed for safety.

HomeTusk must not send:

- raw audio;
- auth tokens;
- invite tokens;
- device push tokens;
- emails unless separately approved;
- private comments;
- unrelated historical task data;
- cross-household data.

## Allowed Capabilities for Narrow v0

Initial provider request capability allowlist:

- `create_task`
- `add_shopping_items`
- `clarify`
- `reject`

Optional, non-executing provider capability:

- `confirm`

Blocked until future contract gate:

- `answer_status`
- `complete_task` from natural text
- `reschedule_task`
- `link_task_shopping`
- broad multi-action planning

## Compatibility With Existing Commands

### `create_task`

The draft must remain compatible with the existing command model:

- clear task creation can map to existing task creation behavior;
- optional `dueDate`, `assigneeId`, and `zoneId` still require domain
  validation;
- inferred non-requester assignment requires confirmation until policy changes.

### `complete_task`

The existing structured command requires an exact task id.

Natural-text completion remains blocked until:

- task matching is contract-governed;
- ambiguous matches clarify;
- exact-match rules are covered by golden scenarios;
- wrong-object mutation and undo/recovery policy are accepted.

## Audit and Tracing Requirements

Future implementation must preserve:

- command id;
- correlation id;
- provider decision id;
- provider trace id;
- schema version;
- decision version;
- planner version;
- prompt version where applicable;
- raw provider payload in `DecisionLog.rawDecisionPayload`;
- guardrail outcome;
- degraded reason when degraded behavior occurs.

## Contract Impact Gate Required Later

This draft becomes implementation-ready only after a future contract gate decides:

- endpoint vs `type: natural_command`;
- accepted request/response JSON Schemas;
- `needs_confirmation` response shape;
- `answered` response shape;
- provider outcome mapping;
- mobile API type changes;
- OpenAPI updates and contract index updates.
