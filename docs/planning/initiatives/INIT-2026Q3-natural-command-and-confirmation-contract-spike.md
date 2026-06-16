# Initiative: INIT-2026Q3-natural-command-and-confirmation-contract-spike — Natural Command & Confirmation Contract Spike

## Status

Draft (to be approved at Human Gate A)

## Initiative type

Contract Discovery / Product Architecture / Backend API Design / AI Safety / Mobile Readiness

## Owner

HomeTusk product engineering team.

## Target milestone

Before HomeTusk `natural_command` runtime implementation and before Mobile AI Command UX v1.

## Parent / Related initiatives

- AI Command Capability Audit: `docs/planning/initiatives/INIT-2026Q3-ai-command-capability-audit.md`
- AI Command Artifact Gate: `docs/planning/initiatives/INIT-2026Q3-ai-command-artifact-gate.md`
- Provider Domain Planner v1 Acceptance Review: `docs/planning/initiatives/INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review.md`
- AI Platform 2.1 Contract Intake: `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.md`
- Future candidate: HomeTusk `natural_command` runtime implementation
- Future candidate: Mobile AI Command UX v1
- Future candidate: read-only `answered` / status-query contract

---

## Sources of Truth

### HomeTusk

- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Public commands contract: `docs/contracts/http/commands.openapi.yaml`
- AI Platform integration v2.1: `docs/integration/ai-platform/v2.1/**`
- Backend command pipeline: `services/backend/src/main/java/com/hometusk/commands/**`
- AI Platform adapter: `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/**`
- DecisionLog and traceability: `services/backend/src/main/java/com/hometusk/commands/domain/DecisionLog.java`
- Mobile command feature: `clients/mobile/src/features/command/**`

### AI Platform read-only input

- AI Platform contract version `2.1.0`
- First-class provider `reject`
- Schema-level provider `confirm`
- Provider `answer` remains blocked
- Provider handoff/eval evidence from `vr_ai_platform` PR `Codex/domain planner v1 contract eval gate`

---

## 1. Problem / Opportunity

HomeTusk can now safely consume AI Platform `2.1.0` decisions at the adapter level:

- provider `reject` maps to safe HomeTusk rejection;
- provider `confirm` maps to controlled non-execution rejection with `AI_CONFIRMATION_UNSUPPORTED`;
- existing execute/clarify flows remain compatible;
- no HomeTusk public API or mobile UX has changed.

This is a necessary integration foundation, but it is not enough for a usable AI-command product experience.

The product needs first-class HomeTusk contracts for:

1. A natural command request:
   - user writes or speaks household text;
   - HomeTusk sends it through the existing command pipeline;
   - AI Platform proposes structured decisions;
   - HomeTusk validates, guardrails, executes, clarifies, confirms, or rejects.
2. A confirmation state:
   - AI Platform or HomeTusk determines that a proposed plan is understood but too risky/ambiguous to execute immediately;
   - HomeTusk returns a controlled `needs_confirmation` response;
   - mobile/web can show confirmation cards;
   - no mutation happens until explicit user approval.

Without this contract spike, runtime implementation risks becoming a pile of ad-hoc mappings:

- natural text hidden inside `create_task.title`;
- provider `confirm` flattened into `rejected` forever;
- no durable confirmation lifecycle;
- no mobile state model;
- no clear DecisionLog traceability.

This initiative designs the contract boundary before runtime APPLY.

---

## 2. Outcome

This initiative produces implementation-ready contract artifacts for future HomeTusk runtime work.

Expected outcome:

```text
HomeTusk natural_command contract draft
+ needs_confirmation response draft
+ confirmation lifecycle model
+ provider confirm mapping rules
+ guardrails/DecisionLog/mobile readiness requirements
+ GO / LIMITED-GO / NO-GO / HOLD for runtime implementation
```

This initiative does **not** implement runtime behavior.

---

## 3. Scope

### NOW — Contract Spike

Create a contract spike package under:

```text
docs/research/ai-command-capabilities/natural-command-contract-spike/
  README.md
  natural-command-request-contract-v0.md
  command-response-outcomes-v0.md
  needs-confirmation-contract-v0.md
  confirmation-lifecycle-v0.md
  provider-confirm-mapping-v0.md
  guardrails-policy-v0.md
  decisionlog-traceability-v0.md
  mobile-state-contract-dependencies-v0.md
  openapi-delta-draft.yaml
  implementation-readiness-decision.md
```

