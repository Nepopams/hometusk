# Gate D - INIT-2026Q3 Natural Command + Needs Confirmation Backend Contract

**Date:** 2026-06-16
**Decision:** GO / LIMITED-GO

## Decision

GO for the completed limited backend/API contract foundation.

LIMITED-GO for the broader confirmation product flow until the approval/cancel
lifecycle is implemented through a separate workpack and review gate.

## Evidence

- Roadmap NOW points to
  `INIT-2026Q3-natural-command-needs-confirmation-backend-contract`.
- Initiative and execution notes record delegated Gate A, Gate B, artifact gate,
  Gate C, review gate, and Gate D.
- Accepted Commands API documents `natural_command` and `needs_confirmation`.
- ADR-022 records HomeTusk-owned pending confirmation state.
- Sequence diagram records provider confirm to pending confirmation flow.
- Backend supports `NATURAL_COMMAND`, natural command schema validation, and
  deterministic non-mutating degraded behavior.
- AI Platform request capabilities include `confirm`.
- Supported provider `confirm` maps to `needs_confirmation`.
- `command_confirmations` persists pending confirmation state.
- DecisionLog preserves raw provider payload and mapped confirmation outcome.
- Provider `confirm` no-mutation behavior is covered by integration tests.

## Checks

Command run from `services/backend`:

```text
java -cp .\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain spotlessCheck test --tests "com.hometusk.commands.pipeline.decision.client.AiPlatformDecisionAdapterTest" --tests "com.hometusk.integration.CommandPipelineTest" --tests "com.hometusk.integration.aiplatform.AiPlatformIntegrationTest" --console=plain
```

Result: **BUILD SUCCESSFUL**.

Targeted test result summary:

- `AiPlatformDecisionAdapterTest`: 8 tests, 0 failures.
- `CommandPipelineTest`: 24 tests, 0 failures.
- `AiPlatformIntegrationTest`: 15 scenario tests, 0 failures.

## Residual Risks

- Approve/cancel endpoints are not implemented.
- Approval idempotency and stale/expired confirmation execution rules are not
  implemented.
- Expiry scheduler is not implemented.
- Mobile/web UI remains blocked.
- `answered` remains blocked.
- Full `./scripts/test.sh` was not run because the Windows environment does not
  provide a Unix shell for the script.
- Production rollout/config remains unapproved.

## Rationale

The delivered slice gives HomeTusk a real backend-owned contract state for
natural command confirmation without weakening execution authority. Provider
`confirm` now creates a pending proposal and audit trail; it still cannot mutate
household data before explicit approval.

Holding approve/cancel keeps lifecycle/idempotency risk out of this slice and
forces a focused follow-up gate before clients can depend on confirmation
execution.

## Next Recommended Action

Create the next workpack inside this initiative:

```text
INIT-2026Q3-natural-command-confirmation-approval-lifecycle-backend-contract
```

The workpack should cover approve/cancel contract deltas, household/requester
authorization, approval idempotency, stale/expired behavior, lifecycle audit
evidence, execution after approval, and integration tests.
