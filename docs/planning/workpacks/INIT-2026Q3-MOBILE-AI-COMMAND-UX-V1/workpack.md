# INIT-2026Q3 Mobile AI Command UX v1 - Workpack

## Sources of Truth

- Scope anchor: `docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.md`
- Execution notes: `docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.execution.md`
- Product goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- MVP release scope: `docs/planning/releases/MVP.md`
- DoR / DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Workflow: `AGENTS.md`, `docs/CODEX-WORKFLOW.md`
- Accepted Commands contract: `docs/contracts/http/commands.openapi.yaml`
- Backend dependency: `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.execution.md`
- Mobile app: `clients/mobile/**`

## Status

**DONE - GATE D GO / LIMITED-GO.** Delegated Gate A, Gate B, artifact gate,
read-only PLAN, Gate C, APPLY, verification, review gate, and Gate D are
complete as of 2026-06-16.

## Outcome

Implement the native mobile client UX for HomeTusk-owned natural commands and
confirmation approval/cancel without backend, OpenAPI, AI Platform, or production
rollout changes.

Target mobile behavior:

```text
typed command text
-> POST /api/v1/commands type=natural_command
-> render executed / degraded / clarify / rejected / scheduled / needs_confirmation
-> approve or cancel pending confirmation through HomeTusk backend
-> never call AI Platform and never execute proposed actions locally
```

## Acceptance Criteria

- [x] Roadmap marks Mobile AI Command UX v1 as the current NOW client initiative.
- [x] Main mobile Command composer sends `type=natural_command`, not the old plain-text `create_task` wrapper.
- [x] Typed requests include `payload.text`, `inputMode=text`, `locale`, `timezone`, `referenceInstant`, `source=mobile`, and `clientTimestamp`.
- [x] Request-builder support exists for `inputMode=voice_transcript` with optional `asrTraceId`, but no native voice capture UI is invented in this slice.
- [x] Mobile API types include `needs_confirmation`, confirmation shape, trace shape, approval response, cancel request, and cancel response.
- [x] Mobile API client calls:
  - `POST /api/v1/commands`
  - `POST /api/v1/commands/{commandId}/confirmations/{confirmationId}/approve`
  - `POST /api/v1/commands/{commandId}/confirmations/{confirmationId}/cancel`
- [x] Outcome UI renders `executed`, `executed_degraded`, `needs_input`, `needs_confirmation`, `rejected`, and `scheduled` as controlled outcomes.
- [x] Confirmation card displays user-safe summary, reasons, risk labels, proposed actions, expiry, approve, cancel, loading, terminal, and error states.
- [x] Confirmation card clearly says no action has happened yet.
- [x] Approve/cancel controls disable duplicate in-flight taps and show terminal replay/error safely.
- [x] Clarify continuation remains separate from confirmation.
- [x] Recent command history distinguishes controlled outcomes.
- [x] Mobile does not expose raw provider payload or provider prompts/credentials.
- [x] Mobile never calls AI Platform directly.
- [x] Mobile README and AI-command smoke checklist are updated.
- [x] `cd clients/mobile && npm run typecheck` passes or failure is documented.
- [x] Review gate records GO / NO-GO before Gate D.

## Non-goals

- Backend changes.
- Public OpenAPI changes.
- Database migrations.
- AI Platform changes or upstream snapshot edits.
- Direct mobile-to-AI-Platform calls.
- Native mobile ASR recording UI.
- `answered` / status-query card.
- Durable pending confirmation list/read model.
- Production rollout or feature-flag enablement.
- Natural completion/reschedule auto-execute outside backend-controlled contract.

## Impact Flags

| Flag | Value | Notes |
| --- | --- | --- |
| `contract_impact` | no | Consumes accepted Commands API only |
| `backend_impact` | no | Backend source of truth remains unchanged |
| `mobile_impact` | yes | Main scope |
| `ai_platform_impact` | no | No direct provider calls |
| `security_sensitive` | yes | Approval/cancel UI can trigger backend command execution |
| `traceability_critical` | yes | Command/correlation/confirmation ids and safe audit hints |
| `adr_needed` | no | No new architecture boundary |
| `diagrams_needed` | no | No new diagram required for this mobile-only consumption slice |
| `cross_repo` | no | HomeTusk repo only |

## Files to Change

- `docs/planning/strategy/roadmap.md` - current initiative and next action.
- `docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.md` - initiative anchor imported from user-provided draft.
- `docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.execution.md` - gates, evidence, risks.
- `docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/**` - workpack, PLAN, Gate C, APPLY, review, Gate D evidence.
- `clients/mobile/src/api/types.ts` - natural command, confirmation, and lifecycle types.
- `clients/mobile/src/api/client.ts` - approve/cancel client methods.
- `clients/mobile/src/features/command/commandRequestBuilder.ts` - natural command request builder.
- `clients/mobile/src/features/command/commandOutcomeFormatting.ts` - controlled outcome copy.
- `clients/mobile/src/features/command/CommandOutcomeCard.tsx` - safe non-confirmation outcome rendering.
- `clients/mobile/src/features/command/CommandConfirmationCard.tsx` - confirmation UX.
- `clients/mobile/src/features/command/CommandSurface.tsx` - compose outcome/confirmation/history views.
- `clients/mobile/src/features/command/CommandComposer.tsx` - natural command composer copy.
- `clients/mobile/src/features/command/commandTypes.ts` - component prop shape if needed.
- `clients/mobile/src/features/command/commandHistoryStore.ts` - recent history labeling.
- `clients/mobile/src/app/types.ts` - command controls for confirmation actions.
- `clients/mobile/src/app/AppShell.tsx` - submit/approve/cancel handlers and state.
- `clients/mobile/src/shared/ui/styles.ts` - small confirmation layout styles.
- `clients/mobile/src/shared/errors/apiErrorFormatting.ts` - user-safe confirmation errors.
- `clients/mobile/src/storage/localAppMemory.ts` - safe recent command status metadata if needed.
- `clients/mobile/README.md` - command UX docs and boundary.
- `clients/mobile/docs/release-smoke-ai-command.md` - manual smoke checklist.

