# INIT-2026Q3 AI Platform 2.1 Contract Intake Gate C

## Sources of Truth

- Initiative: `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.md`
- Execution notes: `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.execution.md`
- Workpack: `docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/workpack.md`
- PLAN findings: `docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/plan-findings.md`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Workflow: `AGENTS.md`, `docs/planning/AGENTS.md`, `docs/CODEX-WORKFLOW.md`

## Decision

GO.

## Decider

Codex, under delegated human-gate authority from the user goal.

## Date

2026-06-15

## Approved Scope

- HomeTusk-owned AI Platform `2.1.0` integration docs and snapshot.
- HomeTusk-owned v1 mapping supersession notes.
- Backend AI Platform adapter/schema/client/mapper compatibility for `reject` and `confirm`.
- `reject` advertised as a capability.
- `confirm` not advertised by default.
- Unexpected provider `confirm` mapped to controlled non-execution rejected result with `AI_CONFIRMATION_UNSUPPORTED`.
- Raw provider response preserved in `DecisionLog.rawDecisionPayload`.
- Focused backend tests and fixtures for provider `2.1.0`.
- Service catalog and contract index updates.
- Planning evidence updates.

## Approved Files

- `docs/planning/strategy/roadmap.md`
- `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.md`
- `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.execution.md`
- `docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/**`
- `docs/integration/ai-platform/README.md`
- `docs/integration/ai-platform/v1/README.md`
- `docs/integration/ai-platform/v1/mapping/hometusk-to-upstream.md`
- `docs/integration/ai-platform/v1/mapping/hometusk-to-aiplatform.md`
- `docs/integration/ai-platform/v2.1/**`
- `docs/_indexes/contracts-index.md`
- `docs/architecture/service-catalog.md`
- `services/backend/src/main/resources/schemas/ai-decision-response.schema.json`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionRequest.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionResponse.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionResponseMapper.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiPlatformClient.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/AiPlatformDecisionProvider.java`
- `services/backend/src/test/java/com/hometusk/commands/pipeline/decision/client/AiPlatformDecisionAdapterTest.java`
- `services/backend/src/test/java/com/hometusk/integration/aiplatform/AiPlatformIntegrationTestBase.java`
- `services/backend/src/test/java/com/hometusk/integration/aiplatform/AiPlatformIntegrationTest.java`
- `services/backend/src/test/resources/ai-platform/v2.1/**`

## Forbidden Files and Scopes

- `docs/integration/ai-platform/v1/upstream/**`
- `docs/contracts/http/commands.openapi.yaml` unless APPLY stops and records a public-contract HOLD decision first
- `clients/mobile/**`
- `clients/web/**`
- `C:/Users/user/Documents/projects/VR_AI_Platform/**`
- HomeTusk `natural_command`
- `needs_confirmation`
- `answered`
- Mobile AI Command UX
- Direct mobile/web to AI Platform calls
- AI Platform code/schema/doc writes
- Production rollout/config changes
- Provider `confirm` execution
- Provider `answer`

## Conditions

- Existing execute/clarify behavior must remain compatible.
- Provider `reject` and `confirm` must create no domain mutation.
- Provider `confirm` must never map to `StartJob`.
- Public HomeTusk rejected response may stay limited to `errorCode` and `reason`; rich provider fields must remain auditable through DecisionLog raw payload.
- If APPLY needs any out-of-scope file or public behavior, stop and record HOLD.

## Required Verification

- `git diff --check`
- `cd services/backend && ./gradlew test --tests "*AiPlatformDecisionAdapterTest*"`
- `cd services/backend && ./gradlew test --tests "com.hometusk.integration.aiplatform.AiPlatformIntegrationTest"`
- `./scripts/test.sh` or documented blocker
- `git -C C:/Users/user/Documents/projects/VR_AI_Platform status --short`

## Gate D Posture

HOLD until APPLY is complete, tests/checks are recorded, and a read-only review gate confirms the diff stayed inside this Gate C scope.
