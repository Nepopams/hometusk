# Codex PLAN Prompt - INIT-2026Q3 Natural Command + Needs Confirmation Backend Contract

You are planning the HomeTusk current roadmap initiative:

```text
docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md
```

## Mode

Read-only PLAN. Do not edit, create, delete, move, format, or generate tracked
files.

## Must Read

- `AGENTS.md`
- `docs/CODEX-WORKFLOW.md`
- `docs/planning/strategy/product-goal.md`
- `docs/planning/strategy/roadmap.md`
- `docs/planning/releases/MVP.md`
- `docs/_governance/dor.md`
- `docs/_governance/dod.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.execution.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.execution.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/**`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/_indexes/contracts-index.md`
- `docs/architecture/service-catalog.md`
- relevant `services/backend/src/main/java/com/hometusk/commands/**`
- relevant `services/backend/src/test/**`

## Questions to Answer

1. What exact backend, contract, docs, migration, ADR, and diagram files should APPLY touch?
2. Is Gate C GO, NO-GO, or HOLD?
3. Should first APPLY include approve/cancel execution, or only pending
   confirmation creation and `needs_confirmation` response?
4. What is the smallest safe pending confirmation source of truth?
5. How will `natural_command` degrade safely when AI Platform is unavailable?
6. How will provider `confirm` be schema-validated, guardrail-checked, and
   prevented from mutating before approval?
7. What tests prove compatibility and no-mutation behavior?
8. What rollback path is safe if migration/API changes fail review?

## Invariants

- HomeTusk remains execution authority.
- AI output must be schema-validated before use.
- Domain invariants are enforced in code, not prompts.
- Invalid AI output is rejected; do not silently repair it.
- `DecisionLog` is audit evidence, not the only pending confirmation state.
- Provider `confirm` must never execute before explicit approval.
- Existing structured command clients must remain compatible.
- Mobile/web UI, `answered`, direct client-to-AI Platform calls, AI Platform repo writes, and production rollout are out of scope.

## Expected Output Artifact

Record findings in:

```text
docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-NEEDS-CONFIRMATION-BACKEND-CONTRACT/plan-findings.md
```

The PLAN findings must include:

- files read;
- current-state findings;
- recommended APPLY slice;
- exact allowed and forbidden files;
- gate decisions needed;
- tests/checks;
- risks and STOP conditions.
