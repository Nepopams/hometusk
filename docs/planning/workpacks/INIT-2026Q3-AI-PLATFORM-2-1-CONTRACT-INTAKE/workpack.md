# Workpack: INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE

## Sources of Truth

- Scope anchor: `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.md`
- Execution notes: `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.execution.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Product goal: `docs/planning/strategy/product-goal.md`
- MVP release scope: `docs/planning/releases/MVP.md`
- Workflow: `AGENTS.md`, `docs/planning/AGENTS.md`, `docs/CODEX-WORKFLOW.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- Current Commands contract: `docs/contracts/http/commands.openapi.yaml`
- Contract index: `docs/_indexes/contracts-index.md`
- Service catalog: `docs/architecture/service-catalog.md`
- Current AI Platform integration docs: `docs/integration/ai-platform/v1/**`
- Backend adapter: `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/**`
- Backend decision log: `services/backend/src/main/java/com/hometusk/commands/domain/DecisionLog.java`, `services/backend/src/main/java/com/hometusk/commands/pipeline/DecisionLogWriter.java`
- Backend AI Platform tests: `services/backend/src/test/java/com/hometusk/commands/pipeline/decision/client/AiPlatformDecisionAdapterTest.java`, `services/backend/src/test/java/com/hometusk/integration/aiplatform/**`
- Provider read-only source: `C:/Users/user/Documents/projects/VR_AI_Platform`
- Provider handoff: `C:/Users/user/Documents/projects/VR_AI_Platform/docs/planning/workpacks/ST-056/hometusk-handoff.md`
- Provider schemas: `C:/Users/user/Documents/projects/VR_AI_Platform/contracts/schemas/command.schema.json`, `C:/Users/user/Documents/projects/VR_AI_Platform/contracts/schemas/decision.schema.json`
- Provider eval: `C:/Users/user/Documents/projects/VR_AI_Platform/docs/planning/workpacks/ST-052/local-50-scenario-eval-report.json`

## Status

**DONE - GATE D GO.** Gate A/B/C/D are delegated to Codex and recorded in the execution notes. APPLY and review evidence are complete as of 2026-06-15.

`prompt-apply.md`, `review-gate.md`, and `gate-d.md` are recorded in this workpack.

## Objective

Make HomeTusk a safe consumer of AI Platform `2.1.0` decisions without approving HomeTusk `natural_command`, public `/commands` changes, `needs_confirmation`, `answered`, mobile AI UX, or AI Platform repository changes.

Target behavior:

```text
AI Platform 2.1.0 snapshot documented
-> HomeTusk adapter understands execute / clarify / reject / confirm
-> reject maps to safe HomeTusk rejection
-> confirm maps to safe non-execution rejection until needs_confirmation exists
-> existing command flows keep working
-> no natural_command runtime yet
```

## In Scope

- Create a HomeTusk-owned AI Platform `v2.1` integration package.
- Import provider `2.1.0` `command.schema.json` and `decision.schema.json` as a local snapshot from the read-only provider repo.
- Add mapping docs for HomeTusk to AI Platform and AI Platform to HomeTusk.
- Add examples for execute create-task, execute shopping items, clarify, reject, confirm, and unknown/invalid behavior.
- Update or supersede stale HomeTusk-owned AI Platform v1 mapping docs, without editing `docs/integration/ai-platform/v1/upstream/**`.
- Update adapter/schema/client mapping so provider `reject` maps to `status=rejected` and never executes.
- Keep provider `confirm` non-executing and map it to `status=rejected`, `errorCode=AI_CONFIRMATION_UNSUPPORTED`.
- Send `reject` capability by default.
- Do not send `confirm` capability by default.
- Preserve provider raw response payload for audit in `DecisionLog.rawDecisionPayload`.
- Add focused backend tests and fixtures for provider `2.1.0`.
- Update service catalog and contract index for the consumed provider snapshot.

## Out of Scope

- HomeTusk `natural_command`.
- Public `/commands` OpenAPI change.
- `needs_confirmation`.
- `answered`.
- Mobile AI Command UX.
- Mobile confirmation or answer cards.
- Direct mobile/web to AI Platform calls.
- AI Platform repository changes.
- Production rollout/config changes.
- Provider `confirm` execution.
- Provider `answer` support.
- Direct plural provider `add_shopping_items` action.

## Impact Flags

