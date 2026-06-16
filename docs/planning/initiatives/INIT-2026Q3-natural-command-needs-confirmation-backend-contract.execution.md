# INIT-2026Q3 Natural Command + Needs Confirmation Backend Contract Execution Notes

**Status:** Gate A GO; Gate B GO; artifact gate GO/HOLD split; Gate C GO;
review gate GO; Gate D GO for backend contract foundation and approval/cancel
lifecycle. Initiative backend scope complete; client/product readiness remains
separately gated.
**Date:** 2026-06-16
**Initiative:** `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md`
**Roadmap:** `docs/planning/strategy/roadmap.md`
**Delegation:** Human gates for this initiative are delegated to Codex. Every
GO / NO-GO / HOLD decision must be recorded here with evidence, risks, and
rationale.

---

## Intake Summary

| Field | Decision |
| --- | --- |
| Request type | Activate current roadmap initiative and proceed through HomeTusk planning pipeline |
| Scope anchor | `INIT-2026Q3-natural-command-needs-confirmation-backend-contract` |
| Workflow path | `intake -> planning -> artifact gate -> workpack -> Codex PLAN -> Gate C -> APPLY or HOLD -> review gate -> Gate D` |
| Change type | contract-change, backend feature, AI safety, traceability, docs/process |
| Work level | initiative-level backend contract implementation |
| Primary boundary | HomeTusk owns public API, execution authority, pending confirmation state, guardrails, and audit trail |
| Runtime posture | Backend/runtime changes are expected only after Gate C GO |
| Public API posture | Accepted Commands API changes are expected only after contract gate and Gate C GO |

## Sources of Truth Read

| Artifact | Path |
| --- | --- |
| Active repo rules | `AGENTS.md` |
| Workflow | `docs/CODEX-WORKFLOW.md` |
| Product goal | `docs/planning/strategy/product-goal.md` |
| Roadmap | `docs/planning/strategy/roadmap.md` |
| MVP release scope | `docs/planning/releases/MVP.md` |
| DoR / DoD | `docs/_governance/dor.md`, `docs/_governance/dod.md` |
| Prior contract spike initiative | `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.md` |
| Prior contract spike execution | `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.execution.md` |
| Prior spike workpack and Gate D | `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/**` |
| Draft natural command contract package | `docs/research/ai-command-capabilities/natural-command-contract-spike/**` |
| Current Commands contract | `docs/contracts/http/commands.openapi.yaml` |
| Contract index | `docs/_indexes/contracts-index.md` |
| Service catalog | `docs/architecture/service-catalog.md` |
| Current backend command DTOs/models | `services/backend/src/main/java/com/hometusk/commands/dto/CommandRequest.java`, `services/backend/src/main/java/com/hometusk/commands/dto/CommandResponse.java`, `services/backend/src/main/java/com/hometusk/commands/domain/CommandType.java`, `services/backend/src/main/java/com/hometusk/commands/domain/CommandStatus.java` |
| Current DecisionLog and mapper | `services/backend/src/main/java/com/hometusk/commands/domain/DecisionLog.java`, `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionResponseMapper.java` |
| Current command service | `services/backend/src/main/java/com/hometusk/commands/service/CommandService.java` |

## Triage Classification

| Flag | Value | Evidence |
| --- | --- | --- |
| `contract_impact` | yes | Public Commands API must add `natural_command` and `needs_confirmation` if APPLY proceeds |
| `backend_impact` | yes | `CommandType`, request validation, response model, provider confirm mapping, and tests are expected |
| `data_impact` | yes/maybe | Spike requires explicit pending confirmation state rather than DecisionLog-only state |
| `mobile_impact` | no | Mobile/web UI remains out of scope; mobile files are read-only context |
| `ai_platform_impact` | no | AI Platform is read-only input; HomeTusk adapts to upstream |
| `security_sensitive` | yes | Confirmation, household scope, approval actor, no-mutation guarantees |
| `traceability_critical` | yes | Command lifecycle, raw provider payload, mapped outcome, confirmation lifecycle |
| `adr_needed` | yes/maybe | Required if a pending confirmation persistence/lifecycle model is introduced |
| `diagrams_needed` | maybe | Sequence/state diagram may reduce implementation and review risk |
| `cross_repo` | yes-read-only | Provider evidence only; no external repo writes |

## Scope Boundary Preserved

In scope:

