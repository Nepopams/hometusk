# Codex APPLY Prompt - INIT-2026Q3 Mobile AI Command UX v1

Implement only the Gate C approved scope for Mobile AI Command UX v1.

## Sources

- `AGENTS.md`
- `docs/CODEX-WORKFLOW.md`
- `docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.md`
- `docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.execution.md`
- `docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/workpack.md`
- `docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/plan-findings.md`
- `docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/gate-c.md`
- `docs/contracts/http/commands.openapi.yaml`
- `clients/mobile/**`

## Allowed Files

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
- AI Platform or external repository files

## Implementation Requirements

- Main mobile Command composer sends `natural_command`.
- Typed commands use `inputMode=text`, `source=mobile`, locale, timezone,
  reference instant, and client timestamp.
- Future voice transcript handoff support may exist in the request builder, but
  no native ASR recording UI may be invented.
- `needs_confirmation` is rendered as pending, not executed.
- Approve/cancel call HomeTusk backend confirmation endpoints only.
- Duplicate approve/cancel taps are disabled while in flight and after terminal
  result.
- `needs_input` continuation remains separate from confirmation.
- `rejected` and `executed_degraded` remain controlled outcomes, not crashes.
- Recent history labels distinguish outcomes.
- No raw provider payload, prompts, credentials, or stack traces are displayed.

## STOP-THE-LINE

Stop and record HOLD if implementation requires:

- backend code changes;
- OpenAPI edits;
- AI Platform edits;
- direct mobile-to-provider calls;
- `answered` support;
- durable pending confirmation backend read models;
- new native voice/ASR behavior outside the existing command surface.

## Verification

- `cd clients/mobile && npm run typecheck`
- `git diff --check`
- Update review gate and Gate D artifacts after verification.
