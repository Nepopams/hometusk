# Gate D - INIT-2026Q3 Natural Command Confirmation Approval Lifecycle Backend Contract

**Date:** 2026-06-16
**Decision:** GO / LIMITED-GO

## Decision

GO for the completed backend approval/cancel lifecycle inside
`INIT-2026Q3-natural-command-needs-confirmation-backend-contract`.

LIMITED-GO for broader product readiness until client UI, expiry scheduling, and
production rollout are planned through separate gates.

## Evidence

- Accepted Commands API now documents approve/cancel endpoints for
  HomeTusk-owned pending confirmations.
- Confirmation lifecycle fields are persisted by
  `V034__add_confirmation_lifecycle_fields.sql`.
- `CommandConfirmation` records approval, cancellation, expiry, execution
  result, and failure metadata.
- `CommandService` implements initiator-only approval/cancel, pessimistic
  confirmation locking, lazy expiry on approve, guardrails revalidation, action
  execution through the existing `ActionExecutor`, terminal-state replay, and
  lifecycle DecisionLog entries.
- `CommandController` exposes the lifecycle under the existing Commands API
  boundary.
- ADR-022, the sequence diagram, service catalog, and contract index were updated
  for the lifecycle.
- Integration tests cover approve execution once, repeated approval, cancel
  without mutation, repeated cancel, non-initiator denial, and expired approval.

## Checks

Commands run from `services/backend`:

```text
java -cp .\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain spotlessApply --console=plain
java -cp .\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain spotlessCheck --console=plain
java -cp .\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests "com.hometusk.integration.CommandPipelineTest" --tests "com.hometusk.commands.pipeline.decision.client.AiPlatformDecisionAdapterTest" --tests "com.hometusk.integration.aiplatform.AiPlatformIntegrationTest" --console=plain
java -cp .\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --console=plain
```

Result: **BUILD SUCCESSFUL**.

## Residual Risks

- No mobile/web confirmation UI exists yet.
- Expiry scheduler is still out of scope; expiry is lazy on approve in this
  slice.
- `answered` remains blocked.
- Broader household approval policy remains unapproved; only original initiator
  approval/cancel is accepted.
- Production rollout/config remains unapproved.
- Full `./scripts/test.sh` was not run because this Windows environment does not
  provide a Unix shell for the repo script.

## Rationale

The delivered slice completes the backend-owned confirmation loop without
weakening HomeTusk's execution authority. Provider `confirm` creates a pending
proposal, and only an authorized initiator approval can execute it after current
guardrails revalidation. Cancellation, expiry, and repeated terminal requests are
auditable and mutation-safe.

## Next Recommended Action

Plan the next gated initiative/workpack for Mobile AI Command UX v1 using the
accepted backend contract. Keep expiry scheduler, broader approval policy,
`answered`, and production rollout as separately gated follow-ups.
