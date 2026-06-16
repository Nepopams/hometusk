# Review Gate - INIT-2026Q3 Natural Command + Needs Confirmation Backend Contract

**Date:** 2026-06-16
**Mode:** Read-only review of the limited backend/API contract APPLY.
**Decision:** GO

## Scope Reviewed

- Roadmap and initiative activation.
- Workpack, PLAN, Gate C, Gate C addendum, and checklist.
- Accepted Commands API additive contract delta.
- ADR-022 and sequence diagram.
- Backend natural command request support, provider confirm mapping, pending
  confirmation persistence, DecisionLog traceability, and tests.

## Critical Invariants Re-checked

- AI Platform output remains a suggestion, not source of truth.
- Provider `confirm` never executes domain mutations before explicit HomeTusk
  approval.
- `DecisionLog` is audit evidence, not the pending confirmation state store.
- Unsupported provider confirmation output rejects safely.
- Existing structured commands remain compatible.
- Mobile/web UI, `answered`, AI Platform repo writes, and production rollout are
  out of scope.

## Findings

### Must-fix

None.

### Should-fix / Follow-up

- Implement approve/cancel lifecycle in a separate Gate C-approved workpack:
  authorization, idempotency, stale/expired behavior, execution, cancellation,
  and lifecycle audit evidence.
- Add expiry scheduler only after lifecycle semantics are accepted.
- Keep provider semantic mismatch evidence under review before production
  rollout.

## Evidence

- `type=natural_command` is additive under existing `POST /api/v1/commands`.
- `status=needs_confirmation` is a first-class response and does not overload
  `needs_input`.
- `command_confirmations` stores HomeTusk-owned pending confirmation state with
  command, household, initiator, provider trace fields, proposed actions, status,
  and expiry.
- `AiDecisionRequest` advertises `confirm` capability.
- `AiDecisionResponseMapper` maps supported provider `confirm` payloads to
  `DecisionResult.Confirm` and unsupported actions to safe rejection.
- `CommandService` guardrail-checks provider confirm as a proposal, creates
  pending confirmation, writes DecisionLog, and returns `needs_confirmation`
  without executing actions.
- `AiPlatformIntegrationTest.ConfirmScenario` proves `needs_confirmation`,
  pending confirmation persistence, DecisionLog raw payload preservation, and no
  task/shopping mutation.

## Verification

Command run from `services/backend`:

```text
java -cp .\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain spotlessCheck test --tests "com.hometusk.commands.pipeline.decision.client.AiPlatformDecisionAdapterTest" --tests "com.hometusk.integration.CommandPipelineTest" --tests "com.hometusk.integration.aiplatform.AiPlatformIntegrationTest" --console=plain
```

Result: **BUILD SUCCESSFUL**.

Targeted test result summary:

- `AiPlatformDecisionAdapterTest`: 8 tests, 0 failures.
- `CommandPipelineTest`: 24 tests, 0 failures.
- `AiPlatformIntegrationTest`: 15 scenario tests, 0 failures.

## Review Decision

GO for Gate D on the limited backend/API contract foundation.

Gate D should record LIMITED-GO for full confirmation product readiness until
approve/cancel lifecycle, expiry behavior, and client-facing lifecycle semantics
are implemented and reviewed.
