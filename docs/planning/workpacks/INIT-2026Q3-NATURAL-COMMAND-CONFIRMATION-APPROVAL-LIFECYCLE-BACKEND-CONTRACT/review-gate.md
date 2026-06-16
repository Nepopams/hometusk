# Review Gate - INIT-2026Q3 Natural Command Confirmation Approval Lifecycle Backend Contract

**Date:** 2026-06-16
**Mode:** Read-only review of the approval/cancel lifecycle APPLY.
**Decision:** GO

## Scope Reviewed

- Accepted Commands API approve/cancel endpoints.
- Confirmation lifecycle persistence fields and migration.
- Domain lifecycle transitions and repository locking.
- `CommandController` and `CommandService` approval/cancel behavior.
- Initiator-only authorization and terminal-state idempotency.
- Lazy expiry on approve and guardrails revalidation before execution.
- DecisionLog lifecycle evidence.
- Integration tests and targeted backend verification.

## Critical Invariants Re-checked

- AI Platform output remains a suggestion, not source of truth.
- Provider `confirm` still does not mutate domain state before explicit approval.
- Only the original command initiator can approve or cancel in this slice.
- Stored proposed actions are revalidated through guardrails before mutation.
- Expired, cancelled, rejected, or opposite-terminal confirmations cannot execute.
- DecisionLog records lifecycle evidence; it is not used as lifecycle state.
- Mobile/web UI, `answered`, AI Platform writes, expiry scheduler, and
  production rollout remain out of scope.

## Findings

### Must-fix

None.

### Should-fix / Follow-up

- Add an expiry scheduler only through a separate gated hardening slice.
- Add mobile/web confirmation UX through its own initiative/workpack now that
  backend lifecycle is available.
- Revisit broader household approval policy only with explicit product/ADR
  rationale.
- Keep provider semantic mismatch evidence under review before production
  rollout.

## Evidence

- `POST /api/v1/commands/{commandId}/confirmations/{confirmationId}/approve`
  executes stored supported actions only after initiator authorization,
  pessimistic confirmation locking, pending-state checks, lazy expiry checks,
  and guardrails revalidation.
- Repeated approval after execution returns stored execution result with
  `idempotentReplay=true` and does not duplicate mutation.
- `POST /api/v1/commands/{commandId}/confirmations/{confirmationId}/cancel`
  marks pending confirmations cancelled, records actor/reason, and never
  executes actions.
- Repeated cancel after cancellation returns `idempotentReplay=true`.
- Non-initiator approval is rejected with access denied and no mutation.
- Expired approval marks confirmation `EXPIRED`, records audit evidence, and
  returns a controlled rejected response with `CONFIRMATION_EXPIRED`.
- Contract, ADR-022, sequence diagram, service catalog, and contract index were
  updated for the accepted lifecycle.

## Verification

Commands run from `services/backend`:

```text
java -cp .\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain spotlessApply --console=plain
java -cp .\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain spotlessCheck --console=plain
java -cp .\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests "com.hometusk.integration.CommandPipelineTest" --tests "com.hometusk.commands.pipeline.decision.client.AiPlatformDecisionAdapterTest" --tests "com.hometusk.integration.aiplatform.AiPlatformIntegrationTest" --console=plain
java -cp .\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --console=plain
```

Result: **BUILD SUCCESSFUL**.

Targeted test evidence:

- `AiPlatformDecisionAdapterTest`: provider mapping remains compatible.
- `CommandPipelineTest`: existing command pipeline behavior remains compatible.
- `AiPlatformIntegrationTest`: provider confirm, approve once, repeated approve,
  cancel, repeated cancel, non-initiator denial, and expired approval scenarios
  passed.
- Flyway validated and applied migrations through
  `V034__add_confirmation_lifecycle_fields.sql` in Testcontainers.
- Full backend `test` also passed after the targeted lifecycle run.

## Review Decision

GO for Gate D on the backend confirmation approval/cancel lifecycle.

Gate D should record LIMITED-GO for product/client readiness until mobile/web
confirmation UI, expiry scheduling if needed, and production rollout are planned
through separate gates.