- Roadmap activation of the backend contract implementation initiative.
- Initiative and execution notes.
- Workpack and read-only PLAN for backend/API contract implementation.
- Contract gate decision before accepted public contract changes.
- Backend changes only if Gate C records GO with exact files.
- DecisionLog traceability and no-mutation confirmation behavior.

Out of scope:

- Mobile/web UI.
- `answered`.
- Direct mobile/web to AI Platform.
- AI Platform repository changes.
- Production rollout/config enablement.
- Local LLM implementation.
- Broad autonomous household planning.

## Gate A Decision - GO

**Decision:** GO for making
`INIT-2026Q3-natural-command-needs-confirmation-backend-contract` the current
roadmap initiative.

**Evidence:**

- The roadmap ranked "HomeTusk natural_command + needs_confirmation backend
  contract implementation" as recommended next / LIMITED-GO.
- The prior contract spike closed on 2026-06-16 with Gate D GO and explicit
  LIMITED-GO for this separate backend implementation initiative.
- Current backend code still supports only `CREATE_TASK` and `COMPLETE_TASK`.
- Current public command responses still lack `needs_confirmation`.
- Current AI Platform provider `confirm` is mapped to
  `AI_CONFIRMATION_UNSUPPORTED`, which is safe but not the target product
  contract.
- The new initiative preserves anti-scope: no mobile UX, no `answered`, no
  direct client-to-provider calls, no AI Platform writes, no production rollout.

**Rationale:**

This is the correct next roadmap step because the contract spike produced a
bounded backend implementation recommendation. Keeping the work backend/API
first prevents mobile UX from depending on an unimplemented confirmation state.

**Risks accepted:**

- Provider eval still has non-blocker semantic mismatch buckets from earlier
  evidence.
- The pending confirmation data model may require ADR/diagram work before APPLY.
- Scope must stay sliceable; approve/cancel execution may become LIMITED-GO or a
  follow-up if PLAN finds too much implementation risk.

## Gate B Decision - GO

**Decision:** GO for creating one initiative-level workpack and running a
read-only Codex PLAN.

**Workpack path:**

```text
docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-NEEDS-CONFIRMATION-BACKEND-CONTRACT/
```

**Committed planning scope:**

- Exact contract/backend/data/doc files to change.
- Contract, ADR, diagram, security, traceability, and data impact decisions.
- Acceptance criteria for natural command validation and provider confirm
  no-mutation behavior.
- Verification commands and rollback strategy.
- Gate C GO / NO-GO / HOLD decision before any runtime APPLY.

**DoR evidence:**

- Title, owner, target milestone, sources, outcome, scope, out-of-scope, flags,
  risks, and exit criteria are present in the initiative.
- Related draft contract artifacts exist from the prior spike.
- Current code/contract gaps have been verified read-only.

**Rationale:**

The initiative is Ready for workpack and PLAN because the prior spike supplies
the contract direction and this initiative narrows the implementation boundary to
backend/API contract foundation only.

## Artifact Gate Decision

### Contract Gate

**Decision:** GO for additive accepted Commands API contract changes covering
`natural_command`, `needs_confirmation`, and pending confirmation creation;
HOLD for approve/cancel endpoints and mobile/web client contracts.

**Evidence:**

- The prior spike produced draft request/response/confirmation/lifecycle
  artifacts.
- Current public contract lacks `natural_command` and `needs_confirmation`.
- Existing structured command clients can remain compatible because the planned
  change is additive under `POST /api/v1/commands`.
- Approval/cancel idempotency and stale execution rules need a separate
  workpack before accepted contract implementation.

### ADR / Diagram Gate

**Decision:** GO.

**Evidence:**

- Persistent pending confirmation state is a data-model and command-lifecycle
  decision.
- Provider `confirm` changes command flow from unsupported rejection to pending
  confirmation creation.

### Security / Traceability Gate

**Decision:** GO for limited APPLY with mandatory no-mutation and DecisionLog
tests.

**Future implementation must prove:**

- no mutation happens before explicit approval;
- confirmation state is household-scoped;
- approval/cancel actors are auditable;
- provider payloads are preserved in DecisionLog;
- raw audio is not stored;
- unsupported AI output clarifies or rejects safely.

## Codex PLAN Findings

Detailed findings are recorded in:

```text
docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-NEEDS-CONFIRMATION-BACKEND-CONTRACT/plan-findings.md
```

Key findings:

- `POST /api/v1/commands` remains the correct compatibility boundary.
- `natural_command` can use existing AI Platform request shape through
  `payload.text`, but must add its own schema validation.
