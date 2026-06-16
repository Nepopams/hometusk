# PLAN Findings - INIT-2026Q3 AI Platform 2.1 Contract Intake

## Scope Decision

Proceed with a HomeTusk-owned AI Platform `2.1.0` contract-intake APPLY only.

The approved APPLY may update HomeTusk integration docs, backend adapter/schema/client/mapping, focused tests, fixtures, service catalog, contract index, roadmap, and planning evidence.

The approved APPLY must not add HomeTusk `natural_command`, public `/commands` contract changes, `needs_confirmation`, `answered`, mobile/web UX, direct mobile/web to AI Platform, AI Platform repository writes, provider `answer`, production rollout/config changes, or provider `confirm` execution.

## Provider Evidence

Read-only provider repository:

```text
C:/Users/user/Documents/projects/VR_AI_Platform
```

Provider revision:

```text
5c2eb8c5fbdd75e5bc8d0a9d56333ee756354bb1
```

Provider `2.1.0` evidence:

- `contracts/schemas/command.schema.json` supports capabilities `start_job`, `propose_create_task`, `propose_add_shopping_item`, `clarify`, `reject`, and `confirm`.
- `contracts/schemas/decision.schema.json` supports actions `start_job`, `propose_create_task`, `propose_add_shopping_item`, `clarify`, `reject`, and `confirm`.
- `decision_outcome` is optional and supports `execute`, `clarify`, `reject`, and `confirm`.
- `reject` payload requires `code` and `reason`; optional `ui_message` and `details`.
- `confirm` payload requires `confirmation_id`, `summary`, `proposed_actions`, and `expires_at`.
- Handoff reports 50 evaluated scenarios, 50 schema-valid decisions, 50 outcome matches, 0 unsupported auto-execute, 0 cross-household references, and 0 blocker failure scenarios.
- Provider `answer` remains blocked.

## Current HomeTusk Findings

### Integration docs

- `docs/integration/ai-platform/v1/README.md` states upstream package v1 and HomeTusk default `/v1/decide`.
- `docs/integration/ai-platform/v1/mapping/hometusk-to-aiplatform.md` still contains legacy `/decision` language and camelCase response examples.
- `docs/integration/ai-platform/v1/mapping/hometusk-to-upstream.md` has a runtime note for snake_case envelope but still documents older wrapper fields and has no `confirm` mapping.
- `docs/integration/ai-platform/v1/contracts/schemas/decision.schema.json` allows only old wrapper `type` values `start_job`, `clarify`, and `reject`.
- `docs/integration/ai-platform/v1/upstream/**` is read-only and must not be edited.

Decision:

- Create a new `docs/integration/ai-platform/v2.1/` package because the provider contract version is `2.1.0` and v1 has stale wrapper/upstream history.
- Add supersession notes to HomeTusk-owned v1 docs instead of editing v1 upstream snapshots.

### Backend request capabilities

File:

```text
services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionRequest.java
```

Current state:

```text
DEFAULT_CAPABILITIES = start_job, propose_create_task, propose_add_shopping_item, clarify
```

Decision:

- Add `reject` to advertised capabilities.
- Do not add `confirm` to advertised capabilities by default.
- Do not add a runtime confirm feature flag in this initiative; future `confirm` advertisement requires a `needs_confirmation` contract initiative.

### Backend response schema

File:

```text
services/backend/src/main/resources/schemas/ai-decision-response.schema.json
```

Current state:

- Accepted `action` enum excludes `reject` and `confirm`.
- There is no `decision_outcome`.
- There are no `reject_payload` or `confirm_payload` definitions.

Decision:

- Align schema with provider `2.1.0`.
- Keep existing execute/clarify schema compatibility.
- Add `reject` and `confirm` branches.
- Add optional `decision_outcome`.

### Backend response DTO

File:

```text
services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionResponse.java
```

Current state:

- DTO has no `decision_outcome` field.

Decision:

- Add `@JsonProperty("decision_outcome") String decisionOutcome`.
- Keep existing constructor call sites updated.

### Raw provider payload preservation

Files:

```text
services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiPlatformClient.java
services/backend/src/main/java/com/hometusk/commands/pipeline/decision/AiPlatformDecisionProvider.java
services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionResponseMapper.java
```

Current state:

- `AiPlatformClient` deserializes directly to `AiDecisionResponse`.
- `AiDecisionResponseMapper` serializes the DTO back to JSON for `rawPayload`.
- Unknown fields would be lost before DecisionLog capture.

Decision:

- Preserve the exact raw response JSON from `AiPlatformClient`.
- Validate raw JSON before mapping.
- Deserialize to `AiDecisionResponse` after raw validation.
- Pass exact raw JSON to the mapper and DecisionResult so `DecisionLog.rawDecisionPayload` is audit-faithful.

### Backend response mapping

File:

```text
services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionResponseMapper.java
```

Current state:

