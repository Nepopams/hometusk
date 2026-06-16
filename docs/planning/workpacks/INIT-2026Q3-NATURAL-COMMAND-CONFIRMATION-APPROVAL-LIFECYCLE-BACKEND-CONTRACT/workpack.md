# Workpack: INIT-2026Q3 Natural Command Confirmation Approval Lifecycle Backend Contract

## Metadata

- Initiative: `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md`
- Execution notes: `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.execution.md`
- Parent foundation workpack: `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-NEEDS-CONFIRMATION-BACKEND-CONTRACT/`
- Date: 2026-06-16
- Status: Completed; review gate GO and Gate D GO recorded

## Outcome

HomeTusk can approve or cancel a pending `needs_confirmation` proposal through
backend-owned endpoints without introducing mobile/web UI or AI Platform writes.

Approved confirmation execution:

```text
pending confirmation -> authorized approval -> revalidate guardrails -> execute supported actions -> audit -> terminal status
```

Cancel flow:

```text
pending confirmation -> authorized cancel -> no mutation -> audit -> terminal status
```

## Sources of Truth

- `AGENTS.md`
- `docs/CODEX-WORKFLOW.md`
- `docs/planning/strategy/product-goal.md`
- `docs/planning/strategy/roadmap.md`
- `docs/planning/releases/MVP.md`
- `docs/_governance/dor.md`
- `docs/_governance/dod.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.execution.md`
- `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-NEEDS-CONFIRMATION-BACKEND-CONTRACT/gate-d.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/confirmation-lifecycle-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/openapi-delta-draft.yaml`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/adr/022-pending-command-confirmation-state.md`
- `docs/diagrams/sequence-natural-command-needs-confirmation.md`
- `services/backend/src/main/java/com/hometusk/commands/**`

## In Scope

- Accepted Commands API contract delta for:
  - `POST /api/v1/commands/{commandId}/confirmations/{confirmationId}/approve`
  - `POST /api/v1/commands/{commandId}/confirmations/{confirmationId}/cancel`
- Backend endpoints in `CommandController`.
- `CommandService` approval/cancel lifecycle methods.
- Confirmation lifecycle persistence fields for approval/cancel/expiry/execution result.
- Initiator-only approval/cancel authorization for this slice.
- Lazy expiry on approve.
- Re-run guardrails before executing approved proposed actions.
- Idempotent terminal behavior for repeated approve/cancel calls.
- DecisionLog lifecycle evidence.
- Integration tests for approve execution, cancel no-mutation, unauthorized actor, and expired approval.

## Out of Scope

- Mobile/web UI.
- `answered`.
- Direct mobile/web to AI Platform.
- AI Platform repo or upstream snapshot changes.
- Expiry scheduler.
- Production rollout/config.
- Broad approval policy beyond original initiator.
- Editing pending proposal content before approval.

## Acceptance Criteria

- Given a pending confirmation created by provider `confirm`, when the initiator
  approves it, then supported proposed actions execute once and the confirmation
  reaches `EXECUTED`.
- Given the same approval is repeated after execution, then no duplicate domain
  mutation happens and an idempotent executed response is returned.
- Given a pending confirmation, when the initiator cancels it, then no domain
  mutation happens and the confirmation reaches `CANCELLED`.
- Given the same cancel is repeated, then an idempotent cancelled response is
  returned.
- Given a different household member tries to approve/cancel the initiator's
  confirmation, then the request is rejected with access denied and no mutation.
- Given a pending confirmation is expired, when approval is attempted, then it is
  marked `EXPIRED`, no mutation happens, and a controlled rejected approval
  response returns `CONFIRMATION_EXPIRED`.
- DecisionLog records approval/cancel/expiry lifecycle evidence.
- Existing command execution and provider confirm no-mutation tests remain
  compatible.

## Files Approved For APPLY

- `docs/planning/strategy/roadmap.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.execution.md`
- `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-CONFIRMATION-APPROVAL-LIFECYCLE-BACKEND-CONTRACT/**`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/_indexes/contracts-index.md`
- `docs/architecture/service-catalog.md`
- `docs/adr/022-pending-command-confirmation-state.md`
- `docs/diagrams/sequence-natural-command-needs-confirmation.md`
- `services/backend/src/main/java/com/hometusk/commands/api/CommandController.java`
- `services/backend/src/main/java/com/hometusk/commands/domain/CommandConfirmation.java`
- `services/backend/src/main/java/com/hometusk/commands/domain/CommandConfirmationStatus.java`
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandConfirmationApprovalResponse.java`
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandConfirmationCancelRequest.java`
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandConfirmationCancelResponse.java`
- `services/backend/src/main/java/com/hometusk/commands/repository/CommandConfirmationRepository.java`
- `services/backend/src/main/java/com/hometusk/commands/service/CommandService.java`
- `services/backend/src/main/java/com/hometusk/shared/exception/ErrorCode.java`
- `services/backend/src/main/java/com/hometusk/shared/exception/GlobalExceptionHandler.java`
- `services/backend/src/main/resources/db/migration/V034__add_confirmation_lifecycle_fields.sql`
- `services/backend/src/test/java/com/hometusk/integration/aiplatform/AiPlatformIntegrationTest.java`

## Forbidden Files

- `clients/**`
- `docs/integration/ai-platform/v1/upstream/**`
- external AI Platform repositories
- production deployment/config files

## Implementation Notes

- Use the existing command boundary; do not create a separate AI endpoint.
- Approval/cancel authorization is intentionally strict: original initiator only.
- `Idempotency-Key` may be accepted/documented, but this slice relies on
  terminal-state idempotency rather than a new idempotency table.
- Reuse existing `ActionExecutor` and guardrails; do not duplicate domain
  mutation code.
- Add pessimistic locking on confirmation load for approve/cancel to avoid
  duplicate execution.
- Store execution result JSON on `command_confirmations` so repeated approval can
  return a stable response.

## Verification Commands

Run from `services/backend`:

```text
java -cp .\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain spotlessApply --console=plain
java -cp .\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain spotlessCheck --console=plain
java -cp .\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests "com.hometusk.integration.CommandPipelineTest" --tests "com.hometusk.commands.pipeline.decision.client.AiPlatformDecisionAdapterTest" --tests "com.hometusk.integration.aiplatform.AiPlatformIntegrationTest" --console=plain
java -cp .\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --console=plain
```

Run `./scripts/test.sh` only if a Unix shell is available.

## Rollback

Before deployment, revert this branch's contract/code/migration changes. After
deployment, rollback requires a forward migration that disables endpoints and
preserves `command_confirmations` audit rows.

## STOP Conditions

Stop and record HOLD if:

- approval requires mobile/web UI changes;
- execution requires AI Platform writes;
- proposed actions need mutation before explicit approval;
- authorization cannot be enforced with current user/household data;
- idempotent repeated approval cannot be made mutation-safe.
