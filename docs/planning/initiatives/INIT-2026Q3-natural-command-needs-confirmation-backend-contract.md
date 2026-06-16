# Initiative: INIT-2026Q3-natural-command-needs-confirmation-backend-contract - Natural Command + Needs Confirmation Backend Contract

## Status

Delivered / current roadmap initiative with delegated Gate D GO.

Backend/API contract foundation slice delivered on 2026-06-16 with delegated
Gate D GO. Approval/cancel lifecycle slice delivered on 2026-06-16 with
delegated Gate D GO.

Client/product readiness remains separately gated: mobile/web UI, expiry
scheduler, `answered`, broader approval policy, and production rollout are not
approved by this initiative.

Delegated Gate A/B/C/D decisions are recorded in
`docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.execution.md`.

## Initiative type

Backend API Contract Implementation / Command Pipeline / AI Safety / Traceability / Compatibility

## Owner

HomeTusk product engineering team.

## Target milestone

Before Mobile AI Command UX v1, before `answered`, and before production rollout
of AI-native command flows.

## Parent / Related initiatives

- AI Command Capability Audit: `docs/planning/initiatives/INIT-2026Q3-ai-command-capability-audit.md`
- AI Command Artifact Gate: `docs/planning/initiatives/INIT-2026Q3-ai-command-artifact-gate.md`
- Provider Domain Planner v1 Acceptance Review: `docs/planning/initiatives/INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review.md`
- AI Platform 2.1 Contract Intake: `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.md`
- Contract Spike: `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.md`
- Draft contract package: `docs/research/ai-command-capabilities/natural-command-contract-spike/`
- Future candidate: Mobile AI Command UX v1
- Future candidate: read-only `answered` / status-query contract

---

## Sources of Truth

### Product and planning

- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- MVP release scope: `docs/planning/releases/MVP.md`
- DoR / DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Workflow: `AGENTS.md`, `docs/CODEX-WORKFLOW.md`

### Contract and architecture

- Commands public contract: `docs/contracts/http/commands.openapi.yaml`
- Contract index: `docs/_indexes/contracts-index.md`
- AI Platform integration v2.1: `docs/integration/ai-platform/v2.1/**`
- Service catalog: `docs/architecture/service-catalog.md`
- Draft natural command contract package:
  `docs/research/ai-command-capabilities/natural-command-contract-spike/**`

### Runtime context

- Backend command pipeline: `services/backend/src/main/java/com/hometusk/commands/**`
- AI Platform decision adapter:
  `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/**`
- Guardrails:
  `services/backend/src/main/java/com/hometusk/commands/pipeline/guardrails/**`
- DecisionLog: `services/backend/src/main/java/com/hometusk/commands/domain/DecisionLog.java`
- Current mobile command shell: `clients/mobile/src/features/command/**` (read-only context)

### AI Platform read-only input

- AI Platform provider contract version `2.1.0`
- First-class provider `reject`
- Schema-level, non-executing provider `confirm`
- Provider `answer` remains blocked
- Provider eval evidence from prior acceptance and intake gates

---

## 1. Problem / Opportunity

The prior contract spike closed with LIMITED-GO for a separate backend contract
implementation initiative. HomeTusk now has a draft contract direction for:

- `type=natural_command` on the existing Commands API boundary;
- a first-class `needs_confirmation` response;
- HomeTusk-owned pending confirmation state;
- provider `confirm` mapping that never mutates before user approval;
- DecisionLog traceability for provider payloads, guardrails, and confirmation lifecycle.

The runtime still does not support those concepts:

- current `CommandType` supports only `create_task` and `complete_task`;
- public `/commands` contract does not accept `natural_command`;
- public command responses do not include `needs_confirmation`;
- provider `confirm` maps to controlled `rejected / AI_CONFIRMATION_UNSUPPORTED`;
- `DecisionLog` is audit evidence, but there is no source of truth for pending confirmation state.

This initiative turns the accepted draft direction into a backend-owned contract
foundation while preserving HomeTusk's execution authority and safety posture.

---

## 2. Outcome

HomeTusk backend has an accepted, tested contract foundation for natural command
and confirmation flows:

```text
POST /api/v1/commands with type=natural_command
-> validated natural command payload
-> AI Platform decision mapping
-> execute / clarify / reject / needs_confirmation
-> provider confirm creates HomeTusk-owned pending confirmation
-> no mutation before explicit approval
-> DecisionLog preserves raw provider payload and mapped outcome
```

