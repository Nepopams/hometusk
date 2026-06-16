# Codex APPLY Prompt - INIT-2026Q3 AI Platform 2.1 Contract Intake

## Mode

APPLY. Implement only the approved Gate C scope.

## Objective

Make HomeTusk safely consume AI Platform provider contract `2.1.0`:

- import/document provider `2.1.0` snapshot;
- safely map provider `reject` to HomeTusk `rejected`;
- safely map provider `confirm` to non-executing `rejected / AI_CONFIRMATION_UNSUPPORTED`;
- preserve raw provider payload in `DecisionLog`;
- keep existing execute/clarify behavior compatible.

## Sources of Truth

- Initiative: `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.md`
- Execution: `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.execution.md`
- Workpack: `docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/workpack.md`
- PLAN findings: `docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/plan-findings.md`
- Gate C: `docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/gate-c.md`

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
- AI Platform repo writes
- Production rollout/config changes
- Provider `confirm` execution
- Provider `answer`

## Required Behavior

- AI output is schema-validated before mapping.
- Default capabilities include `reject`.
- Default capabilities do not include `confirm`.
- Provider `reject` returns public `status=rejected` and creates no mutation.
- Provider `confirm` returns public `status=rejected`, `errorCode=AI_CONFIRMATION_UNSUPPORTED`, and creates no mutation.
- Existing execute/clarify/degraded behavior remains compatible.
- Raw provider response is stored in `DecisionLog.rawDecisionPayload`.

## Verification

- `git diff --check`
- `cd services/backend && ./gradlew test --tests "*AiPlatformDecisionAdapterTest*"`
- `cd services/backend && ./gradlew test --tests "com.hometusk.integration.aiplatform.AiPlatformIntegrationTest"`
- `./scripts/test.sh` or documented blocker
- `git -C C:/Users/user/Documents/projects/VR_AI_Platform status --short`

## STOP-THE-LINE

- Need to change public `/commands` contract.
- Need to add `natural_command`, `needs_confirmation`, or `answered`.
- Need to modify mobile/web UX.
- Need to modify AI Platform repo.
- Need to execute provider `confirm`.
- Need to change production rollout/config.
