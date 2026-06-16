# PLAN Findings: Natural Command & Confirmation Contract Spike

## Decision

PLAN result: **GO for docs-only APPLY**.

## Sources Read

- `AGENTS.md`
- `docs/CODEX-WORKFLOW.md`
- `docs/planning/strategy/product-goal.md`
- `docs/planning/strategy/roadmap.md`
- `docs/planning/releases/MVP.md`
- `docs/_governance/dor.md`
- `docs/_governance/dod.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.md`
- `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.execution.md`
- `docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/gate-d.md`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/integration/ai-platform/v2.1/**`
- `docs/research/ai-command-capabilities/**`
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandRequest.java`
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandResponse.java`
- `services/backend/src/main/java/com/hometusk/commands/domain/CommandStatus.java`
- `services/backend/src/main/java/com/hometusk/commands/domain/CommandType.java`
- `services/backend/src/main/java/com/hometusk/commands/domain/DecisionLog.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/DecisionResult.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionResponseMapper.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/guardrails/GuardrailResult.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/guardrails/GuardrailOutcome.java`
- `clients/mobile/src/api/types.ts`
- `clients/mobile/src/features/command/**`
- Provider handoff and eval under `C:/Users/user/Documents/projects/VR_AI_Platform/**` read-only.

## Current-State Findings

- Current public `CommandRequest.type` supports `create_task` and `complete_task`.
- Current public response variants are `executed`, `scheduled`, `needs_input`, `rejected`, and `executed_degraded`.
- Current mobile command types mirror those response variants.
- Current `CommandStatus` has no pending confirmation status.
- Current `DecisionResult` has `StartJob`, `Clarify`, and `Reject`; no `Confirm` variant.
- Current AI Platform mapper maps provider `confirm` to `DecisionResult.Reject` with `AI_CONFIRMATION_UNSUPPORTED`.
- Current `DecisionLog` stores raw provider payload, external decision id, confidence, alternatives, and validation flags, but it is not a pending-state source of truth.
- Current guardrails can proceed, clarify, or reject; they do not create confirmation records.
- Current command execution links task and shopping items if both are created in the same decision, so mixed plans need explicit future confirmation policy.
- Provider `2.1.0` supports first-class `reject` and schema-level non-executing `confirm`; `answer` remains blocked.

## Contract Direction Findings

- Prefer future `type=natural_command` under existing `POST /api/v1/commands`; do not create a direct AI endpoint.
- Require `payload.text`, `inputMode`, `locale`, `timezone`, and `referenceInstant` for natural commands.
- Require `Idempotency-Key` and `X-Correlation-ID` compatibility with existing command semantics.
- Add future first-class `needs_confirmation`; do not overload `needs_input`.
- Keep `answered` out of scope.
- Treat provider `confirm` as no-mutation proposal input until HomeTusk creates a pending confirmation.
- HomeTusk should mint/own the public confirmation id even when provider supplies a provider confirmation id.
- Expiry, approval/cancel idempotency, and household authorization are HomeTusk-owned.
- Store pending confirmation state in a future explicit model; keep `DecisionLog` as audit evidence.

## Approved Docs-Only APPLY Files

- `docs/planning/strategy/roadmap.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.execution.md`
- `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/**`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/**`
- `docs/_indexes/contracts-index.md`

## Forbidden Files

- `docs/contracts/http/commands.openapi.yaml`
- `services/backend/src/main/java/**`
- `services/backend/src/main/resources/db/migration/**`
- `clients/**`
- `docs/integration/ai-platform/v1/upstream/**`
- `C:/Users/user/Documents/projects/VR_AI_Platform/**`

## STOP Conditions

- Need to change accepted public OpenAPI.
- Need to change Java/backend/mobile/web/provider files.
- Need to treat provider `confirm` as executable.
- Need to include `answered` runtime/API contract.
- Missing provider evidence for blocker-free schema-valid behavior.
- Need to store pending state only in `DecisionLog`.

## Gate C Recommendation

**GO** for docs-only APPLY, limited to the approved file list above.