| Flag | Value | Notes |
| --- | --- | --- |
| `contract_impact` | yes-external-provider-snapshot; no public HomeTusk API expected | Consume AI Platform `2.1.0`, no `/commands` shape change |
| `backend_impact` | yes | Adapter/schema/client/tests |
| `mobile_impact` | no | Explicitly out of scope |
| `ai_platform_impact` | no | Provider repo read-only |
| `security_sensitive` | yes | AI rejection/confirmation and raw provider payload |
| `traceability_critical` | yes | Provider trace/version/decision id and raw payload |
| `adr_needed` | no for planned scope | Reassess if APPLY needs architecture policy changes |
| `diagrams_needed` | no for planned scope | Reassess if APPLY changes integration flow |
| `cross_repo` | yes | Provider evidence read-only |

## Acceptance Criteria

- [x] AI Platform `2.1.0` provider contract is documented/snapshotted in HomeTusk.
- [x] Mapping docs explain `execute`, `clarify`, `reject`, and `confirm`.
- [x] Stale `/decision`, old wrapper schema, and old reject/confirm mapping language are superseded in HomeTusk-owned docs.
- [x] Provider `reject` maps to HomeTusk `rejected` and never executes actions.
- [x] Provider `confirm` never executes actions and maps to `rejected / AI_CONFIRMATION_UNSUPPORTED`.
- [x] Existing `start_job`, `propose_create_task`, `propose_add_shopping_item`, and `clarify` behavior remains compatible.
- [x] Unknown provider actions reject safely.
- [x] Invalid provider schema rejects safely.
- [x] Malformed provider JSON rejects safely without fallback mutation.
- [x] Provider failure still degrades/falls back according to existing policy.
- [x] DecisionLog captures raw provider payload for `reject` and `confirm`.
- [x] No domain mutation happens for `reject` or `confirm`.
- [x] No HomeTusk `natural_command`, `needs_confirmation`, `answered`, mobile/web UX, or public `/commands` contract change is added.
- [x] No AI Platform repository files are modified.

## Files Expected To Change

Planning artifacts:

- `docs/planning/strategy/roadmap.md`
- `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.md`
- `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.execution.md`
- `docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/**`

Integration docs:

- `docs/integration/ai-platform/README.md`
- `docs/integration/ai-platform/v1/README.md`
- `docs/integration/ai-platform/v1/mapping/hometusk-to-upstream.md`
- `docs/integration/ai-platform/v1/mapping/hometusk-to-aiplatform.md`
- `docs/integration/ai-platform/v2.1/AGENTS.md`
- `docs/integration/ai-platform/v2.1/README.md`
- `docs/integration/ai-platform/v2.1/upstream/README.md`
- `docs/integration/ai-platform/v2.1/upstream/contracts/VERSION`
- `docs/integration/ai-platform/v2.1/upstream/contracts/schemas/command.schema.json`
- `docs/integration/ai-platform/v2.1/upstream/contracts/schemas/decision.schema.json`
- `docs/integration/ai-platform/v2.1/mapping/hometusk-to-aiplatform.md`
- `docs/integration/ai-platform/v2.1/mapping/aiplatform-to-hometusk.md`
- `docs/integration/ai-platform/v2.1/examples/decision-execute-create-task.json`
- `docs/integration/ai-platform/v2.1/examples/decision-execute-shopping-items.json`
- `docs/integration/ai-platform/v2.1/examples/decision-clarify.json`
- `docs/integration/ai-platform/v2.1/examples/decision-reject.json`
- `docs/integration/ai-platform/v2.1/examples/decision-confirm.json`
- `docs/integration/ai-platform/v2.1/examples/decision-invalid-unknown-action.json`
- `docs/integration/ai-platform/v2.1/compatibility.md`
- `docs/_indexes/contracts-index.md`
- `docs/architecture/service-catalog.md`

Backend/runtime and tests:

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

Do not change:

- `docs/integration/ai-platform/v1/upstream/**`
- `docs/contracts/http/commands.openapi.yaml` unless APPLY proves a public contract note is unavoidable and records a STOP/HOLD decision first
- `clients/mobile/**`
- `clients/web/**`
- `C:/Users/user/Documents/projects/VR_AI_Platform/**`

## Implementation Plan

### Commit 1 - Integration docs and provider snapshot

Steps:

1. Create the `docs/integration/ai-platform/v2.1/` package.
2. Import provider schemas from read-only provider revision `5c2eb8c5fbdd75e5bc8d0a9d56333ee756354bb1`.
3. Add mapping, examples, and compatibility notes.
4. Supersede stale HomeTusk-owned v1 mapping docs without editing `v1/upstream/**`.
5. Update service catalog and contracts index.

Verification:

- JSON parse provider snapshots/examples.
- `git diff --check`.

### Commit 2 - Backend adapter compatibility

Steps:

