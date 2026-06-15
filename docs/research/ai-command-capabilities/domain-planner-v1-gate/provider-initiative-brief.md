# Provider Initiative Brief

Status: Handoff brief for future provider work

Date: 2026-06-15

## Recommended Initiative Name

`AI Platform Domain Planner v1 - Narrow Household Command Corridor`

## Repository Owner

`vr_ai_platform`

Cross-repo policy:

- HomeTusk commits stay in the HomeTusk repository.
- AI Platform changes must happen in the provider repository through a separate
  initiative, branch, and PR.
- HomeTusk artifacts are consumer acceptance inputs, not provider code changes.

## HomeTusk Artifacts To Consume

- `docs/research/ai-command-capabilities/domain-planner-v1-gate/README.md`
- `docs/research/ai-command-capabilities/domain-planner-v1-gate/decision-action-taxonomy-accepted-v0.md`
- `docs/research/ai-command-capabilities/domain-planner-v1-gate/natural-command-contract-v0-draft.md`
- `docs/research/ai-command-capabilities/domain-planner-v1-gate/golden-scenarios-fixtures-v0/`
- `docs/research/ai-command-capabilities/domain-planner-v1-gate/eval-rubric-v0.md`
- `docs/research/ai-command-capabilities/domain-planner-v1-gate/privacy-and-retention-questions.md`
- `docs/research/ai-command-capabilities/domain-planner-v1-gate/provider-planner-readiness-checklist.md`
- `docs/research/ai-command-capabilities/domain-planner-v1-gate/hometusk-ai-platform-integration-doc-drift.md`

## Provider Scope

- Implement or adapt a single Domain Planner v1 for the narrow household
  command corridor.
- Produce schema-valid provider decisions.
- Support `create_task`.
- Support multi-item `add_shopping_items`.
- Support `clarify`.
- Support safe `reject` mapping.
- Optionally support non-executing `confirm`.
- Preserve date/time timezone handling or clarify.
- Run HomeTusk golden fixtures and emit deterministic eval output.
- Provide planner version, decision version, trace id, and schema version.
- Document privacy/retention answers.

## Provider Non-Goals

- No direct mutation of HomeTusk state.
- No direct mobile/web integration.
- No broad multi-agent production planner.
- No full household workload optimizer.
- No natural reschedule or completion auto-execute.
- No answer/status implementation until HomeTusk answer contract exists.
- No prompt-only rollout without schema and eval evidence.

## Expected Contract Outputs

The provider initiative should either:

1. keep current schema and document accepted mapping into HomeTusk taxonomy, or
2. propose contract changes through provider contract governance.

Expected output fields:

- decision id;
- command id;
- status/outcome;
- action/proposed actions;
- confidence;
- explanation;
- trace id;
- schema version;
- decision version;
- planner version;
- created timestamp.

## Expected Eval Outputs

- per-scenario results;
- aggregate metrics;
- failure bucket counts;
- fixture version/source;
- run command;
- environment feature flags;
- blocker failure count;
- skipped scenario list with reason.

## HomeTusk Acceptance Gates

HomeTusk acceptance remains separate from provider implementation:

- Provider passing its tests is necessary but not sufficient.
- HomeTusk must review fixture results against accepted taxonomy.
- HomeTusk must verify no unsupported actions auto-execute.
- HomeTusk must verify privacy/retention answers.
- HomeTusk must keep final runtime execution authority.

## Rationale

The current HomeTusk posture is LIMITED-GO: move forward with a narrow provider
planner only after artifact, eval, privacy, and drift gates are explicit. This
brief gives the provider enough bounded input to start planning without
accidentally approving HomeTusk runtime or mobile work.
