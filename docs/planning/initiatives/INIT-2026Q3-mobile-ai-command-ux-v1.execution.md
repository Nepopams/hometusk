# INIT-2026Q3 Mobile AI Command UX v1 Execution Notes

**Status:** Gate A GO; Gate B GO; artifact gate GO; Gate C GO; review gate GO; Gate D GO / LIMITED-GO.
**Date:** 2026-06-16
**Initiative:** `docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.md`
**Roadmap:** `docs/planning/strategy/roadmap.md`
**Delegation:** Human gates for this initiative are delegated to Codex. Every
GO / NO-GO / HOLD decision must be recorded here with evidence, risks, and
rationale.

---

## Intake Summary

| Field | Decision |
| --- | --- |
| Request type | Activate new roadmap initiative and proceed through HomeTusk planning pipeline |
| Scope anchor | `INIT-2026Q3-mobile-ai-command-ux-v1` |
| Workflow path | `intake -> planning -> artifact gate -> workpack -> Codex PLAN -> Gate C -> APPLY -> review gate -> Gate D` |
| Change type | native mobile feature, AI command UX, contract integration, docs/process |
| Work level | initiative-level mobile client implementation |
| Primary boundary | Mobile consumes HomeTusk Commands API only; HomeTusk backend remains source of truth |
| Runtime posture | Mobile runtime changes are expected after Gate C GO |
| Public API posture | No public contract changes are approved; OpenAPI is read-only source of truth |

## Sources of Truth Read

| Artifact | Path |
| --- | --- |
| Active repo rules | `AGENTS.md` |
| Planning scoped rules | `docs/planning/AGENTS.md` |
| Workflow | `docs/CODEX-WORKFLOW.md` |
| Product goal | `docs/planning/strategy/product-goal.md` |
| Roadmap | `docs/planning/strategy/roadmap.md` |
| MVP release scope | `docs/planning/releases/MVP.md` |
| DoR / DoD | `docs/_governance/dor.md`, `docs/_governance/dod.md` |
| Initiative | `docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.md` |
| Backend contract initiative | `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md` |
| Backend contract execution | `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.execution.md` |
| Commands OpenAPI | `docs/contracts/http/commands.openapi.yaml` |
| Mobile README | `clients/mobile/README.md` |
| Mobile API client/types | `clients/mobile/src/api/client.ts`, `clients/mobile/src/api/types.ts` |
| Mobile command feature | `clients/mobile/src/features/command/**` |
| Mobile app shell | `clients/mobile/src/app/**` |

## Triage Classification

| Flag | Value | Evidence |
| --- | --- | --- |
| `contract_impact` | no | Initiative consumes accepted Commands API; `commands.openapi.yaml` already includes `natural_command`, `needs_confirmation`, and approve/cancel endpoints |
| `backend_impact` | no | Backend approval/cancel lifecycle already reached Gate D GO; backend files are forbidden unless HOLD records a follow-up |
| `data_impact` | no | Mobile persists only safe in-session/recent command UX state |
| `mobile_impact` | yes | Current mobile composer still sends `create_task` / `complete_task`; mobile lacks `needs_confirmation` and approve/cancel support |
| `ai_platform_impact` | no | Mobile must never call AI Platform directly |
| `security_sensitive` | yes | Confirmation approval/cancel controls can trigger household command execution through backend |
| `traceability_critical` | yes | UI must preserve command, correlation, and confirmation ids without exposing raw provider payload |
| `adr_needed` | no | No architecture boundary or persistence model change is introduced by mobile client consumption |
| `diagrams_needed` | no | Existing backend sequence/ADR covers confirmation lifecycle; mobile state is documented in workpack and smoke checklist |
| `cross_repo` | no | HomeTusk repo only |

## Scope Boundary Preserved

In scope:

- Add this initiative to `docs/planning/initiatives/**`.
- Promote Mobile AI Command UX v1 into the roadmap as the current NOW client initiative.
- Create initiative execution notes and a mobile implementation workpack.
- Update mobile command request building to send `natural_command`.
- Update mobile API types/client for `needs_confirmation` and approve/cancel.
- Render safe outcome cards, confirmation card, clarify/reject/degraded/scheduled states, and recent command labels.
- Update mobile README and manual smoke docs.

Out of scope:

- Backend runtime changes.
- Public OpenAPI changes.
- Database migrations.
- AI Platform changes or direct mobile-to-provider calls.
- `answered` / status-query UX.
- Production rollout/config enablement.
- Durable pending confirmation list/read model unless a backend read endpoint is separately gated.

## Gate A Decision - GO

**Decision:** GO for making `INIT-2026Q3-mobile-ai-command-ux-v1` the current
roadmap initiative.