- `ManualDecisionProvider` cannot safely execute natural commands; degraded mode
  must return a deterministic non-mutating clarification/rejection.
- `CommandType` and `CommandStatus` use lowercase enum converters, so check
  constraints must be updated by migration.
- `DecisionLog` can record raw payloads, but pending confirmation requires a new
  source-of-truth entity.
- First APPLY should not include approve/cancel execution; that is a separate
  lifecycle/idempotency slice.

## Gate C Decision - GO

**Decision:** GO for limited APPLY.

**Approved files:** See
`docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-NEEDS-CONFIRMATION-BACKEND-CONTRACT/plan-findings.md`.

**Approved scope:**

- Accepted Commands API docs for `natural_command` and `needs_confirmation`.
- ADR and sequence diagram for pending confirmation state.
- Backend natural command schema/type support.
- Persistent pending confirmation state.
- Provider `confirm -> needs_confirmation` for supported payloads.
- Guardrail pre-check as proposal.
- DecisionLog traceability and tests.

**Held scope:**

- approve/cancel endpoints;
- approval idempotency;
- stale confirmation execution;
- expiry scheduler;
- mobile/web UI;
- `answered`;
- production rollout/config.

**Rationale:**

The limited slice creates the backend contract state mobile/web can later depend
on while preserving the strongest safety invariant: provider `confirm` never
mutates state before explicit approval.

## PLAN Questions Answered

1. Should first APPLY include approve/cancel execution, or only pending
   confirmation creation and response contract? **Answered:** only pending
   confirmation creation and response contract.
2. What exact persistence model is smallest and safe for pending confirmations?
   **Answered:** `command_confirmations` table with HomeTusk-owned id,
   command/household/initiator scope, provider trace fields, status, summary,
   reason/risk labels, proposed actions, and expiry.
3. Which integration tests can assert no domain mutation for provider confirm?
   **Answered:** `AiPlatformIntegrationTest.ConfirmScenario` asserts
   `needs_confirmation`, no task/shopping mutation, confirmation row creation,
   and DecisionLog raw payload.
4. Does the pending confirmation model require a full ADR, ADR-lite, or only
   service-catalog/diagram updates? **Answered:** full ADR-022 plus sequence
   diagram and index updates.
5. How should idempotency apply to confirmation approval in the first backend
   slice? **Answered:** held for the next approval/cancel lifecycle workpack.

## APPLY Evidence

**Decision:** APPLY completed for the limited backend/API contract foundation.

**Delivered:**

- roadmap NOW updated to this initiative;
- accepted Commands API documents `type=natural_command` and
  `status=needs_confirmation`;
- ADR-022 and natural command confirmation sequence diagram added;
- backend `NATURAL_COMMAND` command type and schema validation added;
- AI Platform request capabilities now advertise `confirm`;
- supported provider `confirm` maps to non-mutating `needs_confirmation`;
- unsupported provider confirmation payload rejects safely;
- `command_confirmations` is the HomeTusk-owned pending confirmation source of
  truth;
- `DecisionLog` preserves raw provider payload and mapped confirmation outcome;
- manual/degraded natural command path returns deterministic non-mutating
  clarification.

**Held by design:**

- approve/cancel endpoints;
- approval idempotency and stale execution rules;
- expiry scheduler;
- mobile/web UI;
- `answered`;
- production rollout/config.

**Scope control:**

- No files under `clients/**` changed.
- No files under `docs/integration/ai-platform/v1/upstream/**` changed.
- No AI Platform repo writes were made.
- Provider `confirm` does not execute actions before explicit approval.

**Gate C deviation evidence:**

Three implementation-local files not listed in the exact PLAN allowed-file list
were required and approved by Gate C addendum:

- `Command.java` for the `NEEDS_CONFIRMATION` state transition;
- `CommandRequest.java` for approved `natural_command` parsing;
- `CommandConfirmationStatusConverter.java` for the confirmation status
  persistence pattern.

## Verification Evidence

Command run from `services/backend`:

```text
java -cp .\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain spotlessCheck test --tests "com.hometusk.commands.pipeline.decision.client.AiPlatformDecisionAdapterTest" --tests "com.hometusk.integration.CommandPipelineTest" --tests "com.hometusk.integration.aiplatform.AiPlatformIntegrationTest" --console=plain
```

Result: **BUILD SUCCESSFUL** on 2026-06-16.

Observed targeted test evidence:

