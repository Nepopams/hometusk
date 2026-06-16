# INIT-2026Q3 Natural Command + Needs Confirmation Backend Contract - Workpack

## Sources of Truth

- Scope anchor: `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md`
- Execution notes: `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.execution.md`
- Product goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- MVP release scope: `docs/planning/releases/MVP.md`
- DoR / DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Workflow: `AGENTS.md`, `docs/CODEX-WORKFLOW.md`
- Accepted spike: `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.md`
- Spike execution evidence: `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.execution.md`
- Draft contract package: `docs/research/ai-command-capabilities/natural-command-contract-spike/**`
- Commands contract: `docs/contracts/http/commands.openapi.yaml`
- Contract index: `docs/_indexes/contracts-index.md`
- Service catalog: `docs/architecture/service-catalog.md`
- Current backend command package: `services/backend/src/main/java/com/hometusk/commands/**`
- Current backend tests: `services/backend/src/test/java/com/hometusk/integration/**`, `services/backend/src/test/java/com/hometusk/commands/**`

## Outcome

Implement the backend/API contract foundation for HomeTusk `natural_command` and
`needs_confirmation` without mobile/web UI or AI Platform repo changes.

Target backend behavior:

```text
POST /api/v1/commands type=natural_command
-> schema-validated natural command payload
-> AI Platform decision
-> execute / clarify / reject / needs_confirmation
-> provider confirm creates HomeTusk-owned pending confirmation
-> no domain mutation before approval
-> DecisionLog preserves raw provider payload and mapped outcome
```

## Acceptance Criteria

- [ ] Roadmap marks this initiative as current NOW scope.
- [ ] Commands API contract documents additive `natural_command` request support.
- [ ] Commands API contract documents first-class `needs_confirmation` response.
- [ ] Backend accepts `CommandType.NATURAL_COMMAND` and validates natural command payload fields.
- [ ] `natural_command` uses the existing `POST /api/v1/commands` boundary; no separate AI endpoint is added.
- [ ] AI Platform request text comes from `payload.text` for `natural_command`.
- [ ] Missing `text`, `inputMode`, `locale`, `timezone`, or `referenceInstant` rejects with schema validation or clarifies safely.
- [ ] Provider `confirm` maps to `needs_confirmation`, not `rejected / AI_CONFIRMATION_UNSUPPORTED`, when payload is supported.
- [ ] Provider `confirm` does not execute task or shopping mutations before explicit approval.
- [ ] Pending confirmation has HomeTusk-owned state if `needs_confirmation` runtime is implemented.
- [ ] `DecisionLog` records raw provider payload and mapped confirmation outcome.
- [ ] Unsupported/invalid provider confirmation payload rejects or clarifies safely and never mutates.
- [ ] Existing structured `create_task`, `complete_task`, execute, clarify, reject, scheduled, and degraded behavior remains compatible.
- [ ] Tests cover natural command validation, provider confirm no-mutation, DecisionLog traceability, and household boundary/authorization.
- [ ] ADR/diagram decision is recorded; artifacts are updated if required.
- [ ] Review gate produces GO / NO-GO before Gate D.

## Non-goals

- Mobile/web UI or confirmation cards.
- `answered`.
- Broad autonomous planning.
- Direct mobile/web calls to AI Platform.
- AI Platform repo or upstream snapshot writes.
- Production rollout/config enablement.
- Local LLM implementation.
- Silent auto-fix of invalid AI output.
- Raw audio storage.

## Impact Flags

| Flag | Value | Notes |
| --- | --- | --- |
| `contract_impact` | yes | Accepted Commands API behavior changes |
| `backend_impact` | yes | DTOs, command pipeline, provider mapping, tests |
| `data_impact` | yes | Pending confirmation state likely requires migration |
| `security_sensitive` | yes | Household-scoped confirmation, approval actor, no-mutation guarantee |
| `traceability_critical` | yes | DecisionLog and confirmation lifecycle evidence |
| `adr_needed` | yes/maybe | Required if persistent pending confirmation lifecycle is introduced |
| `diagrams_needed` | maybe | Use if it reduces review risk for confirm/approve flow |
| `mobile_impact` | no | Read-only context only |
| `ai_platform_impact` | no | Provider read-only input only |