Codex may adjust paths after PLAN if there is a better repository convention, but must justify deviations.

#### 3.1 Natural command request contract

Design the future HomeTusk-facing request shape.

Default direction:

```json
{
  "type": "natural_command",
  "householdId": "uuid",
  "source": "mobile",
  "clientTimestamp": "2026-06-16T12:00:00Z",
  "payload": {
    "text": "купи молоко и курицу",
    "inputMode": "text",
    "locale": "ru-RU",
    "timezone": "Europe/Moscow",
    "referenceInstant": "2026-06-16T12:00:00+03:00",
    "asrTraceId": null
  }
}
```

Design questions:

- Add `type: natural_command` to existing `/api/v1/commands`, or create a separate endpoint?
- Required fields vs optional fields.
- Text source: typed, voice transcript, imported shortcut.
- Locale/timezone/reference instant policy.
- ASR trace handling without raw audio.
- Backward compatibility with current structured commands.
- Validation failure behavior.
- Idempotency/correlation behavior.

Default recommendation to test:

```text
Use existing POST /api/v1/commands with type=natural_command.
Do not create a separate AI endpoint.
Mobile/web still call HomeTusk only.
```

#### 3.2 Command response outcome model

Define the target command response states for AI-command flows.

Current supported states include:

- `executed`;
- `executed_degraded`;
- `scheduled`;
- `needs_input`;
- `rejected`.

Target design should evaluate adding:

- `needs_confirmation`;
- later `answered`.

This initiative must decide:

- whether `needs_confirmation` should be a new public response status;
- whether it should reuse `needs_input` temporarily;
- which fields are required for mobile/web confirmation cards;
- how `rejected` should carry provider reason/code safely;
- whether `answered` remains out of scope.

Default recommendation to test:

```text
Add a future first-class needs_confirmation response.
Do not overload needs_input for confirmation.
Keep answered out of scope until read-only answer contract starts.
```

#### 3.3 Confirmation contract

Design the future `needs_confirmation` response shape.

Required concepts:

- confirmation id;
- command id;
- proposed actions;
- user-safe summary;
- risk/reason labels;
- expiry;
- approve/cancel semantics;
- no mutation before approval;
- stale confirmation handling;
- household membership/auth checks on approval;
- idempotency on approval;
- audit trail.

Candidate response:

```json
{
  "commandId": "uuid",
  "status": "needs_confirmation",
  "confirmation": {
    "confirmationId": "uuid-or-provider-id",
    "summary": "Create a task for another household member.",
    "reasons": ["Non-requester assignment requires confirmation."],
    "expiresAt": "2026-06-16T12:10:00Z",
    "proposedActions": [
      {
        "type": "create_task",
        "title": "Clean kitchen",
        "assigneeId": "uuid",
        "zoneId": "uuid",
        "dueDate": null
      }
    ]
  },
  "trace": {
    "providerDecisionId": "...",
    "providerTraceId": "...",
    "schemaVersion": "2.1.0",
    "decisionVersion": "mvp1-graph-0.1"
  }
}
```

The draft must not be added to public OpenAPI as accepted contract yet unless the initiative explicitly changes scope through Gate C. Default is **draft only**.

#### 3.4 Confirmation lifecycle

Design lifecycle states and transitions.

Candidate states:

```text
CREATED
PENDING_CONFIRMATION
CONFIRMED
CANCELLED
EXPIRED
REJECTED
EXECUTED
FAILED
```

Design questions:

- Reuse `Command` table/statuses or add a future confirmation record?
- Store provider proposed actions in `DecisionLog.rawDecisionPayload`, new table, or command payload?
- How approve/cancel endpoints should look later.
- Who can approve?
- Can a different household member approve?
- What happens if the underlying entity changes before approval?
- How long confirmations live.
- What gets logged.

Default recommendation to test:

```text
Use a future explicit confirmation/pending-command model, not only DecisionLog.
DecisionLog is audit evidence, not the source of truth for pending confirmation state.
```

#### 3.5 Provider confirm mapping

Define future mapping from AI Platform `2.1.0` provider `confirm` into HomeTusk `needs_confirmation`.

Current intake behavior:

```text
provider confirm → rejected / AI_CONFIRMATION_UNSUPPORTED
```