- `AiPlatformDecisionAdapterTest`: 8 tests, 0 failures.
- `CommandPipelineTest`: 24 tests, 0 failures.
- `AiPlatformIntegrationTest`: 15 scenario tests, 0 failures.
- Flyway applied migrations through `V033__add_natural_command_confirmations.sql`
  in Testcontainers.
- `spotlessCheck` passed after `spotlessApply`.

Not run:

- `./scripts/test.sh` was not run because this Windows environment has no
  available Unix shell for the repo script. The backend wrapper jar command above
  was used instead.

## Review Gate Decision - GO

**Review artifact:** `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-NEEDS-CONFIRMATION-BACKEND-CONTRACT/review-gate.md`

**Decision:** GO for the limited backend contract foundation.

**Must-fix findings:** none.

**Rationale:**

- Implementation preserves the initiative's no-mutation invariant for provider
  `confirm`.
- Public contract changes are additive for existing structured command clients.
- Pending confirmation has explicit HomeTusk-owned state rather than relying on
  DecisionLog as a state store.
- Tests cover mapper behavior, natural command validation, degraded behavior,
  provider confirm no-mutation, pending confirmation persistence, and raw
  provider payload traceability.

## Gate D Decision - GO / LIMITED-GO

**Decision:** GO for the completed limited backend/API contract foundation.
LIMITED-GO for the broader confirmation product flow until approve/cancel
lifecycle is implemented and reviewed.

**Evidence:**

- Roadmap, initiative, workpack, contract, ADR, diagram, service catalog, and
  indexes were updated.
- Backend supports `natural_command` request validation and safe degraded mode.
- AI Platform `confirm` is advertised and mapped to `needs_confirmation` for
  supported proposed actions.
- Pending confirmations are persisted in `command_confirmations`.
- DecisionLog records raw provider payload and mapped confirmation evidence.
- Targeted backend checks passed.

**Residual risks:**

- There is no approve/cancel endpoint yet, so pending confirmations cannot be
  executed or cancelled by clients.
- Approval idempotency, stale/expired execution rules, and expiry scheduling are
  still unimplemented.
- Mobile/web UI remains blocked until the approval/cancel lifecycle contract is
  ready.
- Full repository test script was not run in this Windows shell environment.
- Provider semantic mismatch buckets from earlier acceptance evidence remain
  non-blocking input risk and should stay under review before production rollout.

**Next recommended action:**

Create the next workpack inside this initiative for the confirmation
approve/cancel lifecycle: accepted contract delta, authorization/idempotency
rules, stale/expired handling, lifecycle audit evidence, and tests. Mobile AI
Command UX v1 should wait until that workpack reaches Gate D GO.

## Continuation: Approval / Cancel Lifecycle Slice

**Workpack:** `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-CONFIRMATION-APPROVAL-LIFECYCLE-BACKEND-CONTRACT/workpack.md`

### Continuation Intake

| Field | Decision |
| --- | --- |
| Request type | Continue current roadmap initiative after backend foundation Gate D |
| Change type | contract-change, backend lifecycle, data, security, traceability |
| Scope | approve/cancel lifecycle for existing pending confirmations |
| Out of scope | mobile/web UI, `answered`, expiry scheduler, AI Platform writes, production rollout |
| Primary invariant | no mutation unless an authorized initiator explicitly approves |

### Continuation Artifact Gate

**Contract gate:** GO for additive approve/cancel endpoints under the existing
Commands API.

**ADR gate:** GO to update ADR-022; no new ADR required because this implements
the lifecycle already deferred by ADR-022.

**Diagram gate:** GO to update the existing natural command confirmation
sequence diagram.

**Security / traceability gate:** GO with initiator-only authorization,
pessimistic confirmation locking, lifecycle DecisionLog evidence, and tests.

### Continuation Gate C

**Decision:** GO for limited APPLY.

**Evidence:**

- Parent Gate D left approve/cancel as the next required workpack.
- Draft lifecycle contract already defines approval, cancel, expiry, and
  terminal-state semantics.
- Existing backend has a persisted `command_confirmations` anchor and action
  executor/guardrails pipeline.

**Rationale:**

The slice moves the initiative toward a complete backend confirmation flow
without expanding into UI, provider changes, or broad approval policy. The
strict initiator-only policy is safe and reversible in a later gated policy
workpack.

### Continuation Gate C Addendum

**Decision:** GO for adding
`services/backend/src/main/java/com/hometusk/shared/exception/GlobalExceptionHandler.java`
to the approved APPLY file set.

**Rationale:** The lifecycle introduced new confirmation error codes and needed
existing error-handler routing for 404/409 public API semantics. This is an
implementation-local contract alignment change and does not expand behavior
outside the approved lifecycle scope.