The initiative ends with a GO / LIMITED-GO / NO-GO / HOLD decision for the next
step, most likely Mobile AI Command UX v1 or a narrower follow-up slice.

---

## 3. Scope

### NOW - Backend contract implementation

#### 3.1 Public Commands API contract

Update HomeTusk-owned public contract docs for the accepted backend behavior:

- allow `type: natural_command` on `POST /api/v1/commands`;
- define required natural command payload fields:
  - `text`;
  - `inputMode`;
  - `locale`;
  - `timezone`;
  - `referenceInstant`;
  - optional `asrTraceId`;
- define validation failure semantics;
- define backward compatibility with existing structured commands;
- define `needs_confirmation` response shape;
- define approve/cancel confirmation semantics if implemented in this initiative.

The default compatibility posture is additive: existing clients sending
`create_task` or `complete_task` keep working.

#### 3.2 Backend command request handling

Implement `natural_command` as a first-class command type without creating a
separate AI endpoint.

Requirements:

- validate natural command payload explicitly;
- preserve `source`, `clientTimestamp`, and optional ASR trace context;
- require locale/timezone/reference instant rather than guessing date phrases;
- keep manual/degraded fallback safe if AI Platform is unavailable;
- reject unsupported or malformed payloads through existing validation patterns.

#### 3.3 First-class `needs_confirmation`

Implement a backend response outcome for controlled confirmation:

- `status=needs_confirmation`;
- HomeTusk-owned `confirmationId`;
- `commandId`;
- user-safe summary;
- reason/risk labels;
- proposed actions safe for display;
- expiry;
- trace metadata where available.

`needs_confirmation` must not reuse `needs_input`.

#### 3.4 Pending confirmation state

Add explicit HomeTusk-owned pending confirmation state if Gate C confirms the
data-model plan.

Requirements:

- `DecisionLog` remains audit evidence, not the source of truth;
- pending confirmation is household-scoped;
- requester/approval actor is auditable;
- expiry and cancellation are explicit;
- proposed actions are persisted only after schema validation and guardrail pre-checks;
- no raw audio is stored.

#### 3.5 Provider `confirm` mapping

Change HomeTusk behavior from:

```text
provider confirm -> rejected / AI_CONFIRMATION_UNSUPPORTED
```

to the approved backend contract behavior:

```text
provider confirm
-> validate provider payload
-> validate supported proposed actions
-> guardrail-check as proposal
-> create HomeTusk pending confirmation
-> return needs_confirmation
-> no mutation
```

Unsupported provider confirmation payloads must degrade safely to clarify or
reject; they must never execute.

#### 3.6 Confirmation approve/cancel lifecycle

If included by Gate C, implement narrow backend lifecycle operations:

- approve a pending confirmation;
- cancel a pending confirmation;
- reject stale/expired confirmations;
- enforce requester/household authorization;
- preserve idempotency for approval where applicable;
- write DecisionLog/lifecycle audit evidence.

If Gate C finds approve/cancel too large for the first APPLY, this initiative may
close as LIMITED-GO after implementing only creation and contract-safe
`needs_confirmation`, with a follow-up initiative for approval execution.

#### 3.7 Tests

Minimum test evidence:

- existing structured commands still pass;
- `natural_command` request validates required fields;
- missing locale/timezone/reference instant rejects or clarifies safely;
- provider execute/clarify/reject behavior remains compatible;
- provider confirm returns `needs_confirmation` and does not mutate domain data;
- unknown/unsupported confirm payload rejects safely;
- pending confirmation is household-scoped;
- DecisionLog contains raw provider payload and mapped outcome;
- degraded AI behavior remains deterministic.

---

## 4. Explicit Out of Scope

Do not implement in this initiative unless a later gate explicitly changes scope:

- Mobile AI Command UX v1;
- mobile confirmation cards;
- web UI changes;
- `answered` response;
- broad household planning;
- direct mobile/web to AI Platform;
- AI Platform repository changes;
- production rollout/config enablement;
- local LLM implementation;
- silent auto-fix of invalid AI output;
- storing raw audio in command or decision records.

---

## 5. Assumptions

- AI Platform `2.1.0` intake is complete and remains read-only input.
- HomeTusk remains execution authority.
- Existing structured command clients must remain compatible.
- Provider `confirm` is a proposal, never execution.
- `answer` remains blocked.
- Mobile/web clients continue to call HomeTusk only.
- Confirmation runtime may require a small data-model change, but no service
  decomposition is required.

---

## 6. Success Metrics