**Evidence:**

- Backend `natural_command + needs_confirmation` and approve/cancel lifecycle
  reached Gate D GO on 2026-06-16.
- Roadmap next action explicitly recommended planning Mobile AI Command UX v1
  using the accepted backend contract.
- Current mobile code still builds old structured command requests from plain
  text and has no `needs_confirmation` status or approve/cancel client methods.
- Initiative scope is bounded to mobile client consumption and preserves the
  no-direct-AI-platform invariant.

**Rationale:**

This is the correct next roadmap step because the accepted backend contract now
has the client-facing lifecycle mobile needs, and delaying mobile UX would leave
the first user-facing AI command surface unable to use the safety contract.

**Risks accepted:**

- Mobile has no dedicated test runner beyond TypeScript typecheck.
- Pending confirmations are submit-result-driven in v1 because no dedicated
  mobile confirmation-history/read endpoint is in scope.
- Voice/ASR exists for web, but native mobile currently has only command
  deep-link handoff; mobile voice submit must stay a future extension unless
  a local transcript flow is found during APPLY.

## Gate B Decision - GO

**Decision:** GO for one initiative-level workpack and implementation slice.

**Workpack path:**

```text
docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/
```

**Committed planning scope:**

- Exact mobile files for request builder, API types/client, command cards,
  app shell handlers, README, and smoke docs.
- Typecheck and manual smoke verification.
- No backend/OpenAPI/AI Platform edits.
- Review gate and Gate D decision after verification evidence.

**DoR evidence:**

- Initiative includes title, owner, target milestone, sources, outcome,
  in-scope/out-of-scope, flags, risks, success metrics, expected files, and exit
  criteria.
- Accepted backend contract is available in `docs/contracts/http/commands.openapi.yaml`.
- Current mobile gaps were verified in source before APPLY.

**Rationale:**

The initiative is Ready because the backend dependency is closed, the mobile
blast radius is narrow, and the work can be verified with TypeScript plus a
manual smoke checklist.

## Artifact Gate Decision - GO

**Contract gate:** GO without contract edits.

**Evidence:**

- `CommandRequest` already includes `natural_command`.
- `CommandResponse` already includes `needs_confirmation`.
- Approve/cancel endpoints are already documented under `/commands/{commandId}/confirmations/{confirmationId}`.
- This initiative consumes those contracts from mobile and does not change
  public schemas.

**ADR / diagram gate:** GO without new ADR or diagram.

**Evidence:**

- Mobile code does not introduce a new source of truth, persistence model,
  service boundary, or provider integration.
- Backend confirmation lifecycle decisions remain anchored to the prior backend
  contract initiative and ADR/diagram artifacts.

**Security / traceability gate:** GO with mandatory review.

**Future implementation must prove:**

- no mobile-to-AI-Platform path exists;
- mobile does not execute or simulate proposed actions locally;
- confirmation approval and cancellation go only through HomeTusk backend
  endpoints;
- UI does not expose raw provider payload;
- command/confirmation ids remain visible enough for support traceability.

## Codex PLAN Findings

Detailed findings are recorded in:

```text
docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/plan-findings.md
```

Key findings:

- `clients/mobile/src/features/command/commandRequestBuilder.ts` is the main
  request-builder switch point and currently converts typed text into
  `create_task` / `complete_task`.
- `clients/mobile/src/api/types.ts` lacks `natural_command`,
  `needs_confirmation`, confirmation shapes, and approve/cancel response types.
- `clients/mobile/src/api/client.ts` lacks approve/cancel methods.
- `CommandOutcomeCard` is generic and cannot render confirmation detail,
  proposed actions, risk labels, expiry, or terminal approval/cancel states.
- `CommandContinuationCard` is already scoped to `needs_input`, which preserves
  clarify vs confirmation separation.
- Native mobile has no current ASR transcript flow; v1 should support typed
  natural commands now and leave voice-transcript builder support available for
  a later native voice flow.
- `clients/mobile/package.json` has no unit-test script; verification should use
  `npm run typecheck` and a manual release smoke document.

## Gate C Decision - GO

**Decision:** GO for APPLY.

**Approved files:**

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

**Forbidden files:**

