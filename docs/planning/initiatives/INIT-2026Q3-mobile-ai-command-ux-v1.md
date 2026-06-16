# Initiative: INIT-2026Q3-mobile-ai-command-ux-v1 — Mobile AI Command UX v1

## Status

Draft (to be approved at Human Gate A)

## Initiative type

Mobile Feature Implementation / AI Command UX / Contract Consumer / Confirmation UX / Safety-Critical Client Flow

## Owner

HomeTusk product engineering team.

## Target milestone

After backend `natural_command` + `needs_confirmation` contract Gate D GO and before any production rollout of mobile AI-command UX.

## Parent / Related initiatives

- Backend contract implementation: `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md`
- Contract spike: `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.md`
- AI Platform 2.1 intake: `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.md`
- Mobile foundation: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.md`
- Mobile refactor foundation: `docs/planning/initiatives/INIT-2026Q3-mobile-client-refactor-foundation.md`
- AI command research: `docs/research/ai-command-capabilities/**`
- Future candidate: Mobile voice natural command UX
- Future candidate: `answered` / status-query cards
- Future candidate: production rollout / feature flag enablement

---

## Sources of Truth

### Backend contract

- `docs/contracts/http/commands.openapi.yaml`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.execution.md`
- `docs/adr/022-pending-command-confirmation-state.md`
- `docs/diagrams/sequence-natural-command-needs-confirmation.md`

### Mobile code

- `clients/mobile/README.md`
- `clients/mobile/App.tsx`
- `clients/mobile/src/app/AppShell.tsx`
- `clients/mobile/src/api/**`
- `clients/mobile/src/features/command/**`
- `clients/mobile/src/features/tasks/**`
- `clients/mobile/src/features/shopping/**`
- `clients/mobile/src/shared/**`

### Product / governance

- `docs/planning/strategy/product-goal.md`
- `docs/planning/strategy/roadmap.md`
- `docs/_governance/dor.md`
- `docs/_governance/dod.md`
- `docs/CODEX-WORKFLOW.md`
- `AGENTS.md`

---

## 1. Problem / Opportunity

HomeTusk backend now supports the AI-command contract foundation:

- `POST /api/v1/commands` accepts `type=natural_command`;
- `natural_command` payload requires `text`, `inputMode`, `locale`, `timezone`, and `referenceInstant`;
- backend may return `needs_confirmation`;
- backend stores HomeTusk-owned pending confirmation state;
- backend supports approve/cancel endpoints for confirmations;
- provider `confirm` does not mutate until explicit HomeTusk approval;
- approval revalidates guardrails before execution.

The native mobile client still behaves like a deterministic command shell:

- arbitrary text is converted into `create_task` title;
- only `done` / `complete` triggers `complete_task` matching;
- `needs_confirmation` is not represented in mobile types or UI;
- approve/cancel endpoints are not called from mobile;
- outcomes are displayed through generic cards;
- mobile does not use `natural_command` as the first-class command mode.

This blocks the actual product experience: mobile should become the first real user-facing AI command surface, but it must do so through HomeTusk backend contracts only and without direct AI Platform coupling.

---

## 2. Outcome

Mobile AI Command UX v1 lets a household member type a natural command in the native app and receive controlled, contract-backed outcomes:

```text
User types natural command
  -> mobile sends type=natural_command to HomeTusk
  -> backend returns executed / needs_input / rejected / needs_confirmation / executed_degraded
  -> mobile renders an appropriate state card
  -> if needs_confirmation, user can approve or cancel
  -> mobile calls HomeTusk approve/cancel endpoint
  -> mobile shows final executed / rejected / cancelled result
```

The user-visible goal is a working mobile natural command path for the narrow backend-supported corridor, not a generic chatbot.

---

## 3. Scope

### NOW — Mobile AI Command UX v1

#### 3.1 Mobile command request builder

Replace the current default `text -> create_task.title` behavior for the Command tab.

Future behavior:

- default composer submission sends `type=natural_command`;
- `payload.text` is the user-reviewed text;
- `payload.inputMode` is `text` for typed commands;
- `payload.locale` is derived from device/app locale or safe default;
- `payload.timezone` is derived from device timezone;
- `payload.referenceInstant` is generated at submission time;
- `source` remains `mobile`;
- `householdId` remains selected household.

Legacy structured affordances may remain if intentionally exposed, but must not silently hijack natural language:

- `done ...` / `complete ...` may keep current local complete-task shortcut only if it remains explicitly documented and tested;
- otherwise route all typed text through `natural_command`.

Codex must inspect current mobile code and decide whether to preserve the `done/complete` shortcut in v1. Default recommendation: preserve it as a visible compatibility shortcut only if it does not block natural command testing.

#### 3.2 Mobile API types and client methods

Update mobile API types/client wrappers for accepted backend contract:

- `CommandRequest.type = natural_command`;
- `NaturalCommandPayload`;
- `CommandResponse.status = needs_confirmation`;
- `CommandNeedsConfirmationResponse`;
- `CommandConfirmation`;
- `CommandConfirmationProposedAction`;
- `CommandConfirmationTrace`;
- `CommandConfirmationApprovalResponse`;
- `CommandConfirmationCancelResponse`;
- approve/cancel endpoints:
  - `POST /api/v1/commands/{commandId}/confirmations/{confirmationId}/approve`;
  - `POST /api/v1/commands/{commandId}/confirmations/{confirmationId}/cancel`.

Mobile must not import or depend on AI Platform schemas directly.

#### 3.3 Command state cards

Replace or extend the generic command outcome card with contract-aware cards:

- executed card;
- degraded card;
- clarify card;
- rejected card;
- confirmation card;
- approval/cancel result card.

The UI should stay minimal but must show enough information to be safe:

- command status;
- user-safe summary;
- question for `needs_input`;
- rejection reason/error code;
- confirmation summary;
- confirmation reasons/risk labels;
- proposed actions in display-safe form;
- expiry timestamp;
- approve/cancel buttons;
- loading state while approve/cancel is in flight;
- disabled state after terminal outcome.

#### 3.4 Confirmation UX

Implement confirmation card behavior:

- render `needs_confirmation` response;
- display proposed actions without raw provider payload;
- approve button calls backend approve endpoint;
- cancel button calls backend cancel endpoint;
- repeated taps must be guarded client-side while request is in flight;
- terminal response replaces or updates the card;
- expired/stale/server rejected responses must be displayed as controlled rejected/error states;
- no local execution or client-side mutation is performed.

Mobile must treat backend as final authority. It must never execute proposed actions locally.

#### 3.5 Clarification continuation compatibility

Existing `needs_input` continuation should keep working.

If current continuation UI uses generic key/value parsing, it may remain for v1, but should be isolated from confirmation UX. Do not overload continuation for confirmation approval.

#### 3.6 Command history / timeline

Update recent command history display enough to represent new statuses:

- `needs_confirmation`;
- approval/cancel result if command history model exposes it;
- no raw provider trace display.

If backend does not yet expose pending confirmation list/read model, do not invent one client-side. v1 may show only the current response and existing recent commands.

#### 3.7 Local app memory

Preserve existing local/session behavior.

For v1:

- current command text may stay in composer until submitted;
- clear composer after successful submit unless response needs editing/continuation;
- pending confirmation response should remain visible while the Command tab is mounted;
- do not persist provider raw payload;
- do not persist auth-sensitive data;
- no offline approval queue in v1.

#### 3.8 UX copy and safety language

Use clear product language, not AI hype.

Examples:

- `Needs confirmation` — HomeTusk needs your approval before making changes.
- `Approve` — approve and execute after HomeTusk checks current rules.
- `Cancel` — discard this proposed change.
- `Rejected` — HomeTusk did not make changes.
- `Needs input` — HomeTusk needs more details.

Do not claim the assistant understands more than the backend contract supports.

#### 3.9 Tests / verification

Add or update tests where the mobile repo supports them.

Minimum coverage:

- request builder creates `natural_command` with text/inputMode/locale/timezone/referenceInstant;
- `done/complete` shortcut behavior is preserved or intentionally removed with tests/docs;
- response formatting handles `needs_confirmation`;
- confirmation card renders summary/reasons/proposed actions;
- approve calls correct endpoint and updates displayed outcome;
- cancel calls correct endpoint and updates displayed outcome;
- rejected/needs_input/executed/degraded still render;
- no direct AI Platform URL or schema usage exists in mobile code.

If the mobile repo has no UI test runner, Codex must still add tests for pure TypeScript helpers and document manual smoke coverage.

#### 3.10 Documentation

Update:

- `clients/mobile/README.md` with the AI command flow;
- roadmap;
- initiative execution notes;
- mobile smoke checklist, if existing convention supports it.

---

## 4. Explicit Out of Scope

Do not implement:

- direct mobile/web calls to AI Platform;
- AI Platform repository changes;
- backend contract changes unless a blocking contract bug is discovered and explicitly HOLDed;
- production rollout/config changes;
- push notification changes;
- `answered` / status-query cards;
- mobile voice capture/transcription changes;
- streaming ASR;
- offline command queue;
- editing pending proposed actions;
- broader household approval policy;
- natural completion/reschedule/linkage auto-execute;
- broad planner or agent UX;
- generic chatbot UI.

Voice transcript integration may be documented as future work. This initiative focuses on typed mobile natural command and backend-backed confirmation UX.

---

## 5. Assumptions

- Backend natural command and confirmation contract is merged and available on `main`.
- Mobile continues to use HomeTusk backend API only.
- Backend is final authority for execution, confirmation state, guardrails and audit.
- AI Platform is not directly visible to mobile.
- `needs_confirmation` response contains display-safe proposed actions.
- Backend approve/cancel endpoints are initiator-only and revalidate guardrails.
- `answered` is not available.
- Current mobile command feature is isolated under `clients/mobile/src/features/command/**` after mobile refactor.

---

## 6. Success Metrics

### Functional

- User can submit a typed natural command from mobile.
- Mobile sends `type=natural_command`, not `create_task.title` fallback, for ordinary typed text.
- Mobile renders `executed`, `needs_input`, `rejected`, `executed_degraded`, and `needs_confirmation` outcomes.
- User can approve a pending confirmation from mobile.
- User can cancel a pending confirmation from mobile.
- Mobile never calls AI Platform directly.

### Safety

- Confirmation card does not execute locally.
- Approve/cancel buttons are disabled while request is in flight.
- Terminal states prevent duplicate client-side requests.
- Raw provider payload and provider credentials are never stored or displayed.
- Unsupported backend responses degrade to safe display, not app crash.

### Verification

- Mobile TypeScript checks pass.
- Relevant helper tests pass, if test runner exists or is added.
- Backend contract is not modified.
- No backend Java files are changed except docs-only references if unavoidable.
- Manual smoke checklist covers submit, clarify, reject, confirmation approve, confirmation cancel.

---

## 7. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Mobile still wraps natural text as `create_task` | HIGH | Replace default builder path with `natural_command`; add request-builder tests |
| Confirmation UI implies execution before backend approval | HIGH | Card copy and state model emphasize no changes before approval |
| Duplicate approve taps create confusing state | MEDIUM | Disable buttons while in flight and rely on backend terminal replay |
| Mobile depends on provider internals | HIGH | Use HomeTusk API types only; no AI Platform imports/URLs |
| Existing command flow regresses | MEDIUM | Preserve tests/smoke for create/complete/needs_input/rejected |
| No UI test runner | MEDIUM | Test pure helpers and document manual smoke path |
| `answered` sneaks into UX | LOW | Explicit out-of-scope and fallback status rendering |
| Voice scope creep | MEDIUM | Typed natural command v1 only; voice transcript UX is future work |

---

## 8. Expected Files

Codex must inspect actual mobile structure first, but likely files include:

```text
docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.md
docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.execution.md
docs/planning/strategy/roadmap.md
clients/mobile/README.md
clients/mobile/src/api/types.ts
clients/mobile/src/api/client.ts or equivalent command API module
clients/mobile/src/features/command/**
clients/mobile/src/shared/**
clients/mobile/**/__tests__/** or equivalent helper tests
```

Codex may choose a smaller or different file set after PLAN, but must justify deviations.

---

## 9. Exit Criteria

This initiative is complete when:

1. Mobile default typed command submission uses `natural_command`.
2. Mobile request includes `text`, `inputMode`, `locale`, `timezone`, and `referenceInstant`.
3. Mobile API types include `needs_confirmation` and confirmation approve/cancel responses.
4. Mobile renders confirmation cards from backend response.
5. Approve calls backend approve endpoint and updates UI.
6. Cancel calls backend cancel endpoint and updates UI.
7. Existing `needs_input`, `rejected`, `executed`, and `executed_degraded` rendering remains functional.
8. No direct AI Platform calls or provider schema dependencies exist in mobile.
9. Mobile typecheck/tests pass or deviations are documented.
10. Manual smoke checklist exists for the mobile AI command flow.
11. No backend runtime/API changes are made by this initiative.
12. No production rollout/config changes are made.
13. Next recommended initiative is recorded.

---

## 10. Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | Consumes accepted backend contract; no backend API change expected |
| backend_impact | no | Backend is source of truth but should not change here |
| mobile_impact | yes | Command UX, API types, confirmation cards |
| ai_platform_impact | no | Mobile must not touch provider |
| security_sensitive | yes | Confirmation approval/cancel, no direct provider access |
| traceability_critical | yes | Correlation id and command ids must remain visible enough for support |
| adr_needed | no | Existing ADR-022 covers pending confirmation state |
| diagrams_needed | no | Existing sequence diagram covers backend flow; mobile docs may add simple flow if useful |
| cross_repo | no | HomeTusk repo only |

---

## 11. Anti-Scope-Creep

DO NOT:

- call AI Platform from mobile;
- implement generic assistant chat;
- add mobile voice/ASR changes;
- add `answered` cards;
- modify backend contracts;
- modify backend Java runtime;
- add production rollout/config;
- implement broad planner UX;
- let mobile execute proposed actions locally;
- store raw provider payload on device.

---

## 12. Next Step After Gate A

Codex should:

1. Read this initiative.
2. Read mobile README and command feature code.
3. Read accepted Commands OpenAPI only as contract input.
4. Inspect current mobile API client/types and command request builder.
5. Produce PLAN with:
   - exact files;
   - request builder strategy;
   - response/card state model;
   - approve/cancel API methods;
   - tests/typecheck/manual smoke;
   - rollback;
   - stop conditions.
6. Do not APPLY before Gate C.
