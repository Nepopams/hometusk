# PLAN Findings - INIT-2026Q3 Mobile AI Command UX v1

**Date:** 2026-06-16
**Mode:** Read-only exploration before Gate C.
**Result:** GO for mobile APPLY.

## Sources Read

- `AGENTS.md`
- `docs/CODEX-WORKFLOW.md`
- `docs/planning/strategy/product-goal.md`
- `docs/planning/strategy/roadmap.md`
- `docs/planning/releases/MVP.md`
- `docs/_governance/dor.md`
- `docs/_governance/dod.md`
- `docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.execution.md`
- `docs/contracts/http/commands.openapi.yaml`
- `clients/mobile/README.md`
- `clients/mobile/package.json`
- `clients/mobile/src/api/types.ts`
- `clients/mobile/src/api/client.ts`
- `clients/mobile/src/app/AppShell.tsx`
- `clients/mobile/src/app/types.ts`
- `clients/mobile/src/features/command/**`
- `clients/mobile/src/storage/localAppMemory.ts`

## Current State

- The mobile Command tab is a deterministic text shell:
  - plain text becomes `type=create_task`;
  - `done` / `complete` text becomes `type=complete_task`;
  - `needs_input` continuation uses `/commands/{commandId}/continue`.
- `CommandStatus` does not include `needs_confirmation`.
- `CommandSource` is mobile-only and does not represent `voice`.
- `CommandResponse` lacks `confirmation` and `trace`.
- API client lacks approve/cancel lifecycle methods.
- Outcome copy handles `executed`, `executed_degraded`, `scheduled`,
  `needs_input`, and `rejected`, but not confirmation detail.
- `CommandContinuationCard` already returns `null` unless status is
  `needs_input`, so clarify and confirmation can stay separate.
- Recent command history stores id, text, status, and createdAt in AsyncStorage,
  which is safe UX state.
- Native mobile currently has command deep-link handoff but no ASR recording or
  transcript draft flow.
- `clients/mobile/package.json` exposes `typecheck` but no unit-test script.

## Accepted Contract Findings

- `docs/contracts/http/commands.openapi.yaml` already documents:
  - `CommandRequest.type=natural_command`;
  - `NaturalCommandPayload` with `text`, `inputMode`, `locale`, `timezone`,
    `referenceInstant`, optional `asrTraceId`;
  - `CommandResponse.status=needs_confirmation`;
  - `CommandConfirmation`, proposed actions, and trace shape;
  - approve endpoint;
  - cancel endpoint.
- No OpenAPI change is required or approved.

## Request Builder Strategy

- Replace the main Command composer default with `natural_command`.
- Preserve structured `create_task` / `complete_task` calls only for explicit
  task quick actions outside the main Command tab.
- Generate one timestamp per request and use it for both `referenceInstant` and
  `clientTimestamp`.
- Resolve locale/timezone from `Intl.DateTimeFormat().resolvedOptions()` with
  safe fallbacks.
- Default typed commands to `inputMode=text` and `source=mobile`.
- Provide a builder option for future `inputMode=voice_transcript`,
  `source=voice`, and optional `asrTraceId`; do not invent native ASR UI in this
  APPLY.

## Response Type Strategy

- Add `needs_confirmation` to `CommandStatus`.
- Add `CommandConfirmation`, `CommandConfirmationTrace`,
  `CommandConfirmationApprovalResponse`, `CommandConfirmationCancelRequest`, and
  `CommandConfirmationCancelResponse`.
- Keep `CommandResponse` as a pragmatic mobile type with optional status-specific
  fields because the existing mobile code already uses that shape.

## Confirmation UX Strategy

- Add a dedicated `CommandConfirmationCard` for `needs_confirmation`.
- Do not show continuation form for confirmation.
- Display:
  - no-action-yet safety copy;
  - summary;
  - reasons;
  - risk labels;
  - proposed actions formatted by known safe fields;
  - expiry timestamp;
  - command/confirmation ids;
  - approve/cancel actions;
  - loading state;
  - terminal approval/cancel result;
  - user-safe error copy for forbidden/not found/conflict/expired cases.
- Do not render raw provider payload or JSON dumps.

## API Handling Strategy

- Add `approveCommandConfirmation(commandId, confirmationId)`.
- Add `cancelCommandConfirmation(commandId, confirmationId, request)`.
- Use the existing authenticated fetch wrapper and `X-Correlation-ID`.
- In the app shell, disable duplicate taps during in-flight actions and after
  terminal result.
- Refresh household read models after approval because approval may execute
  backend mutations.

## Verification Strategy

- Run `cd clients/mobile && npm run typecheck`.
- Run `git diff --check`.
- Add manual smoke checklist because no mobile unit-test runner exists today.

## Stop Conditions

Move to HOLD instead of patching backend if:

- OpenAPI lacks a required field/endpoint.
- Backend returns a response shape incompatible with accepted contract.
- Mobile compilation requires backend or contract edits.
- Native voice transcript submission requires ASR UI or endpoint changes beyond
  the current mobile scope.

## Gate C Recommendation

GO for APPLY using only the approved mobile and planning files. Backend,
OpenAPI, AI Platform, `answered`, durable pending confirmation restore, and
production rollout remain out of scope.
