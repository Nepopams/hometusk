# Codex PLAN Prompt - INIT-2026Q3 Mobile AI Command UX v1

Read-only only. Do not edit, create, delete, move, format, or generate tracked
files while executing this PLAN.

## Objective

Produce a decision-complete implementation plan for Mobile AI Command UX v1
against the accepted HomeTusk Commands API contract.

## Sources

- `AGENTS.md`
- `docs/CODEX-WORKFLOW.md`
- `docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.md`
- `docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.execution.md`
- `docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/workpack.md`
- `docs/contracts/http/commands.openapi.yaml`
- `clients/mobile/README.md`
- `clients/mobile/src/api/types.ts`
- `clients/mobile/src/api/client.ts`
- `clients/mobile/src/features/command/**`
- `clients/mobile/src/app/**`

## Required Findings

- Exact mobile files to change.
- Request builder strategy for typed natural commands and future voice transcript handoff.
- Response type strategy for `needs_confirmation`, trace, approval, and cancel responses.
- Confirmation card rendering and terminal/error state strategy.
- Approve/cancel API handling strategy.
- Existing continuation behavior and separation from confirmation.
- Tests and verification commands available in `clients/mobile`.
- Manual smoke checklist and limitations.
- Stop conditions that should move the initiative to HOLD.

## Invariants

- Mobile must not call AI Platform.
- Mobile must not execute or simulate proposed actions locally.
- Backend and OpenAPI files are read-only for this initiative.
- `answered`, production rollout, and durable confirmation history remain out of scope.
- Invalid or missing contract support results in HOLD, not backend patching.

## Output

Record findings in
`docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/plan-findings.md`
after the read-only PLAN is complete and Gate C is ready for GO / NO-GO / HOLD.