Forbidden:

- `services/backend/**`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/integration/ai-platform/v1/upstream/**`
- AI Platform or other external repositories

## Implementation Plan

### Commit 1 - Planning and roadmap

Steps:

1. Import initiative into `docs/planning/initiatives`.
2. Create execution notes and workpack packet.
3. Update roadmap NOW/NEXT entries.

Verification:

- `rg -n "mobile-ai-command-ux-v1|Mobile AI Command UX v1|Gate C" docs/planning`

### Commit 2 - Mobile contract consumption

Steps:

1. Add natural command and confirmation lifecycle types.
2. Add approve/cancel API client methods.
3. Replace command composer request building with `natural_command`.
4. Preserve structured task quick actions outside the Command tab.

Verification:

- `cd clients/mobile && npm run typecheck`

### Commit 3 - Mobile UX, docs, and review evidence

Steps:

1. Add confirmation card and controlled outcome formatting.
2. Wire approve/cancel handlers, loading, terminal, and error states.
3. Update recent command history labels.
4. Update README and manual smoke checklist.
5. Record review gate and Gate D.

Verification:

- `cd clients/mobile && npm run typecheck`
- `git diff --check`

## Contract Impact

No OpenAPI or backend contract edits are approved. The mobile client consumes
the accepted additive Commands API contract:

- existing structured `create_task` and `complete_task` calls remain available
  for explicit task quick actions;
- Command tab typed text sends `natural_command`;
- approve/cancel use documented confirmation lifecycle endpoints.

## Docs Updates

- [x] Roadmap updated.
- [x] Initiative execution notes updated.
- [x] Mobile README updated.
- [x] Mobile smoke checklist added.
- [x] Workpack evidence and Gate D recorded.

## Tests

- [x] TypeScript typecheck.
- [ ] Manual smoke: submit natural command.
- [ ] Manual smoke: receive executed.
- [ ] Manual smoke: receive needs_input and continue.
- [ ] Manual smoke: receive rejected.
- [ ] Manual smoke: receive executed_degraded.
- [ ] Manual smoke: receive needs_confirmation.
- [ ] Manual smoke: approve confirmation.
- [ ] Manual smoke: cancel confirmation.
- [x] Manual smoke: app refresh/retry limitation documented.

Unit tests are not committed in this slice because `clients/mobile/package.json`
does not currently define a test runner.

## Verification Commands

- `git status --short` - inspect diff and unrelated dirty files.
- `rg -n "natural_command|needs_confirmation|approveCommandConfirmation|cancelCommandConfirmation" clients/mobile docs/planning` - verify scope.
- `cd clients/mobile && npm run typecheck` - mobile TypeScript verification.
- `git diff --check` - whitespace check.

## DoD Checklist

- [ ] Tests/checks pass or failures are documented.
- [ ] No backend/OpenAPI/AI Platform files changed.
- [ ] No direct mobile-to-AI-Platform call path added.
- [ ] Confirmation card does not imply execution before approval.
- [ ] Approve/cancel go through backend API client only.
- [ ] User-safe error handling exists for forbidden/not-found/conflict/expired cases.
- [ ] Docs and smoke checklist updated.
- [ ] Review gate recorded before Gate D.

## Risks

| Risk | Mitigation |
| --- | --- |
| Mobile treats `needs_confirmation` as executed | Dedicated confirmation card with explicit no-action-yet copy |
| Main composer keeps old `create_task` wrapper | Replace request builder default with `natural_command` |
| Duplicate approval taps | Disable actions while in flight and after terminal result |
| Pending confirmation lost on app restart | Document v1 submit-result-driven limitation and backend read endpoint follow-up |
| Voice transcript auto-executes | No native ASR UI in this slice; builder support remains explicit-submit only |
| Missing unit test runner | Run typecheck and document manual smoke checklist |

## Rollback

- Revert mobile and planning commits on this branch.
- No database or external-provider rollback is required.
- No backend feature flag rollback is required because backend behavior is not changed.

## Prompt Pack

- PLAN: `docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/prompt-plan.md`
- PLAN findings: `docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/plan-findings.md`
- Gate C: `docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/gate-c.md`
- APPLY: `docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/prompt-apply.md`
- Review gate: `docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/review-gate.md`
- Gate D: `docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/gate-d.md`
