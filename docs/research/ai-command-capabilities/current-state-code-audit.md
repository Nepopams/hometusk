# Current-State Code Audit

Status: Initial baseline, 2026-06-15

## Sources of Truth

- `docs/planning/initiatives/INIT-2026Q3-ai-command-capability-audit.md`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/contracts/external/ai-platform.decision.openapi.yaml`
- `docs/integration/ai-platform/v1/README.md`
- `docs/integration/ai-platform/v1/mapping/hometusk-to-upstream.md`
- `services/backend/src/main/java/com/hometusk/commands/**`
- `clients/mobile/src/features/command/**`
- `../../VR_AI_Platform/contracts/schemas/**`
- `../../VR_AI_Platform/graphs/core_graph.py`
- `../../VR_AI_Platform/routers/v2.py`
- `../../VR_AI_Platform/routers/assist/**`
- `../../VR_AI_Platform/routers/partial_trust_*.py`
- `../../VR_AI_Platform/agent_registry/**`

## Executive Finding

HomeTusk has a strong command execution shell and audit trail, but the current
AI-command capability is not yet a production-grade natural-command planner.

The current stack can safely support a narrow corridor:

- create a task from structured or simple text-derived title;
- complete a task when the client resolves the task id;
- add shopping item actions when AI Platform returns supported proposed actions;
- ask for clarification or reject unsupported/invalid decisions;
- preserve `DecisionLog` traceability and raw AI response payloads.

It does not yet prove readiness for:

- mixed task plus shopping planning from one natural command;
- rescheduling existing tasks;
- status-answering semantics;
- confirmation semantics before risky mutations;
- robust date/time normalization;
- mobile structured AI command UX.

## HomeTusk Backend

### API Surface

Evidence:

- `docs/contracts/http/commands.openapi.yaml`
- `services/backend/src/main/java/com/hometusk/commands/api/CommandController.java`
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandRequest.java`

Current behavior:

- Public endpoint: `POST /api/v1/commands`.
- Continue endpoint: `POST /api/v1/commands/{commandId}/continue`.
- Supported public command types in runtime DTO: `create_task`, `complete_task`.
- Optional command-level fields exist for `create_task`: `dueDate`, `assigneeId`,
  `zoneId`, `scheduleAt`.
- Voice-originated commands can use `source=voice` and `asrTraceId`, but ASR is
  only capture/edit/send, not execution.
- Idempotency is handled through `Idempotency-Key`.
- `X-Correlation-ID` is generated or accepted from the caller.

Assessment:

- The API is still structured-command-first, not natural-command-first.
- The current public request shape has no `natural_command` type or raw text
  field dedicated to planner input.
- `complete_task` currently requires a resolved task id in payload, so natural
  matching such as "mark kitchen done" is client or future planner work.

### Command Pipeline

Evidence:

- `services/backend/src/main/java/com/hometusk/commands/service/CommandService.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/SchemaValidator.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/BusinessValidator.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/ContextBuilder.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/guardrails/GuardrailsOrchestrator.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/ActionExecutor.java`

Pipeline shape:

1. Resolve household and requester.
2. Create `Command` with status `RECEIVED`.
3. Validate schema and business invariants.
4. Build household context for AI and guardrails.
5. Select decision provider.
6. Evaluate guardrails.
7. Execute proposed actions.
8. Write `DecisionLog`.
9. Return controlled command status.

Strengths:

- Domain invariants are enforced in code through validators and guardrails.
- Guardrails fail safe when household context is incomplete.
- Execution stays in HomeTusk domain services.
- Scheduled commands exist for structured `scheduleAt`.
- Voice metrics are recorded for `source=voice`.

Gaps:

- Only two `CommandType` values exist: `CREATE_TASK`, `COMPLETE_TASK`.
- `ActionExecutor.executeAction` supports `create_task`, `complete_task`, and
  singular `add_shopping_item`, but public `CommandRequest` does not expose
  `add_shopping_item` as a command type.
- There is no internal action for `reschedule_task`, `answer_status`,
  `confirm_action`, or explicit `link_task_shopping`.
- Task-shopping linkage is implicit only when a single decision creates both a
  task and shopping items; the current provider planner does not prove that
  combined plan.

### Decision Provider and Adapter

Evidence:

- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/DecisionResult.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/DecisionProviderSelector.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/ManualDecisionProvider.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/AiPlatformDecisionProvider.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionRequest.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionResponseMapper.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiResponseSchemaValidator.java`

Current decision variants:

- `DecisionResult.StartJob`
- `DecisionResult.Clarify`
- `DecisionResult.Reject`

Current upstream actions accepted by mapper:

- `start_job`
- `propose_create_task`
- `propose_add_shopping_item`
- `clarify`

Safe behavior:

- AI response is schema-validated before mapping.
- Invalid AI response maps to rejection with `AI_RESPONSE_INVALID`.
- Unknown upstream action maps to rejection with `UNKNOWN_DECISION_ACTION`.
- AI Platform failure falls back to manual provider when configured.

Gaps:

- No `Confirm` variant.
- No `Answer` variant.
- `reject` is documented in integration README, but the current upstream schema
  and mapper use `status=error` rather than an action enum value `reject`.
- `AiDecisionRequest.DEFAULT_CAPABILITIES` includes `start_job`,
  `propose_create_task`, `propose_add_shopping_item`, `clarify`, but not
  `complete_task`, `reschedule_task`, `answer_status`, or `confirm`.
- Mapping for shopping omits category/source fields even though HomeTusk
  shopping items support them.

### DecisionLog and Traceability

Evidence:

- `services/backend/src/main/java/com/hometusk/commands/domain/DecisionLog.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/DecisionLogWriter.java`

Strengths:

- `DecisionLog` stores intent, context snapshot, decision, source, confidence,
  validation flags, external decision id, and raw AI decision payload.
- Validation failures are logged as decision logs.
- Guardrail clarify/reject outcomes are written into audit records.

Gaps:

- No accepted canonical taxonomy for what must be logged for `confirm` or
  `answer`.
- Alternatives are supported structurally but not currently populated by the
  observed provider path.
- The context snapshot intentionally remains minimal, so replay/eval docs must
  define which external context is required to reproduce a decision.

## Mobile and Web Command UX

Evidence:

- `clients/mobile/src/features/command/CommandSurface.tsx`
- `clients/mobile/src/features/command/CommandComposer.tsx`
- `clients/mobile/src/features/command/commandRequestBuilder.ts`
- `clients/mobile/src/api/types.ts`
- `clients/web/src/routes/Commands.tsx`

Current mobile behavior:

- The mobile command tab accepts text.
- It builds either `create_task` with title or `complete_task` after local task
  matching by id prefix/title substring.
- It posts to `/commands` through the HomeTusk API client.
- It displays `executed`, `executed_degraded`, `scheduled`, `needs_input`, and
  `rejected`.

Current web behavior:

- Web voice command flow records audio, sends ASR, places transcript into an
  editable text field, then explicitly posts `create_task`.
- It supports structured attributes such as due date, assignee, zone, and
  schedule date.

Assessment:

- Mobile is a deterministic command shell, not a planner UI.
- Web voice validates the capture/edit/send pattern, but the command remains a
  structured `create_task`.
- Neither client has confirmation cards, answer cards, proposed action review,
  multi-action timeline, or rich structured result cards for planned actions.

## AI Platform Provider

Evidence:

- `../../VR_AI_Platform/app/routes/decide.py`
- `../../VR_AI_Platform/app/services/decision_service.py`
- `../../VR_AI_Platform/app/models/api_models.py`
- `../../VR_AI_Platform/contracts/schemas/command.schema.json`
- `../../VR_AI_Platform/contracts/schemas/decision.schema.json`
- `../../VR_AI_Platform/graphs/core_graph.py`
- `../../VR_AI_Platform/routers/factory.py`
- `../../VR_AI_Platform/routers/v2.py`
- `../../VR_AI_Platform/docs/adr/ADR-000-ai-platform-intent-decision-engine.md`
- `../../VR_AI_Platform/docs/adr/ADR-004-partial-trust-corridor.md`
- `../../VR_AI_Platform/docs/adr/ADR-005-internal-agent-contract-v0.md`
- `../../VR_AI_Platform/docs/adr/ADR-008-asr-cloudru-whisper-mvp.md`

Current provider contract:

- API route is mounted as `/v1/decide` by app prefix and route `/decide`.
- Input `CommandDTO` requires `command_id`, `user_id`, `timestamp`, `text`,
  `capabilities`, and `context`.
- Capabilities currently include `start_job`, `propose_create_task`,
  `propose_add_shopping_item`, and `clarify`.
- Decision response uses `status` values `ok`, `clarify`, `error`.
- Decision response actions include `start_job`, `propose_create_task`,
  `propose_add_shopping_item`, and `clarify`.

Current provider runtime:

- Default router strategy is `v1` unless `DECISION_ROUTER_STRATEGY=v2`.
- V1 uses deterministic keyword/regex-style logic in `graphs/core_graph.py`.
- V2 has normalize, assist hints, planner, validator, shadow agents, and partial
  trust hooks.
- Assist mode is feature-flagged and disabled by default.
- Partial trust is limited to `add_shopping_item`, feature-flagged, sampled,
  and guarded by strict acceptance rules.
- Agent registry entries are internal-only and disabled by default.
- ASR endpoint is separate and must not call `/v1/decide` automatically.

Provider strengths:

- Contract validation exists at input and output boundaries.
- Deterministic fallback baseline is explicit.
- V2 supports multi-item shopping extraction in tests.
- Partial-trust corridor has risk logging, confidence threshold, allowlist
  shape validation, and kill-switch.
- Agent registry has privacy-oriented summary logging rules.

Provider gaps:

- Current router is not a general household planner.
- Intent detection is narrow.
- Date/time normalization is not production-grade for household scheduling.
- Assignment reasoning is default-assignee-first, not fairness-aware planning.
- No accepted action for task completion matching, rescheduling, status answers,
  confirmation, or explicit task-shopping linkage planning.
- Provider contract does not expose `confirm` or `answer`.
- Provider `reject` is represented as `status=error`, not a canonical action.

## Contract Drift / Documentation Drift

Evidence:

- `docs/integration/ai-platform/v1/README.md`
- `docs/integration/ai-platform/v1/mapping/hometusk-to-upstream.md`
- `docs/integration/ai-platform/v1/mapping/hometusk-to-aiplatform.md`
- `docs/integration/ai-platform/v1/contracts/schemas/command.schema.json`
- `docs/integration/ai-platform/v1/contracts/schemas/decision.schema.json`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionRequest.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionResponseMapper.java`

Observed drift:

- `hometusk-to-aiplatform.md` still describes legacy `/decision`, while runtime
  and current docs use `/v1/decide`.
- HomeTusk wrapper schemas under `docs/integration/ai-platform/v1/contracts`
  still show legacy camelCase request/response structures.
- Runtime sends upstream snake_case envelope directly.
- Runtime maps `propose_add_shopping_item`, but wrapper decision schema only
  lists `create_task` and `complete_task` in internal `proposedAction`.
- Integration README says `reject` support is full, but the current upstream
  action enum does not include `reject`.

Impact:

- The runtime may be safer than the wrapper docs imply, but contract governance
  cannot rely on the wrapper package as-is.
- Before any new planner or `natural_command` work, HomeTusk-owned integration
  mapping must be brought back into alignment.

## Bottom Line

HomeTusk should not build a polished Mobile AI Command Center yet.

The evidence supports a limited next step: define and implement a narrow,
contract-first Domain Planner v1 for simple task creation and shopping item
addition, backed by golden scenarios and explicit HomeTusk adapter cleanup.