- `services/backend/**`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/integration/ai-platform/v1/upstream/**`
- AI Platform or other external repositories

**Approved scope:**

- Main mobile command composer sends `natural_command`.
- Mobile request builder includes `text`, `inputMode`, `locale`, `timezone`,
  `referenceInstant`, optional `asrTraceId`, `source`, and `clientTimestamp`.
- Mobile API types/client understand `needs_confirmation` and approve/cancel.
- Mobile renders safe outcome and confirmation cards, including terminal
  approval/cancel result/error handling.
- Recent command history distinguishes controlled outcomes.
- README and smoke checklist document behavior and limitations.

**Held scope:**

- Native mobile ASR capture UI.
- Durable pending confirmation restore/list view.
- `answered` cards and status-query UX.
- Production rollout/config.

**Rationale:**

The approved slice is the smallest mobile implementation that consumes the
accepted backend safety contract without changing backend behavior or expanding
AI semantics.

## APPLY Evidence

**Decision:** APPLY completed for the mobile implementation slice.

**Delivered:**

- initiative imported into `docs/planning/initiatives`;
- roadmap updated to make Mobile AI Command UX v1 the NOW initiative;
- workpack, PLAN findings, Gate C, APPLY prompt, review gate, and Gate D
  artifacts created;
- mobile API types updated for `natural_command`, `needs_confirmation`,
  confirmation details, trace, approval response, cancel request, and cancel
  response;
- mobile API client now supports approve/cancel confirmation endpoints;
- main Command composer now sends typed text as `type=natural_command`;
- request builder supplies text, input mode, locale, timezone, reference
  instant, optional ASR trace id, source, and client timestamp;
- future explicit-submit voice transcript request builder support exists without
  adding native ASR UI;
- dedicated confirmation card renders no-action-yet copy, summary, reasons,
  risk labels, proposed actions, expiry, ids, approve/cancel buttons, loading,
  terminal, and error states;
- `needs_input` continuation remains separate from `needs_confirmation`;
- rejected, degraded, scheduled, executed, clarify, and confirmation outcomes
  are represented as controlled outcomes;
- recent command history uses readable outcome labels;
- mobile README and smoke checklist document behavior and limitations.

**Held by design:**

- backend runtime changes;
- public OpenAPI changes;
- AI Platform changes or direct mobile-to-provider calls;
- native mobile ASR recording UI;
- durable pending confirmation restore/list view;
- `answered` / status-query UX;
- production rollout/config.

**Scope control:**

- No files under `services/backend/**` changed.
- No `docs/contracts/http/commands.openapi.yaml` changes.
- No `docs/integration/ai-platform/v1/upstream/**` changes.
- Search of `clients/mobile/src` found no direct AI Platform/provider request
  path; networking remains through the existing HomeTusk API client.

## Verification Evidence

Command run from `clients/mobile`:

```text
npm run typecheck
```

Result: **PASS** on 2026-06-16.

Command run from repository root:

```text
git diff --check
```

Result: **PASS** with Windows line-ending warnings only.

Command run from repository root:

```text
git diff --name-only -- services\backend docs\contracts\http\commands.openapi.yaml docs\integration\ai-platform\v1\upstream
```

Result: no output.

Not run:

- Manual mobile emulator/device smoke was documented in
  `clients/mobile/docs/release-smoke-ai-command.md` but was not executed in this
  turn.
- Mobile unit tests were not run because `clients/mobile/package.json` has no
  test script.

## Review Gate Decision - GO

**Review artifact:** `docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/review-gate.md`

**Decision:** GO for the mobile implementation slice.

**Must-fix findings:** none.

**Rationale:**

- The main Command tab now consumes the accepted `natural_command` contract.
- Confirmation approval/cancel goes through HomeTusk backend endpoints only.
- The UI does not mark confirmation as executed before explicit approval.
- `needs_input` remains separate from confirmation.
- No backend/OpenAPI/AI Platform files changed.
- TypeScript verification passed.

## Gate D Decision - GO / LIMITED-GO

**Gate D artifact:** `docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/gate-d.md`

**Decision:** GO for the completed mobile implementation slice. LIMITED-GO for
broader product readiness until manual smoke, production rollout/config, durable
pending-confirmation restore, native mobile ASR capture, and `answered` are
handled through separate gates.

**Evidence:**

- Roadmap, initiative, workpack, execution notes, review gate, and Gate D were
  updated.
- Mobile sends typed commands as `natural_command`.
- Mobile can render `needs_confirmation` and invoke approve/cancel backend
  endpoints.
- Confirmation UI states that no action has happened yet.
- Typecheck passed.
- Forbidden backend/OpenAPI/AI Platform files were not changed.

**Residual risks:**

- Manual smoke has not yet been executed on emulator/device.
- No mobile unit-test runner exists today.
- Pending confirmations are submit-result-driven and not restored after app
  refresh without a future backend read model.
- Native mobile ASR capture remains out of scope.
- `answered` and production rollout/config remain unapproved.

**Next recommended action:**

Run `clients/mobile/docs/release-smoke-ai-command.md` against a backend build
with representative command outcomes. After smoke evidence, plan production
rollout/config as a separate initiative or keep it held until durable pending
confirmation restore is needed.