Future candidate:

```text
provider action=confirm
provider decision_outcome=confirm
→ validate schema
→ validate proposed actions are supported
→ guardrails pre-check as proposal
→ create pending confirmation
→ return needs_confirmation
→ no mutation
```

The spike must define:

- fields required from provider;
- fields HomeTusk must add/determine;
- unsupported proposed action behavior;
- guardrail interaction;
- expiry ownership;
- provider confirmation id vs HomeTusk confirmation id.

#### 3.6 Guardrails and policy model

Define which natural-command actions are allowed to auto-execute and which require confirmation.

Baseline trust corridor:

```text
auto-execute candidates:
  - clear create_task
  - clear multi-item shopping addition

confirmation required:
  - non-requester assignment
  - task-shopping linkage
  - reschedule
  - completion of another user's task
  - batch planning
  - broad workload redistribution

reject:
  - unsupported action
  - unsafe/impossible request
  - cross-household reference
  - unverifiable entity
  - payment/device/external side effect

answer:
  - blocked until read-only answer contract exists
```

The artifact must explicitly cover examples such as:

- `я помыл посуду закрой`;
- `я вынес мусор вместо Пети`;
- `в среду надо встретить газовщика`;
- `к ужину купи молоко и курицу`;
- `назначь всем по 20 задач сегодня ночью`.

For date expressions, the spike must define that `referenceInstant`, `timezone`, and locale are mandatory for normalization or the system must clarify.

#### 3.7 DecisionLog and traceability

Define minimum trace requirements for runtime implementation:

- command id;
- correlation id;
- provider decision id;
- provider trace id;
- provider schema version;
- provider decision version;
- raw provider payload;
- mapped HomeTusk outcome;
- guardrail result;
- confirmation id if created;
- confirmation approval/cancel/expiry event;
- actor performing approval/cancel;
- no raw audio.

Design whether current `DecisionLog` is sufficient or whether future schema changes are required.

#### 3.8 Mobile state dependencies

Define fields mobile/web will need later, without implementing mobile.

States:

- executed card;
- clarify card;
- rejected card;
- confirmation card;
- degraded card;
- pending confirmation timeline item.

The artifact must clearly state:

```text
No mobile implementation in this initiative.
Mobile AI UX remains blocked until backend contract is accepted and implemented.
```

#### 3.9 OpenAPI delta draft

Create a draft OpenAPI delta, but do not modify the accepted public contract.

Expected file:

```text
docs/research/ai-command-capabilities/natural-command-contract-spike/openapi-delta-draft.yaml
```

It should include:

- `natural_command` request draft;
- `needs_confirmation` response draft;
- possible future approve/cancel endpoints as non-binding proposals;
- examples;
- compatibility notes.

It must be marked:

```text
DRAFT ONLY — not an accepted public API contract.
```

#### 3.10 Implementation readiness decision

Produce a final recommendation:

```text
GO / LIMITED-GO / NO-GO / HOLD
```

Possible outcomes:

```text
GO: start runtime implementation for natural_command + needs_confirmation.
LIMITED-GO: start only natural_command request + reject/clarify/execute; hold confirmation runtime.
LIMITED-GO: start only needs_confirmation backend contract; hold natural command runtime.
NO-GO: more provider/contract work required.
HOLD: missing security/product decision.
```

The recommendation must identify the next initiative and its non-goals.

---

## 4. Explicit Out of Scope

No runtime implementation in this initiative:

- no Java backend behavior change;
- no migration;
- no public OpenAPI accepted contract change;
- no `natural_command` execution;
- no confirmation approval/cancel endpoint implementation;
- no `needs_confirmation` public response implementation;
- no `answered` response;
- no mobile/web UI;
- no direct mobile → AI Platform;
- no AI Platform repo changes;
- no production rollout/config changes.

This initiative may create draft contract artifacts only.

---

## 5. Assumptions

- AI Platform `2.1.0` intake is complete.
- HomeTusk can safely consume provider `reject` and `confirm` as non-executing outcomes today.
- HomeTusk does not yet expose `confirm` as a user confirmation UX.
- HomeTusk remains execution authority.
- AI Platform remains external planner/provider.
- Mobile/web remain HomeTusk clients only.
- `answer` is blocked until separate read-only answer contract work.
- Provider intent quality still has non-blocker mismatch buckets; runtime contracts must prefer safe outcomes over semantic overreach.

