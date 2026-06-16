# Codex PLAN Prompt - INIT-2026Q3 AI Platform 2.1 Contract Intake

## Mode

PLAN only. Read-only.

Do not edit, create, delete, move, format, or generate tracked files during PLAN.
Do not implement runtime code.
Do not generate `prompt-apply.md`.

## Objective

Produce a decision-complete implementation plan for `INIT-2026Q3-ai-platform-2-1-contract-intake`:

HomeTusk must safely consume AI Platform `2.1.0` provider decisions, including first-class `reject` and schema-level non-executing `confirm`, without adding HomeTusk `natural_command`, public `/commands` changes, `needs_confirmation`, `answered`, mobile AI UX, direct mobile/web AI Platform calls, production rollout, or AI Platform repository changes.

## Sources of Truth

- Initiative: `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.md`
- Execution notes: `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.execution.md`
- Workpack: `docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/workpack.md`
- Checklist: `docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/checklist.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Product goal: `docs/planning/strategy/product-goal.md`
- MVP release: `docs/planning/releases/MVP.md`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Workflow: `AGENTS.md`, `docs/planning/AGENTS.md`, `docs/CODEX-WORKFLOW.md`
- Commands contract: `docs/contracts/http/commands.openapi.yaml`
- Contract index: `docs/_indexes/contracts-index.md`
- Service catalog: `docs/architecture/service-catalog.md`
- Current AI Platform integration docs: `docs/integration/ai-platform/v1/**`
- Backend AI request: `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionRequest.java`
- Backend AI response: `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionResponse.java`
- Backend AI mapper: `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionResponseMapper.java`
- Backend AI validator: `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiResponseSchemaValidator.java`
- Backend AI client: `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiPlatformClient.java`
- Backend AI provider: `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/AiPlatformDecisionProvider.java`
- Decision log: `services/backend/src/main/java/com/hometusk/commands/domain/DecisionLog.java`, `services/backend/src/main/java/com/hometusk/commands/pipeline/DecisionLogWriter.java`
- AI Platform tests: `services/backend/src/test/java/com/hometusk/commands/pipeline/decision/client/AiPlatformDecisionAdapterTest.java`, `services/backend/src/test/java/com/hometusk/integration/aiplatform/**`
- Provider repo read-only: `C:/Users/user/Documents/projects/VR_AI_Platform`

## In Scope

- Versioned AI Platform `v2.1` HomeTusk integration docs.
- Provider `2.1.0` schema snapshot import.
- HomeTusk-owned v1 mapping supersession notes.
- Backend schema validator, response DTO, client/provider raw payload preservation, mapper, and capabilities.
- Tests and fixtures for execute, clarify, reject, confirm, unknown/invalid, degraded/fallback, raw DecisionLog payload, and no mutation.
- Service catalog and contract index updates.

## Out of Scope

- HomeTusk `natural_command`.
- Public `/commands` OpenAPI change unless PLAN proves unavoidable and stops for a HOLD decision.
- `needs_confirmation`.
- `answered`.
- Mobile/web UX.
- Direct mobile/web to AI Platform.
- AI Platform repo writes.
- Production config or rollout.
- Provider `answer`.
- Direct plural provider `add_shopping_items`.

## Required PLAN Output

Record:

- current-state findings with exact files/classes/methods;
- final implementation file list;
- mapping strategy for `reject`;
- mapping strategy for `confirm`;
- capability negotiation strategy;
- raw provider payload traceability strategy;
- public API compatibility decision;
- test plan;
- verification commands;
- risks and rollback;
- STOP-THE-LINE conditions;
- Gate C recommendation.

## Required Mapping Decisions

- `reject`: map to `DecisionResult.Reject`, public `status=rejected`, no action execution, raw provider payload preserved in DecisionLog.
- `confirm`: do not advertise by default; if received unexpectedly, map to `DecisionResult.Reject` with `AI_CONFIRMATION_UNSUPPORTED`, no action execution, raw provider payload preserved in DecisionLog.
- `answer`: blocked.
- repeated singular provider `propose_add_shopping_item`: keep supported; do not add direct plural provider action.

## Invariants

- AI output is schema-validated before use.
- HomeTusk remains source of truth and execution authority.
- Domain invariants stay in code.
- Every command remains traceable through DecisionLog.
- Degraded/fallback behavior remains deterministic.
- Invalid/unknown provider output rejects safely.
- Confirm never executes.
- No public HomeTusk API scope creep.
- No AI Platform repo writes.

## Verification Candidates

- `git diff --check`
- `cd services/backend && ./gradlew test --tests "*AiPlatformDecisionAdapterTest*"`
- `cd services/backend && ./gradlew test --tests "com.hometusk.integration.aiplatform.AiPlatformIntegrationTest"`
- `./scripts/test.sh`
- `git -C C:/Users/user/Documents/projects/VR_AI_Platform status --short`

## STOP-THE-LINE

- Need to implement HomeTusk `natural_command`.
- Need to add `needs_confirmation` or `answered`.
- Need to change public `/commands` schema/response shape.
- Need to modify mobile/web UX.
- Need to modify AI Platform repository files.
- Need to execute provider `confirm`.
- Need to change production configuration or rollout behavior.
- Need to broaden provider acceptance into HomeTusk product GO.