- Roadmap points to this initiative as current NOW scope.
- Public Commands API contract documents `natural_command` and
  `needs_confirmation` behavior if implemented.
- Backend supports `natural_command` without a separate AI endpoint.
- Provider `confirm` maps to non-mutating `needs_confirmation`.
- Pending confirmation state has an explicit HomeTusk source of truth if runtime
  confirmation is implemented.
- DecisionLog preserves provider raw payload and mapped confirmation outcome.
- Existing execute/clarify/reject structured command behavior does not regress.
- Backend tests cover happy path, confirm no-mutation, validation, and household
  boundary cases.
- Final Gate D records GO / LIMITED-GO / NO-GO / HOLD, evidence, residual risks,
  and next recommended action.

---

## 7. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Confirmation accidentally executes before approval | HIGH | Provider `confirm` maps to pending state only; tests assert no mutation |
| `needs_input` overloaded for confirmation | HIGH | First-class `needs_confirmation` response |
| `DecisionLog` becomes state store | HIGH | Add explicit pending confirmation model or HOLD if data model is not ready |
| Natural command becomes a create-task title hack | HIGH | `type=natural_command` payload has its own validation |
| Scope drifts into mobile UX | HIGH | Mobile/web UI explicitly out of scope |
| Provider semantic mismatches leak into mutations | HIGH | Validate schema, run guardrails as proposal, prefer clarify/reject over guessing |
| Backward compatibility regresses | MEDIUM | Existing structured command tests and additive contract changes |
| Date normalization guesses | MEDIUM | Require locale/timezone/reference instant or clarify |

---

## 8. Expected Files

Planning:

```text
docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md
docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.execution.md
docs/planning/strategy/roadmap.md
docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-NEEDS-CONFIRMATION-BACKEND-CONTRACT/**
```

Likely contract/docs:

```text
docs/contracts/http/commands.openapi.yaml
docs/_indexes/contracts-index.md
docs/architecture/service-catalog.md
docs/adr/**
docs/diagrams/**
```

Likely backend:

```text
services/backend/src/main/java/com/hometusk/commands/**
services/backend/src/main/resources/db/migration/**
services/backend/src/test/**
services/backend/src/test/resources/**
```

Forbidden unless separately gated:

```text
clients/**
docs/integration/ai-platform/v1/upstream/**
C:/Users/user/Documents/projects/VR_AI_Platform/**
```

---

## 9. Exit Criteria

1. Gate A/B/C/D decisions are explicitly recorded with evidence, risks, and rationale.
2. Workpack and PLAN identify exact implementation files and STOP conditions.
3. Contract artifacts are updated for accepted public behavior.
4. Runtime behavior matches the accepted contract scope or the initiative records
   LIMITED-GO/HOLD with rationale.
5. Tests and checks are run or failures are documented.
6. Review gate produces GO / NO-GO before Gate D.
7. Residual risks and next recommended action are recorded.

---

## 10. Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | yes | Accepted Commands API behavior changes are expected |
| backend_impact | yes | Command request handling, provider mapping, response model, tests |
| data_impact | yes/maybe | Pending confirmation source of truth likely needs persistence |
| mobile_impact | no | Mobile/web UI remains out of scope |
| ai_platform_impact | no | Provider repo read-only; HomeTusk adapts to upstream |
| security_sensitive | yes | Confirmation approval, household scope, no-mutation policy |
| traceability_critical | yes | DecisionLog and confirmation lifecycle audit |
| adr_needed | yes/maybe | Required if pending confirmation persistence/lifecycle is implemented |
| diagrams_needed | maybe | Sequence/state diagram if it reduces review risk |
| cross_repo | yes-read-only | AI Platform evidence only |

---

## 11. Anti-Scope-Creep

DO NOT:

- implement mobile or web confirmation UI;
- implement `answered`;
- broaden natural command into autonomous household planning;
- auto-execute provider `confirm`;
- bypass HomeTusk schema validation or guardrails;
- use `DecisionLog` as the only pending confirmation state;
- call AI Platform directly from clients;
- edit AI Platform upstream snapshots or external repo;
- approve production rollout by implication.

---

## 12. Next Step After Gate A

Codex should:

1. Read this initiative, roadmap, DoR/DoD, workflow, and related source artifacts.
2. Inspect Commands API contract and backend command pipeline read-only.
3. Produce or update the implementation workpack.
4. Run Codex PLAN as read-only exploration.
5. Record delegated Gate C GO / NO-GO / HOLD before APPLY.