1. Add `reject` to default advertised capabilities.
2. Keep `confirm` out of advertised capabilities.
3. Add `decision_outcome` to the response DTO.
4. Update the schema validator for AI Platform `2.1.0`.
5. Preserve exact raw provider JSON through the client/provider/mapper path before storing it in `DecisionLog.rawDecisionPayload`.
6. Map provider `reject` to `DecisionResult.Reject`.
7. Map provider `confirm` to `DecisionResult.Reject` with `AI_CONFIRMATION_UNSUPPORTED`.
8. Keep existing execute/clarify behavior compatible.

Verification:

- `cd services/backend && ./gradlew test --tests "*AiPlatformDecisionAdapterTest*"`

### Commit 3 - Fixtures, integration tests, and evidence

Steps:

1. Add provider `2.1` fixtures under `services/backend/src/test/resources/ai-platform/v2.1/`.
2. Extend WireMock stubs for reject/confirm/unknown actions.
3. Add integration tests for no mutation and DecisionLog raw payload.
4. Run focused AI Platform tests.
5. Update workpack/checklist with actual evidence.

Verification:

- `cd services/backend && ./gradlew test --tests "com.hometusk.integration.aiplatform.AiPlatformIntegrationTest"`
- `./scripts/test.sh` if focused tests pass and environment supports the full suite.

## Contract Impact

- Provider: AI Platform external provider.
- Consumer: HomeTusk backend.
- Protocol/version: AI Platform provider contract `2.1.0`.
- HomeTusk public API: no intended `/commands` contract change.
- Compatibility: additive provider intake. Existing provider `start_job`, `propose_create_task`, `propose_add_shopping_item`, and `clarify` remain supported.
- Public response limitation: HomeTusk `rejected` response carries `errorCode` and `reason`; provider trace/version/decision/code/ui fields are preserved in `DecisionLog.rawDecisionPayload`.

## Tests

- [x] Unit/adapter: request capabilities include `reject` and exclude `confirm`.
- [x] Unit/adapter: `reject` maps to `DecisionResult.Reject`.
- [x] Unit/adapter: `confirm` maps to `DecisionResult.Reject` with `AI_CONFIRMATION_UNSUPPORTED`.
- [x] Unit/adapter: existing execute and clarify mappings still pass.
- [x] Unit/validator: provider `2.1.0` reject and confirm fixtures validate.
- [x] Integration: `reject` returns public `status=rejected` and creates no task/shopping mutation.
- [x] Integration: `confirm` returns public `status=rejected`, creates no mutation, and stores raw payload.
- [x] Integration: unknown action/invalid schema rejects safely.
- [x] Integration: malformed non-JSON provider output rejects safely without fallback mutation.
- [x] Integration: provider failure still degrades/falls back.

## Verification Commands

- `git diff --check` - PASS; LF-to-CRLF warnings only.
- PowerShell JSON parse for v2.1 docs/test fixtures - PASS; 14 JSON files.
- Provider schema JSON-equivalence against read-only provider repo - PASS.
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests "*AiPlatformDecisionAdapterTest*" --console=plain` - PASS.
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests "com.hometusk.integration.aiplatform.AiPlatformIntegrationTest" --console=plain` - PASS.
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --console=plain` - PASS; 496 tests, 0 failures, 0 errors, 5 skipped.
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain spotlessCheck --console=plain` - PASS.
- `bash scripts/test.sh` - BLOCKED by missing WSL `/bin/bash`; equivalent full Gradle test command passed.
- `git -C C:/Users/user/Documents/projects/VR_AI_Platform status --short` - PASS; no output.

## Risks

- Provider `confirm` accidentally executes -> map confirm to reject and add no-mutation integration tests.
- Provider `reject` is still treated as schema-invalid -> update schema and mapper together.
- Raw provider payload is not exact -> preserve raw JSON before DTO mapping.
- Public API scope creep -> do not change `/commands`; store extra provider metadata in DecisionLog.
- Stale v1 docs continue to confuse implementers -> add supersession notes and a root integration README.
- Provider repo mutation by mistake -> inspect read-only and verify provider status remains clean.

## Rollback

- Revert HomeTusk docs/backend/test commits from this workpack.
- No database migration is expected.
- No provider repository rollback is needed because provider repo must remain read-only.
- If AI Platform `2.1.0` is not safe after APPLY, set `decision.provider=manual` or rely on existing fallback policy while closing Gate D as HOLD/NO-GO.

## Prompt Pack

- PLAN: `docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/prompt-plan.md`
- PLAN findings: `docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/plan-findings.md`
- Gate C: `docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/gate-c.md`
- APPLY: `docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/prompt-apply.md`
- Review gate: `docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/review-gate.md`
- Gate D: `docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/gate-d.md`