## Files to Change

PLAN must confirm exact files before APPLY. Expected candidates:

- `docs/planning/strategy/roadmap.md` - current initiative and final status.
- `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md` - initiative status if closure changes.
- `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.execution.md` - gates, evidence, risks.
- `docs/contracts/http/commands.openapi.yaml` - accepted Commands API contract.
- `docs/_indexes/contracts-index.md` - material contract note.
- `docs/architecture/service-catalog.md` - command pipeline and data store updates.
- `docs/adr/**` - pending confirmation state decision if required.
- `docs/diagrams/**` - confirmation flow if required.
- `docs/_indexes/adr-index.md` - if ADR is added.
- `docs/_indexes/diagrams-index.md` - if diagram is added.
- `services/backend/src/main/java/com/hometusk/commands/domain/CommandType.java` - add `NATURAL_COMMAND`.
- `services/backend/src/main/java/com/hometusk/commands/domain/CommandStatus.java` - add `NEEDS_CONFIRMATION` if pending state is implemented.
- `services/backend/src/main/java/com/hometusk/commands/domain/**` - pending confirmation entity/status if implemented.
- `services/backend/src/main/java/com/hometusk/commands/repository/**` - pending confirmation repository if implemented.
- `services/backend/src/main/java/com/hometusk/commands/dto/**` - natural command/confirmation DTOs and responses.
- `services/backend/src/main/java/com/hometusk/commands/api/CommandController.java` - contract endpoints if approve/cancel is in scope.
- `services/backend/src/main/java/com/hometusk/commands/service/CommandService.java` - command handling and confirmation lifecycle.
- `services/backend/src/main/java/com/hometusk/commands/pipeline/SchemaValidator.java` - natural command schema registration.
- `services/backend/src/main/java/com/hometusk/commands/pipeline/BusinessValidator.java` - natural command business validation if needed.
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/DecisionResult.java` - confirm result if implemented.
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/ManualDecisionProvider.java` - safe deterministic fallback for natural command.
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/DecisionProviderSelector.java` - fallback source handling for non-executing natural command.
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionRequest.java` - natural command text/context mapping.
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionResponseMapper.java` - provider confirm mapping.
- `services/backend/src/main/resources/schemas/natural-command.schema.json` - request payload schema.
- `services/backend/src/main/resources/db/migration/V033__*.sql` - command type/status constraints and confirmation table if needed.
- `services/backend/src/test/java/com/hometusk/commands/pipeline/decision/client/AiPlatformDecisionAdapterTest.java` - mapper tests.
- `services/backend/src/test/java/com/hometusk/integration/aiplatform/AiPlatformIntegrationTest.java` - provider confirm integration.
- `services/backend/src/test/java/com/hometusk/integration/aiplatform/AiPlatformIntegrationTestBase.java` - confirm stubs.
- `services/backend/src/test/java/com/hometusk/integration/CommandPipelineTest.java` - request validation and compatibility tests.

Forbidden unless a later gate records a scope change:

- `clients/**`
- `docs/integration/ai-platform/v1/upstream/**`
- `C:/Users/user/Documents/projects/VR_AI_Platform/**`

## Implementation Plan (commit-sized)

### Commit 1 - Planning, contract, ADR/diagram

Steps:

1. Complete read-only PLAN and Gate C.
2. Update accepted Commands API contract with additive `natural_command` and
   `needs_confirmation` semantics.
3. Add ADR/diagram if PLAN confirms persistent pending confirmation lifecycle.
4. Update indexes and service catalog.

Verification:

- `rg -n "natural_command|needs_confirmation|Gate C|Artifact Gate" docs/planning docs/contracts docs/adr docs/diagrams docs/_indexes docs/architecture/service-catalog.md`

### Commit 2 - Backend model and mapping

Steps:

1. Add natural command schema and command type.
2. Add confirmation response DTO and pending confirmation model if approved.
3. Map provider `confirm` to `DecisionResult.Confirm`.
4. Handle `DecisionResult.Confirm` in `CommandService` by guardrail-checking as proposal and returning `needs_confirmation`.
5. Add safe natural-command fallback behavior for manual/degraded mode.

Verification:

- `cd services/backend && ./gradlew test --tests "com.hometusk.commands.pipeline.decision.client.AiPlatformDecisionAdapterTest"`

### Commit 3 - Confirmation lifecycle and integration tests

Steps:

1. Add approve/cancel endpoints only if Gate C includes them.
2. Add integration tests for no mutation before approval, DecisionLog traceability, and household/actor boundary.
3. Preserve existing structured command compatibility.

Verification:

- `cd services/backend && ./gradlew test --tests "com.hometusk.integration.CommandPipelineTest"`
- `cd services/backend && ./gradlew test --tests "com.hometusk.integration.aiplatform.AiPlatformIntegrationTest"`

## Contract Impact

The expected contract change is additive:

- existing `create_task` and `complete_task` request variants remain valid;
- `natural_command` is added under existing `POST /api/v1/commands`;
- `needs_confirmation` is added as a command response variant;
- approve/cancel endpoints are added only if Gate C approves their first slice.

Breaking changes are not allowed without a new versioning/migration plan.

## Docs Updates

- [ ] `docs/contracts/http/commands.openapi.yaml`
- [ ] `docs/_indexes/contracts-index.md`
- [ ] `docs/architecture/service-catalog.md`
- [ ] `docs/adr/**` if required
- [ ] `docs/diagrams/**` if required
- [ ] affected indexes if ADR/diagram is added

## Tests

- [ ] Unit: AI Platform mapper maps `confirm` to confirmation result.
- [ ] Unit: natural command request maps `payload.text`.
- [ ] Integration: `natural_command` validates required fields.
- [ ] Integration: provider confirm returns `needs_confirmation`.
- [ ] Integration: provider confirm does not create tasks or shopping items.
- [ ] Integration: raw provider payload is preserved in DecisionLog.
- [ ] Integration: structured command compatibility remains intact.
- [ ] Boundary: pending confirmation is household/requester scoped if approve/cancel is implemented.
- [ ] Degraded: AI unavailable for `natural_command` returns safe deterministic non-mutating fallback.

## Verification Commands

- `git status --short` - inspect diff and unrelated dirty files.
- `cd services/backend && ./gradlew test --tests "com.hometusk.commands.pipeline.decision.client.AiPlatformDecisionAdapterTest"` - mapper/unit slice.
- `cd services/backend && ./gradlew test --tests "com.hometusk.integration.CommandPipelineTest"` - command contract compatibility.
- `cd services/backend && ./gradlew test --tests "com.hometusk.integration.aiplatform.AiPlatformIntegrationTest"` - AI Platform mapping and no-mutation behavior.
- `./scripts/test.sh` - full repo verification if time/resources allow.

## DoD Checklist

- [ ] Tests pass or failures are documented.
- [ ] No cross-household data leaks verified by tests/review.
- [ ] No mutation before confirmation approval.
- [ ] DecisionLog traceability verified.
- [ ] Contracts/docs updated for behavior changes.
- [ ] ADR/diagram/index updates complete if required.
- [ ] Review gate recorded before Gate D.

## Risks

| Risk | Mitigation |
| --- | --- |
| Confirmation executes early | `DecisionResult.Confirm` handler creates pending state only; tests assert no mutation |
| Natural command degraded fallback guesses action | Manual/fallback provider returns non-mutating clarify/reject for natural command |
| Scope grows into mobile UX | Mobile/web files forbidden by Gate C |
| Pending state under-modeled | ADR + migration + explicit repository/service or HOLD |
| Existing structured commands regress | Run existing command and AI Platform integration tests |
| Provider confirm payload is unsupported | Reject/clarify safely, preserve raw payload |

## Rollback

- Revert the APPLY commit(s).
- If migration creates a pending confirmation table, rollback by reverting code and applying a follow-up migration only if already deployed; before deployment, normal branch revert is sufficient.
- No external provider or mobile rollback is needed because those scopes are forbidden.

## Prompt Pack

- PLAN: `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-NEEDS-CONFIRMATION-BACKEND-CONTRACT/prompt-plan.md`
- APPLY: pending Gate C
- REVIEW: separate read-only review gate; do not create `prompt-review.md`
