# Codex APPLY Prompt - INIT-2026Q3 Natural Command + Needs Confirmation Backend Contract

Implement only the Gate C-approved limited APPLY from:

```text
docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-NEEDS-CONFIRMATION-BACKEND-CONTRACT/gate-c.md
docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-NEEDS-CONFIRMATION-BACKEND-CONTRACT/plan-findings.md
```

## Scope

Implement:

- accepted Commands API contract docs for `natural_command` and
  `needs_confirmation`;
- ADR and sequence diagram for pending confirmation state;
- backend `NATURAL_COMMAND` type and natural command payload schema;
- safe non-mutating degraded fallback for natural commands;
- persistent pending confirmation state;
- provider `confirm -> needs_confirmation` mapping for supported payloads;
- guardrail pre-check as proposal;
- DecisionLog traceability with raw provider payload and HomeTusk confirmation id;
- tests for validation, no mutation, traceability, and compatibility.

Do not implement:

- approve/cancel endpoints;
- approval execution or idempotency;
- expiry scheduler;
- mobile/web UI;
- `answered`;
- direct mobile/web to AI Platform;
- AI Platform repo changes;
- production rollout/config changes.

## Invariants

- AI output must be schema-validated before use.
- Business/domain invariants are enforced in code.
- Invalid or unsupported AI output is rejected or clarified safely.
- Provider `confirm` must not execute actions before explicit approval.
- `DecisionLog` is audit evidence, not the only pending confirmation state.
- Existing structured `create_task` and `complete_task` clients must remain compatible.
- Public response must not expose raw provider payload.

## Required Tests

- `cd services/backend && ./gradlew test --tests "com.hometusk.commands.pipeline.decision.client.AiPlatformDecisionAdapterTest"`
- `cd services/backend && ./gradlew test --tests "com.hometusk.integration.CommandPipelineTest"`
- `cd services/backend && ./gradlew test --tests "com.hometusk.integration.aiplatform.AiPlatformIntegrationTest"`

Run `./scripts/test.sh` if targeted tests pass and time/resources allow.

## STOP-THE-LINE

Stop and record HOLD instead of improvising if:

- the first slice cannot create pending confirmation state without approval execution;
- natural command degraded fallback would need to guess a domain action;
- existing clients would require breaking changes;
- mobile/web files need edits;
- AI Platform repo files need edits.