- Maps `start_job`.
- Maps `propose_create_task`.
- Maps `propose_add_shopping_item`.
- Maps `clarify`.
- Handles status `error` as reject with `AI_PLATFORM_ERROR`.
- No `reject` or `confirm` branch.

Decision:

- Map provider `reject` to `DecisionResult.Reject`:
  - `reason`: prefer `payload.ui_message`, then `payload.reason`, then `response.explanation`.
  - `errorCode`: prefer `payload.code`, else `AI_REJECTED`.
  - no action execution.
- Map provider `confirm` to `DecisionResult.Reject`:
  - `errorCode`: `AI_CONFIRMATION_UNSUPPORTED`.
  - `reason`: prefer `payload.ui_message`, then `payload.summary`, then a fixed safe message.
  - no action execution.
- Keep existing unknown action behavior as safe reject.

### Command execution and public response

Files:

```text
services/backend/src/main/java/com/hometusk/commands/service/CommandService.java
services/backend/src/main/java/com/hometusk/commands/dto/CommandResponse.java
services/backend/src/main/java/com/hometusk/commands/domain/DecisionLog.java
services/backend/src/main/java/com/hometusk/commands/pipeline/DecisionLogWriter.java
```

Current state:

- `CommandService.handleReject` already writes `DecisionLog`, marks command rejected, returns public `status=rejected`, and does not execute actions.
- `DecisionLog.rawDecisionPayload` already exists.
- Public rejected response carries `errorCode` and `reason`.

Decision:

- No public response schema change in this initiative.
- Extra provider fields such as trace id, decision version, schema version, provider code, provider reason, and confirmation payload remain in `DecisionLog.rawDecisionPayload`.
- `CommandService` should not need behavioral changes unless APPLY discovers raw payload plumbing requires it.

## Approved APPLY File List

Planning:

- `docs/planning/strategy/roadmap.md`
- `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.md`
- `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.execution.md`
- `docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/**`

Integration docs:

- `docs/integration/ai-platform/README.md`
- `docs/integration/ai-platform/v1/README.md`
- `docs/integration/ai-platform/v1/mapping/hometusk-to-upstream.md`
- `docs/integration/ai-platform/v1/mapping/hometusk-to-aiplatform.md`
- `docs/integration/ai-platform/v2.1/**`
- `docs/_indexes/contracts-index.md`
- `docs/architecture/service-catalog.md`

Backend and tests:

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

## Forbidden Files / Scopes

- `docs/integration/ai-platform/v1/upstream/**`
- `docs/contracts/http/commands.openapi.yaml` unless APPLY stops and records a public-contract HOLD decision first
- `clients/mobile/**`
- `clients/web/**`
- `C:/Users/user/Documents/projects/VR_AI_Platform/**`
- HomeTusk `natural_command`, `needs_confirmation`, `answered`, mobile AI UX, direct mobile/web AI Platform calls, and production rollout/config.

## Test Plan

- Adapter unit test for capabilities: `reject` present, `confirm` absent.
- Adapter unit test for provider `reject` -> `DecisionResult.Reject`.
- Adapter unit test for provider `confirm` -> `DecisionResult.Reject` with `AI_CONFIRMATION_UNSUPPORTED`.
- Adapter unit test for existing `start_job`, proposed task, proposed shopping, and `clarify`.
- Schema validation tests for provider `2.1.0` fixtures.
- Integration test for provider `reject`: public rejected response, no task/shopping mutation, raw payload in DecisionLog.
- Integration test for provider `confirm`: public rejected response, no task/shopping mutation, raw payload in DecisionLog.
- Integration test for unknown/invalid provider response.
- Existing provider failure/degraded tests remain passing.

## Verification Commands

- `git diff --check`
- `cd services/backend && ./gradlew test --tests "*AiPlatformDecisionAdapterTest*"`
- `cd services/backend && ./gradlew test --tests "com.hometusk.integration.aiplatform.AiPlatformIntegrationTest"`
- `./scripts/test.sh`
- `git -C C:/Users/user/Documents/projects/VR_AI_Platform status --short`

## Risks and Mitigations

| Risk | Mitigation |
| --- | --- |
| Provider `confirm` executes by accident | Map to `DecisionResult.Reject`, never to `StartJob`; add no-mutation integration test |
| Provider `reject` still fails schema validation | Update schema and mapper together; add reject fixture |
| Exact raw payload is lost | Preserve raw JSON in client/provider before DTO mapping |
| Public API scope creep | Keep public rejected response unchanged; store rich provider fields in DecisionLog |
| Stale v1 docs continue to mislead | Add v2.1 package and v1 supersession notes |
| Provider repo accidentally modified | Verify provider `git status --short` is clean |

## Rollback

- Revert HomeTusk commits from this workpack.
- No database migration expected.
- If runtime adapter compatibility is unsafe, keep `decision.provider=manual` or rely on existing fallback while closing Gate D as HOLD/NO-GO.

## Gate C Recommendation

GO for APPLY limited to the approved file list and stop conditions above.