---

## 6. Success Metrics

- Natural command request draft exists.
- Response outcome model draft exists.
- `needs_confirmation` contract draft exists.
- Confirmation lifecycle model exists.
- Provider confirm mapping rules exist.
- Guardrails policy matrix exists.
- DecisionLog/traceability requirements exist.
- Mobile state dependencies exist.
- OpenAPI delta draft exists and is marked draft-only.
- Final implementation readiness decision exists.
- No runtime/backend/mobile/AI Platform changes are made.

---

## 7. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Spike becomes runtime implementation | HIGH | Docs-only scope; no Java/OpenAPI accepted contract changes |
| Confirmation lifecycle is underspecified | HIGH | Required lifecycle artifact and approve/cancel design questions |
| Mobile UX starts too early | HIGH | Mobile state dependency only; no implementation |
| Natural command reuses create_task title hack | HIGH | Explicit natural_command contract draft |
| Provider confirm is treated as execute | HIGH | Explicit no-mutation confirmation mapping |
| Date normalization guesses | HIGH | referenceInstant/timezone/locale required or clarify |
| DecisionLog becomes pending-state source of truth | MEDIUM | Decide explicit pending confirmation model |
| Answer/status scope creep | MEDIUM | answer remains blocked |

---

## 8. Expected Files

```text
docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.md
docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.execution.md

docs/research/ai-command-capabilities/natural-command-contract-spike/
  README.md
  natural-command-request-contract-v0.md
  command-response-outcomes-v0.md
  needs-confirmation-contract-v0.md
  confirmation-lifecycle-v0.md
  provider-confirm-mapping-v0.md
  guardrails-policy-v0.md
  decisionlog-traceability-v0.md
  mobile-state-contract-dependencies-v0.md
  openapi-delta-draft.yaml
  implementation-readiness-decision.md
```

Optional updates:

```text
docs/planning/strategy/roadmap.md
```

Do not modify:

```text
docs/contracts/http/commands.openapi.yaml
services/backend/src/main/java/**
clients/**
vr_ai_platform/**
```

unless Gate C explicitly changes the initiative scope, which is not expected.

---

## 9. Exit Criteria

The initiative is complete when:

1. All expected contract spike artifacts exist.
2. The natural command request shape is proposed.
3. The response outcome model is proposed.
4. `needs_confirmation` response shape is proposed.
5. Confirmation lifecycle and state ownership are proposed.
6. Provider confirm mapping is proposed.
7. Guardrails policy matrix is proposed.
8. Date/time normalization policy is addressed.
9. DecisionLog/traceability requirements are addressed.
10. Mobile state dependencies are addressed.
11. OpenAPI delta draft exists and is clearly non-binding.
12. Final readiness decision is recorded.
13. No runtime/backend/mobile/AI Platform implementation is made.
14. Next recommended initiative is explicit.

---

## 10. Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | draft-only | No accepted public OpenAPI change in this spike |
| backend_impact | no | Docs-only unless future initiative starts implementation |
| mobile_impact | no | Mobile dependencies only |
| ai_platform_impact | no | Provider repo read-only |
| security_sensitive | yes | Confirmation, authorization, AI-command execution |
| traceability_critical | yes | DecisionLog, confirmation lifecycle, provider trace ids |
| adr_needed | maybe | Future runtime may need ADR |
| diagrams_needed | maybe | Optional sequence/state diagram if useful |
| cross_repo | yes | AI Platform read-only input |

---

## 11. Anti-Scope-Creep

DO NOT:

- implement backend runtime;
- change accepted public OpenAPI;
- implement `natural_command`;
- implement `needs_confirmation`;
- implement approve/cancel endpoints;
- implement mobile cards;
- implement `answered`;
- call AI Platform directly from mobile;
- change AI Platform repo;
- add production rollout/config;
- treat this spike as product GO.

---

## 12. Next Step After Gate A

Codex should:

1. Read this initiative.
2. Read AI Platform `2.1` integration package.
3. Inspect current `/commands` OpenAPI and command response models.
4. Inspect command pipeline, DecisionLog, guardrails and mobile command feature as read-only context.
5. Produce a docs-only PLAN with exact artifact files.
6. Do not APPLY before Gate C.
