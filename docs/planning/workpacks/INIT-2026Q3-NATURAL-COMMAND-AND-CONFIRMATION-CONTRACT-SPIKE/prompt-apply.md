# Codex APPLY Prompt: Natural Command & Confirmation Contract Spike

You are Codex working in HomeTusk. Gate C is approved for docs-only APPLY.

## Objective

Implement the approved planning and draft contract artifacts for:

```text
INIT-2026Q3-natural-command-and-confirmation-contract-spike
```

## Allowed Files

- `docs/planning/strategy/roadmap.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.execution.md`
- `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/**`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/**`
- `docs/_indexes/contracts-index.md`

## Forbidden Files

- `docs/contracts/http/commands.openapi.yaml`
- `services/backend/src/main/java/**`
- `services/backend/src/main/resources/db/migration/**`
- `clients/**`
- `docs/integration/ai-platform/v1/upstream/**`
- `C:/Users/user/Documents/projects/VR_AI_Platform/**`

## Required Artifacts

- Planning execution notes with Gate A/B/artifact/C/review/D decisions.
- Workpack and checklist.
- Draft contract package:
  - `README.md`
  - `natural-command-request-contract-v0.md`
  - `command-response-outcomes-v0.md`
  - `needs-confirmation-contract-v0.md`
  - `confirmation-lifecycle-v0.md`
  - `provider-confirm-mapping-v0.md`
  - `guardrails-policy-v0.md`
  - `decisionlog-traceability-v0.md`
  - `mobile-state-contract-dependencies-v0.md`
  - `openapi-delta-draft.yaml`
  - `implementation-readiness-decision.md`

## Invariants

- HomeTusk remains source of truth and execution authority.
- AI output must be schema-validated before use.
- Domain invariants belong in code, not prompts.
- No mutation before explicit confirmation approval.
- `DecisionLog` is audit evidence, not pending confirmation state.
- Mobile/web call HomeTusk only.
- `answer` remains blocked.

## STOP THE LINE

Stop and report if APPLY requires:

- accepted OpenAPI changes;
- Java/backend/mobile/provider changes;
- direct mobile/web AI Platform calls;
- runtime confirmation execution;
- `answered` response;
- storing pending confirmation only in `DecisionLog`;
- changing upstream snapshots.

## Verification

- `git diff --check`
- Parse `docs/research/ai-command-capabilities/natural-command-contract-spike/openapi-delta-draft.yaml`
- Scan for forbidden file changes.
- Verify provider repo status remains clean.
