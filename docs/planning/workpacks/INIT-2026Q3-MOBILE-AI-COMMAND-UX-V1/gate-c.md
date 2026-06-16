# Gate C - INIT-2026Q3 Mobile AI Command UX v1

**Date:** 2026-06-16
**Decision:** GO
**Decider:** Codex, under delegated human-gate authority from the user goal.

## Sources of Truth

- `AGENTS.md`
- `docs/CODEX-WORKFLOW.md`
- `docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.md`
- `docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.execution.md`
- `docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/workpack.md`
- `docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/plan-findings.md`
- `docs/contracts/http/commands.openapi.yaml`
- `clients/mobile/**`

## Approved APPLY Scope

- Mobile Command composer sends `natural_command` for typed text.
- Mobile request payload includes required natural command fields.
- API types/client support `needs_confirmation` and approve/cancel lifecycle.
- UI renders safe controlled outcome cards and a dedicated confirmation card.
- Approve/cancel handlers use HomeTusk backend only.
- Recent command history distinguishes outcomes.
- Mobile README and smoke checklist are updated.
- Planning evidence is updated through review gate and Gate D.

## Approved Files

- `clients/mobile/src/api/types.ts`
- `clients/mobile/src/api/client.ts`
- `clients/mobile/src/app/AppShell.tsx`
- `clients/mobile/src/app/types.ts`
- `clients/mobile/src/features/command/**`
- `clients/mobile/src/shared/ui/styles.ts`
- `clients/mobile/src/shared/errors/apiErrorFormatting.ts`
- `clients/mobile/src/storage/localAppMemory.ts`
- `clients/mobile/README.md`
- `clients/mobile/docs/release-smoke-ai-command.md`
- `docs/planning/strategy/roadmap.md`
- `docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.md`
- `docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.execution.md`
- `docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/**`

## Forbidden Files

- `services/backend/**`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/integration/ai-platform/v1/upstream/**`
- External AI Platform repositories

## Held Scope

- Native ASR capture UI.
- `answered` / status-query UX.
- Durable pending confirmation restore/list endpoint.
- Backend or OpenAPI changes.
- Production rollout/config enablement.

## Rationale

The read-only PLAN found that the backend contract dependency is already
accepted and current mobile gaps are contained to the command feature, API
client/types, app shell wiring, and docs. The APPLY can deliver the first mobile
AI-command UX without expanding HomeTusk architecture or contract semantics.

## Required Verification

- `cd clients/mobile && npm run typecheck`
- `git diff --check`
- Review gate with GO / NO-GO before Gate D.