## Continuation APPLY Evidence

**Decision:** APPLY completed for the backend approval/cancel lifecycle.

**Delivered:**

- accepted Commands API approve/cancel endpoints;
- lifecycle persistence fields for approval, cancellation, expiry, execution
  result, and failure metadata;
- initiator-only approval/cancel authorization;
- pessimistic confirmation locking for lifecycle mutations;
- lazy expiry on approve with no domain mutation;
- guardrails revalidation before approved execution;
- execution through the existing `ActionExecutor`;
- terminal-state idempotent replay for repeated approve/cancel;
- lifecycle DecisionLog evidence for approval, cancellation, expiry, and
  guardrail rejection paths;
- integration tests for approve once, repeated approve, cancel no-mutation,
  repeated cancel, non-initiator denial, and expired approval.

**Held by design:**

- mobile/web UI;
- `answered`;
- expiry scheduler;
- broader household approval policy;
- editing pending proposed actions;
- production rollout/config;
- AI Platform writes or upstream snapshot edits.

**Scope control:**

- No files under `clients/**` changed.
- No files under `docs/integration/ai-platform/v1/upstream/**` changed.
- No AI Platform repository writes were made.
- Provider `confirm` still cannot mutate before explicit HomeTusk approval.

## Continuation Verification Evidence

Commands run from `services/backend`:

```text
java -cp .\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain spotlessApply --console=plain
java -cp .\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain spotlessCheck --console=plain
java -cp .\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests "com.hometusk.integration.CommandPipelineTest" --tests "com.hometusk.commands.pipeline.decision.client.AiPlatformDecisionAdapterTest" --tests "com.hometusk.integration.aiplatform.AiPlatformIntegrationTest" --console=plain
java -cp .\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --console=plain
```

Result: **BUILD SUCCESSFUL** on 2026-06-16.

Observed targeted evidence:

- `AiPlatformDecisionAdapterTest`: provider mapping remains compatible.
- `CommandPipelineTest`: existing command pipeline behavior remains compatible.
- `AiPlatformIntegrationTest`: provider confirm no-mutation and lifecycle
  approval/cancel/expiry scenarios passed.
- Flyway validated and applied migrations through
  `V034__add_confirmation_lifecycle_fields.sql` in Testcontainers.
- Full backend `test` also passed after the targeted lifecycle run.

Not run:

- `./scripts/test.sh` was not run because this Windows environment has no
  available Unix shell for the repo script. The backend wrapper jar command above
  was used instead.

## Continuation Review Gate Decision - GO

**Review artifact:** `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-CONFIRMATION-APPROVAL-LIFECYCLE-BACKEND-CONTRACT/review-gate.md`

**Decision:** GO for the backend approval/cancel lifecycle.

**Must-fix findings:** none.

**Rationale:**

- Approval is initiator-only and revalidates guardrails before mutation.
- Repeated approval/cancel requests are terminal-state replay and do not duplicate
  domain mutations.
- Cancellation and expiry paths are auditable and mutation-safe.
- The implementation stays inside the existing Commands API boundary.

## Continuation Gate D Decision - GO / LIMITED-GO

**Gate D artifact:** `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-CONFIRMATION-APPROVAL-LIFECYCLE-BACKEND-CONTRACT/gate-d.md`

**Decision:** GO for the completed backend approval/cancel lifecycle. LIMITED-GO
for broader product/client readiness until mobile/web UI, expiry scheduling if
needed, and production rollout are planned through separate gates.

**Evidence:**

- Accepted Commands API documents approve/cancel lifecycle endpoints.
- Backend executes approved stored proposed actions once after current guardrails
  revalidation.
- Cancel, expiry, non-initiator denial, and terminal replay are mutation-safe.
- DecisionLog records lifecycle audit evidence.
- Targeted backend checks passed.

**Residual risks:**

- No mobile/web confirmation UI exists yet.
- Expiry scheduler remains out of scope.
- `answered` remains blocked.
- Broader household approval policy remains unapproved.
- Production rollout/config remains unapproved.
- Full `./scripts/test.sh` was not run in this Windows shell environment.
- Full backend Gradle `test` was run and passed with the wrapper jar command.

**Next recommended action:**

Plan Mobile AI Command UX v1 as a separate gated initiative/workpack using the
accepted backend contract. Keep expiry scheduler, broader approval policy,
`answered`, and production rollout as separately gated follow-ups.
