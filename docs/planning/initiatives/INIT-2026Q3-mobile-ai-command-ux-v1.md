# Initiative: INIT-2026Q3-mobile-ai-command-ux-v1 — Mobile AI Command UX v1

## Status

Implemented through delegated Gate D GO / LIMITED-GO on 2026-06-16.

See execution evidence:

- `docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.execution.md`
- `docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/gate-d.md`

## Initiative type

Native Mobile Feature / AI Command UX / Contract Integration / Confirmation UX / Safety-Oriented Client

## Owner

HomeTusk product engineering team.

## Target milestone

After backend `natural_command + needs_confirmation` contract and approval/cancel lifecycle; before production rollout of mobile AI command UX.

## Parent / Related initiatives

- Backend contract implementation: `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md`
- Contract spike: `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.md`
- AI Platform 2.1 intake: `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.md`
- Provider acceptance review: `docs/planning/initiatives/INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review.md`
- Mobile foundation: `docs/planning/initiatives/INIT-2026Q3-mobile-client-refactor-foundation.md`
- Native Mobile MVP: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.md`
- Future candidate: read-only `answered` / status-query UX
- Future candidate: production rollout / feature-flag enablement

---

## Sources of Truth

### Backend contract

- Commands OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- Backend contract initiative: `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md`
- Backend contract execution: `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.execution.md`
- Natural command contract spike: `docs/research/ai-command-capabilities/natural-command-contract-spike/**`
- AI Platform integration v2.1: `docs/integration/ai-platform/v2.1/**`

### Mobile

- Mobile README: `clients/mobile/README.md`
- Mobile command feature: `clients/mobile/src/features/command/**`
- Mobile API types: `clients/mobile/src/api/types.ts`
- Mobile API client: `clients/mobile/src/api/**`
- App shell/orchestration: `clients/mobile/src/app/**`

### Product / governance

- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Workflow: `docs/CODEX-WORKFLOW.md`

---

## 1. Problem / Opportunity

HomeTusk backend now has the core AI-command contract foundation:

- `POST /api/v1/commands` accepts `type=natural_command`;
- natural command payload includes `text`, `inputMode`, `locale`, `timezone`, and `referenceInstant`;
- backend can return `needs_confirmation`;
- backend stores pending confirmations in `command_confirmations`;
- backend exposes approve/cancel endpoints;
- approval is initiator-only and revalidates guardrails before mutation;
- provider `confirm` cannot mutate state before explicit HomeTusk approval.

Mobile still behaves like an older command shell:

- text is converted client-side into `create_task` or `complete_task`;
- mobile does not send `natural_command`;
- mobile does not render `needs_confirmation`;
- mobile does not call approve/cancel;
- mobile command cards are generic and do not show proposed actions, risk labels, expiry, or confirmation lifecycle.

The product opportunity is to turn the existing mobile Command tab into the first real user-facing AI-command surface while preserving HomeTusk safety boundaries:

```text
mobile captures intent
→ HomeTusk owns command/confirmation state
→ AI Platform proposes
→ HomeTusk validates/guardrails/executes/clarifies/rejects/confirms
→ mobile renders controlled outcomes and approval/cancel affordances
```

This initiative implements Mobile AI Command UX v1 against the accepted backend contract. It must not call AI Platform directly and must not introduce new backend AI semantics.

---

## 2. Outcome

The mobile client can submit natural commands and render all backend-controlled AI-command outcomes needed for v1:

```text
executed
executed_degraded
needs_input
needs_confirmation
rejected
scheduled, if returned by existing contract
```

The user can:

1. open the mobile Command surface;
2. type or submit an edited voice transcript as a natural command;
3. see executed / degraded / clarify / rejected / confirmation results;
4. approve or cancel a pending confirmation from mobile;
5. see safe retry/loading/error behavior;
6. see recent command history with meaningful labels.

This initiative does not implement `answered`, status-query cards, direct AI chat, production rollout, or direct mobile-to-AI-Platform calls.

---

## 3. Scope

### NOW — Mobile AI Command UX v1

#### 3.1 Mobile request builder

Replace the current client-side `create_task`/`complete_task` text heuristic for the main command composer with a first-class natural command request.

Future default request:

```json
{
  "householdId": "uuid",
  "type": "natural_command",
  "payload": {
    "text": "купи молоко и курицу",
    "inputMode": "text",
    "locale": "ru-RU",
    "timezone": "Europe/Moscow",
    "referenceInstant": "2026-06-16T09:00:00Z",
    "asrTraceId": null
  },
  "source": "mobile",
  "clientTimestamp": "2026-06-16T09:00:00Z"
}
```

Requirements:

- Use existing `POST /api/v1/commands`.
- Mobile must not call AI Platform.
- `payload.text` is the user-reviewed command text.
- `inputMode=text` for typed commands.
- `inputMode=voice_transcript` when submitting an ASR transcript draft.
- `locale`, `timezone`, and `referenceInstant` are required.
- `source=mobile` for typed commands from mobile.
- `source=voice` may be used only when the command text originated from voice flow, if existing backend/mobile convention expects it; Codex must inspect current voice flow and choose one consistent mapping.
- Keep existing structured `complete_task` convenience only if explicitly justified and not used as the primary AI-command path.

Default recommendation:

```text
Main composer sends natural_command.
Legacy done/complete shortcut may be removed or kept as an explicit quick action, but it must not hide natural command semantics.
```

#### 3.2 API types and client support

Update mobile API types to include:

- `natural_command` request payload;
- `needs_confirmation` response;
- confirmation shape:
  - `confirmationId`;
  - `providerConfirmationId`;
  - `summary`;
  - `reasons`;
  - `riskLabels`;
  - `expiresAt`;
  - `proposedActions`;
- confirmation trace shape;
- approve response;
- cancel response;
- approve/cancel API calls.

Expected endpoints:

```text
POST /api/v1/commands
POST /api/v1/commands/{commandId}/confirmations/{confirmationId}/approve
POST /api/v1/commands/{commandId}/confirmations/{confirmationId}/cancel
```

#### 3.3 Command outcome cards

Replace generic command result copy with structured cards for:

- executed;
- executed_degraded;
- needs_input;
- needs_confirmation;
- rejected;
- scheduled if already supported.

Each card should be safe and plain:

- do not expose raw provider payload;
- do not expose provider credentials/prompts;
- do not expose internal stack traces;
- do not claim “AI did X” when HomeTusk only proposed/confirmed.

#### 3.4 Confirmation card

Implement a mobile confirmation card for `needs_confirmation`.

Required UX elements:

- user-safe summary;
- reasons;
- risk labels;
- proposed actions list;
- expiry timestamp;
- approve button;
- cancel button;
- loading state while approve/cancel request is in flight;
- terminal state after approve/cancel;
- error state for expired/stale/forbidden/not found/conflict.

Safety requirements:

- confirmation card must say no action has happened yet;
- approve action must be explicit;
- cancel must never mutate domain entities;
- repeated approve/cancel retries must be safe from client perspective;
- mobile must not execute or simulate proposed actions locally.

#### 3.5 Clarify card / continuation behavior

Current `needs_input` continuation flow may remain if compatible.

Review and update:

- `CommandContinuationCard`;
- continuation input parsing;
- `POST /commands/{commandId}/continue` usage;
- display of required fields and suggestions.

Requirements:

- clarify remains separate from confirmation;
- no approve/cancel controls on `needs_input`;
- no continuation form on `needs_confirmation` unless backend contract later supports it.

#### 3.6 Rejected/degraded handling

Mobile must render:

- `rejected.errorCode`;
- user-safe `reason`;
- `executed_degraded.degradedReason`;
- fallback strategy if present.

Do not treat `rejected` as a crash/error state. It is a controlled command outcome.

#### 3.7 Command history / timeline

Update recent command rendering to distinguish:

- executed;
- needs_input;
- needs_confirmation;
- rejected;
- executed_degraded;
- scheduled.

If the backend does not provide a dedicated command-history endpoint with confirmation details, use local in-session history only and document limitation.

#### 3.8 Voice transcript handoff

If mobile already has voice/ASR draft flow or deep-link handoff, route submitted transcript through `natural_command` instead of `create_task`.

Requirements:

- ASR transcript remains editable before submit;
- no auto-execution after ASR;
- raw audio is not sent to command pipeline;
- `asrTraceId` is passed when available;
- `inputMode=voice_transcript`.

#### 3.9 Local persistence / app memory

Persist only safe UX state:

- recent command display entries;
- last response in command surface;
- pending confirmation card state if needed for app refresh.

Do not persist:

- provider raw payload;
- auth tokens outside existing secure storage;
- AI prompts;
- raw audio.

If durable pending confirmations require a backend read endpoint not present today, document the gap and keep v1 as submit-result-driven.

#### 3.10 Tests / verification

Minimum mobile checks:

- typecheck passes;
- unit tests for request builder:
  - typed natural command;
  - voice transcript natural command;
  - missing text validation;
  - locale/timezone/referenceInstant generation;
- unit tests for outcome formatting:
  - executed;
  - needs_input;
  - needs_confirmation;
  - rejected;
  - executed_degraded;
- API client tests/mocks for approve/cancel if test framework exists;
- emulator/manual smoke checklist:
  - submit natural command;
  - receive executed;
  - receive needs_input;
  - receive rejected;
  - receive needs_confirmation;
  - approve confirmation;
  - cancel confirmation;
  - app refresh/retry behavior.

If mobile test runner is still limited, document exact manual smoke steps in release notes.

#### 3.11 Docs

Update:

- `clients/mobile/README.md`;
- relevant mobile release smoke docs;
- roadmap;
- initiative execution notes.

---

## 4. Explicit Out of Scope

Do not implement:

- backend changes;
- public OpenAPI changes;
- database migrations;
- AI Platform changes;
- direct mobile/web calls to AI Platform;
- mobile `answered` / status-query card;
- natural completion auto-execute;
- natural reschedule auto-execute;
- task-shopping linkage auto-execute beyond whatever backend already controls;
- broad workload redistribution;
- payment/device/external side effects;
- production rollout/config enablement.

Do not change backend contract unless a must-fix bug blocks mobile compilation; if found, stop and record HOLD/recommendation instead of patching backend under this initiative.

---

## 5. Assumptions

- Backend PR for `natural_command + needs_confirmation` is merged.
- OpenAPI/backend contract is the source of truth for mobile API types.
- Mobile must remain a HomeTusk client, not an AI Platform client.
- Backend owns all execution, guardrails, confirmation state, approval/cancel, idempotency, and audit.
- Mobile only renders outcomes and sends explicit user actions.
- `answered` remains blocked.
- Production rollout is separate.

---

## 6. Success Metrics

### User path

- User can submit a typed natural command from mobile.
- User can submit an edited voice transcript as natural command if voice flow exists.
- User can see `needs_confirmation`.
- User can approve a confirmation.
- User can cancel a confirmation.
- User sees clear controlled outcomes for executed, clarify, rejected, degraded, and confirmation.

### Safety

- Mobile never calls AI Platform.
- Mobile never executes proposed actions locally.
- Mobile does not treat confirmation as executed.
- Mobile does not expose raw provider payload.
- Mobile does not bypass backend guardrails.

### Compatibility

- Existing mobile auth/session/household/task/shopping flows still work.
- Existing command continuation flow still works or is intentionally superseded with documentation.
- Existing push/deeplink behavior is not broken.

### Verification

- Mobile typecheck passes.
- Relevant unit tests pass if available.
- Manual smoke checklist exists and is executable.
- No backend/AI Platform files changed.

---

## 7. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Mobile treats `needs_confirmation` as executed | HIGH | Dedicated confirmation card and explicit no-mutation copy |
| Mobile sends old `create_task` wrapper for natural text | HIGH | Replace main composer request builder with `natural_command` |
| Direct mobile-to-AI-Platform shortcut appears | HIGH | Explicitly forbidden; API client uses HomeTusk only |
| Approve/cancel retry creates duplicate mutation | MEDIUM | Backend handles terminal replay; mobile shows loading and disables duplicate taps |
| App refresh loses pending confirmation | MEDIUM | Store last response locally or document backend read gap |
| Voice transcript auto-executes | HIGH | Editable draft + explicit submit only |
| `answered` scope creep | MEDIUM | Keep status/query out of scope |
| Backend contract gap discovered | HIGH | HOLD and recommend backend cleanup, do not patch backend inside mobile initiative |

---

## 8. Expected Files

Codex must inspect repository conventions first, but likely files include:

```text
docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.md
docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.execution.md

docs/planning/strategy/roadmap.md
clients/mobile/README.md

clients/mobile/src/api/types.ts
clients/mobile/src/api/client.ts
clients/mobile/src/features/command/**
clients/mobile/src/app/**
```

Potential test/docs files:

```text
clients/mobile/src/features/command/*.test.ts
clients/mobile/docs/release-smoke-ai-command.md
docs/planning/workpacks/INIT-2026Q3-MOBILE-AI-COMMAND-UX-V1/**
```

Do not modify:

```text
services/backend/**
docs/contracts/http/commands.openapi.yaml
vr_ai_platform/**
```

unless the initiative explicitly moves to HOLD and creates a backend follow-up rather than applying runtime changes.

---

## 9. Exit Criteria

The initiative is complete when:

1. Mobile sends `natural_command` for the main command composer.
2. Mobile supplies `text`, `inputMode`, `locale`, `timezone`, `referenceInstant`, and optional `asrTraceId`.
3. Mobile renders `needs_confirmation`.
4. Mobile can approve a pending confirmation through backend endpoint.
5. Mobile can cancel a pending confirmation through backend endpoint.
6. Mobile renders executed, needs_input, rejected, executed_degraded, and scheduled outcomes without regression.
7. Mobile does not call AI Platform directly.
8. Mobile does not expose raw provider payload.
9. Existing mobile auth/household/task/shopping flows still pass typecheck/smoke.
10. Mobile README/smoke docs are updated.
11. No backend/AI Platform files are changed.
12. Final recommendation for production rollout / `answered` / expiry-read model follow-ups is recorded.

---

## 10. Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | Consumes accepted backend contract; no OpenAPI change expected |
| backend_impact | no | Backend is source-of-truth and should remain unchanged |
| mobile_impact | yes | Main scope |
| ai_platform_impact | no | No provider changes or direct calls |
| security_sensitive | yes | Confirmation approval/cancel UX and household command execution |
| traceability_critical | yes | Correlation ids, command ids, confirmation ids |
| adr_needed | no | Backend architecture already covered; mobile work may document UX decisions |
| diagrams_needed | maybe | Optional mobile state diagram if useful |
| cross_repo | no | HomeTusk repo only |

---

## 11. Anti-Scope-Creep

DO NOT:

- change backend contract;
- change backend runtime;
- change AI Platform;
- add direct mobile-to-AI calls;
- implement `answered`;
- implement production rollout;
- implement broad planner actions;
- execute proposed actions locally;
- hide natural command text inside `create_task.title`;
- overload `needs_input` as confirmation;
- ship mobile confirmation UI without approve/cancel error handling.

---

## 12. Known Backend Hygiene Notes

These are not blockers for this initiative, but Codex should verify whether they affect mobile work:

- `CommandController` Java annotation text may still mention only old command types even though OpenAPI is current.
- DB constraint may allow `add_shopping_item` as command type while Java `CommandType` does not; treat this as backend hygiene, not mobile scope.
- Provider decision id is UUID-typed in some HomeTusk fields while provider schema says string; mobile should rely on HomeTusk response fields, not provider raw ids.

If any of these blocks mobile implementation, stop and recommend a backend hygiene follow-up.

---

## 13. Next Step After Gate A

Codex should:

1. Read this initiative.
2. Read current mobile command feature.
3. Read current mobile API types/client.
4. Read backend OpenAPI for commands and confirmation endpoints.
5. Produce PLAN with:
   - exact mobile files;
   - request builder strategy;
   - response type strategy;
   - confirmation card design;
   - approve/cancel API handling;
   - tests;
   - smoke checklist;
   - risks and stop conditions.
6. Do not APPLY before Gate C.
