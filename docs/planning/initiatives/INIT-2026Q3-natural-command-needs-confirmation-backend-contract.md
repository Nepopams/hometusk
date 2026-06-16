# Initiative: INIT-2026Q3-natural-command-needs-confirmation-backend-contract — Natural Command & Needs Confirmation Backend Contract

## Status

Draft (to be approved at Human Gate A)

## Initiative type

Backend Contract Implementation / Command Pipeline / AI Safety / Confirmation Lifecycle / Traceability

## Owner

HomeTusk product engineering team.

## Target milestone

Before Mobile AI Command UX v1 and before any production rollout of AI-command natural execution.

## Parent / Related initiatives

- Contract spike: `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.md`
- AI Platform 2.1 intake: `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.md`
- Provider acceptance review: `docs/planning/initiatives/INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review.md`
- Natural command spike package: `docs/research/ai-command-capabilities/natural-command-contract-spike/**`
- AI Platform integration package: `docs/integration/ai-platform/v2.1/**`
- Future candidate: Mobile AI Command UX v1
- Future candidate: read-only `answered` / status-query contract

---

## Sources of Truth

### Contract spike artifacts

- `docs/research/ai-command-capabilities/natural-command-contract-spike/README.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/natural-command-request-contract-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/command-response-outcomes-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/needs-confirmation-contract-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/confirmation-lifecycle-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/provider-confirm-mapping-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/guardrails-policy-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/decisionlog-traceability-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/mobile-state-contract-dependencies-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/openapi-delta-draft.yaml`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/implementation-readiness-decision.md`

### HomeTusk implementation sources

- Public command contract: `docs/contracts/http/commands.openapi.yaml`
- Command controller/service: `services/backend/src/main/java/com/hometusk/commands/**`
- Command DTOs: `services/backend/src/main/java/com/hometusk/commands/dto/**`
- Command domain: `services/backend/src/main/java/com/hometusk/commands/domain/**`
- Command pipeline: `services/backend/src/main/java/com/hometusk/commands/pipeline/**`
- AI Platform adapter: `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/**`
- Guardrails: `services/backend/src/main/java/com/hometusk/commands/pipeline/guardrails/**`
- Action executor: `services/backend/src/main/java/com/hometusk/commands/pipeline/ActionExecutor.java`
- Database migrations: `services/backend/src/main/resources/db/migration/**`
- Backend tests: `services/backend/src/test/**`

### Read-only context

- Mobile command feature: `clients/mobile/src/features/command/**`
- AI Platform provider repository: read-only only if needed

---

## 1. Problem / Opportunity

HomeTusk now has the planning and provider foundations required for a first-class natural command backend contract:

- AI Platform `2.1.0` can return `execute`, `clarify`, `reject`, and schema-supported `confirm` decisions.
- HomeTusk safely consumes provider `reject` and maps provider `confirm` to controlled non-execution today.
- The contract spike recommends using existing `POST /api/v1/commands` with `type=natural_command` rather than a separate AI endpoint.
- The contract spike recommends a first-class `needs_confirmation` response instead of overloading `needs_input`.
- The contract spike recommends an explicit pending confirmation model because `DecisionLog` is audit evidence, not workflow state.

The current product gap is that HomeTusk still cannot expose this as a real backend contract:

- `CommandRequest.type` does not support `natural_command`.
- `CommandResponse.status` does not support `needs_confirmation`.
- There is no pending confirmation persistence model.
- Provider `confirm` cannot become a user-visible confirmation card because there is no approval/cancel lifecycle.
- Mobile cannot safely build confirmation UI until backend contract exists.

This initiative implements the backend contract and persistence foundation while keeping mobile UI, `answered`, broad planning, and production rollout out of scope.

---

## 2. Outcome

HomeTusk supports natural command submission and first-class confirmation state at the backend/API contract level.

Target flow:

```text
POST /api/v1/commands type=natural_command
  -> HomeTusk validates natural payload
  -> HomeTusk builds AI Platform request with text, input mode, locale, timezone, reference instant, context and capabilities
  -> AI Platform returns execute / clarify / reject / confirm
  -> HomeTusk validates provider response
  -> HomeTusk guardrails and domain validation remain final authority
  -> HomeTusk returns one controlled outcome:
       executed
       needs_input
       rejected
       needs_confirmation
       executed_degraded, only for safe deterministic fallback if already supported
```

For provider `confirm`:

```text
provider confirm
  -> validate provider schema
  -> validate proposed actions are supported
  -> guardrail pre-check as proposal
  -> create HomeTusk pending confirmation
  -> return needs_confirmation
  -> no domain mutation until explicit approval
```

For confirmation approval:

```text
POST approve endpoint
  -> household membership/authz check
  -> idempotency check
  -> status/expiry/stale check
  -> revalidate proposed actions
  -> re-run guardrails
  -> execute approved actions
  -> mark confirmation terminal
  -> audit
```

---

## 3. Scope

### NOW — Backend contract and confirmation foundation

#### 3.1 Public command contract update

Update accepted public command contract:

- add `natural_command` as a `CommandRequest.type` variant;
- define natural command payload fields:
  - `text`;
  - `inputMode`;
  - `locale`;
  - `timezone`;
  - `referenceInstant`;
  - optional `asrTraceId`;
- add response status `needs_confirmation`;
- add `NeedsConfirmationResponse` schema;
- add confirmation approve/cancel endpoints;
- keep existing structured `create_task` and `complete_task` compatibility.

Default endpoint direction from spike:

```text
POST /api/v1/commands with type=natural_command
```

Draft approve/cancel direction from spike:

```text
POST /api/v1/commands/{commandId}/confirmations/{confirmationId}/approve
POST /api/v1/commands/{commandId}/confirmations/{confirmationId}/cancel
```

Codex must inspect current controller/router conventions and choose the final accepted shape in PLAN before APPLY.

#### 3.2 Backend DTO/domain model update

Implement backend DTO/domain support for:

- `CommandType.NATURAL_COMMAND` or equivalent;
- natural command payload validation;
- `CommandStatus.PENDING_CONFIRMATION` or equivalent state handling;
- `CommandResponse.status=needs_confirmation`;
- confirmation response DTOs;
- confirmation approval/cancel DTOs;
- stable public error codes for confirmation lifecycle.

Required public error codes to consider:

```text
CONFIRMATION_EXPIRED
CONFIRMATION_CANCELLED
CONFIRMATION_ALREADY_APPROVED
CONFIRMATION_NOT_FOUND
CONFIRMATION_FORBIDDEN
CONFIRMATION_STALE
CONFIRMATION_ACTION_UNSUPPORTED
CONFIRMATION_VALIDATION_FAILED
AI_CONFIRMATION_UNSUPPORTED, retained only for unexpected legacy/no-state paths
```

#### 3.3 Pending confirmation persistence

Create explicit pending confirmation model/table.

Do not use only `DecisionLog` as the source of truth.

Candidate entity:

```text
CommandConfirmation
  id
  commandId
  householdId
  initiatorId
  providerConfirmationId
  providerDecisionId
  providerTraceId
  schemaVersion
  decisionVersion
  status
  summary
  reasonsJson
  riskLabelsJson
  proposedActionsJson
  expiresAt
  approvedBy
  approvedAt
  cancelledBy
  cancelledAt
  expiryProcessedAt
  createdAt
  updatedAt
```

Codex may refine field names and types after inspecting existing persistence conventions.

Required status lifecycle:

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

If implementation chooses a smaller status set, it must justify the simplification and preserve no-mutation safety.

#### 3.4 Natural command pipeline integration

Implement natural command routing through existing command pipeline.

Requirements:

- no separate AI endpoint;
- mobile/web still call HomeTusk only;
- AI Platform remains external provider;
- HomeTusk remains execution authority;
- existing structured command behavior must not regress;
- `natural_command` text must not be hidden inside `create_task.title`;
- natural payload must provide `locale`, `timezone`, and `referenceInstant` for date expressions or the command must clarify/reject according to validation stage.

Default AI Platform capabilities for natural command should include:

```text
start_job
propose_create_task
propose_add_shopping_item
clarify
reject
confirm
```

Rationale: this initiative introduces HomeTusk confirmation state, so provider `confirm` may be advertised after Gate C approval.

If Codex decides to keep `confirm` capability disabled by default, it must explain how provider `confirm` is tested and how future enablement happens.

#### 3.5 Provider decision mapping

Update mapping behavior:

| Provider outcome | HomeTusk behavior |
|---|---|
| `execute/start_job` | existing guarded execution path, narrow corridor only |
| `clarify` | `needs_input`, no mutation |
| `reject` | `rejected`, no mutation |
| `confirm` | create pending confirmation and return `needs_confirmation`, no mutation |
| unknown/invalid | `rejected`, no mutation |
| `answer` | blocked / rejected until separate answer contract |

For `confirm`, HomeTusk must:

- validate proposed action types;
- map provider proposed actions into HomeTusk proposed action shape;
- reject unsupported proposed actions safely;
- pre-check guardrails where possible as proposals;
- store raw provider payload in `DecisionLog`;
- store public pending confirmation state separately;
- never execute proposed actions before approval.

#### 3.6 Confirmation approval and cancel

Implement approval/cancel contract.

Approve must:

- load confirmation by `commandId` + `confirmationId` or accepted resource shape;
- verify household membership;
- enforce approval policy;
- check status is pending;
- check not expired;
- revalidate proposed actions against current household state;
- re-run guardrails;
- execute supported actions transactionally where possible;
- mark confirmation terminal;
- update command status/result;
- write audit/DecisionLog or confirmation event evidence;
- support idempotency.

Cancel must:

- load confirmation;
- verify household membership;
- return idempotent terminal result if already cancelled/expired/executed as appropriate;
- mark cancelled;
- audit actor/time;
- never mutate domain entities.

Default approval policy for v0:

```text
Only the command initiator may approve/cancel, as long as they are still a household member.
Different household member approval is out of scope until product policy is accepted.
```

If Codex proposes a different policy, it must be explicit and justified.

#### 3.7 Supported proposed actions for approval

V0 approval execution may support only:

```text
create_task
add_shopping_item
```

Everything else should remain rejected or unsupported:

```text
complete_task
reschedule_task
link_task_shopping
batch planning
workload redistribution
payment/device/external side effects
```

Natural completion/reschedule/linkage may appear in confirmation proposals but must not execute unless explicitly accepted in PLAN and covered by tests.

Default recommendation: do not execute them in this initiative.

#### 3.8 Guardrails and date policy

Guardrails must apply before execution and after approval.

Confirmation is not a bypass.

Date policy:

- natural command payload must include `referenceInstant`, `timezone`, and `locale`;
- missing/invalid date context must not be guessed;
- provider due/date values must be validated by HomeTusk;
- deadlines in the past must clarify/reject according to existing guardrails;
- weekday phrases such as `в среду надо встретить газовщика` require deterministic normalization or clarification.

This initiative does not need to build advanced date normalization unless required for accepted tests. It must not silently rely on model/server current date.

#### 3.9 DecisionLog and traceability

Implementation must capture:

- natural command input metadata;
- provider raw payload;
- provider decision id, trace id, schema version, decision version;
- mapped HomeTusk outcome;
- pending confirmation id;
- confirmation approval/cancel/expiry events;
- actor ids and timestamps;
- guardrail results;
- no raw audio.

If current `DecisionLog` cannot represent all fields directly, store critical data in confirmation table and raw JSON, and document limitations.

#### 3.10 Tests

Minimum tests:

1. `natural_command` execute create_task happy path.
2. `natural_command` execute multi-item shopping happy path if supported by existing executor.
3. `natural_command` clarify returns `needs_input` and no mutation.
4. `natural_command` reject returns `rejected` and no mutation.
5. Provider confirm returns `needs_confirmation`, creates pending confirmation, no mutation.
6. Pending confirmation stores provider ids, trace, schema/decision version and proposed actions.
7. Approve confirmation executes supported actions and marks terminal state.
8. Approve expired confirmation rejects without mutation.
9. Approve cancelled confirmation rejects/idempotently returns terminal result.
10. Cancel pending confirmation marks cancelled and no mutation.
11. Unauthorized/non-member approve/cancel is forbidden.
12. Unsupported proposed action in confirm is rejected/no pending executable confirmation.
13. Unknown/invalid provider response remains safe.
14. Existing `create_task` structured command still works.
15. Existing `complete_task` structured command still works.
16. Existing `needs_input` continuation still works.
17. DecisionLog/confirmation audit evidence exists for confirm/approve/cancel.
18. Idempotency on natural command submission and approval is covered or explicitly documented if not currently supported.

#### 3.11 Documentation and roadmap

Update:

- accepted OpenAPI contract;
- contract index;
- backend runbook if needed;
- AI Platform integration mapping if behavior changes;
- service catalog if a new confirmation model/table is introduced;
- roadmap and initiative execution/closure docs.

---

## 4. Explicit Out of Scope

Do not implement:

- mobile/web UI;
- mobile confirmation cards;
- read-only `answered` / status query response;
- natural completion auto-execute;
- natural reschedule auto-execute;
- task-shopping linkage auto-execute;
- broad workload redistribution;
- external payment/device/order side effects;
- direct mobile/web calls to AI Platform;
- AI Platform repository changes;
- production rollout/config changes.

Do not expand provider capabilities beyond AI Platform `2.1.0` contract without separate provider initiative.

---

## 5. Assumptions

- AI Platform `2.1.0` intake is complete and merged.
- Contract spike is complete and merged.
- AI Platform provider can emit schema-valid `confirm` under `2.1.0`.
- HomeTusk can advertise `confirm` after pending confirmation model exists.
- Existing command pipeline can be extended without creating a separate AI endpoint.
- Existing idempotency/correlation infrastructure can be reused or extended.
- Mobile will consume the future contract later, but is not modified now.

---

## 6. Success Metrics

### Contract

- Public OpenAPI supports `natural_command` request.
- Public OpenAPI supports `needs_confirmation` response.
- Public OpenAPI supports confirmation approve/cancel or an accepted equivalent.
- Existing structured command contract remains backward compatible.

### Backend

- Natural commands route through existing command pipeline.
- Provider `confirm` creates pending confirmation instead of rejection when capability is enabled.
- No domain mutation occurs before confirmation approval.
- Approval executes only supported actions after revalidation and guardrails.
- Cancel and expiry are safe and auditable.

### Safety

- Unsupported actions never execute.
- Unknown/invalid provider outputs never execute.
- Confirmation does not bypass guardrails.
- Cross-household references are rejected.
- Non-member approval/cancel is forbidden.

### Verification

- Backend tests pass.
- OpenAPI/schema validation passes.
- Migration tests pass if applicable.
- No mobile/web changes.
- No AI Platform repo changes.

---

## 7. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Confirmation executes before explicit approval | HIGH | Pending state, tests, no mutation before approve |
| DecisionLog used as workflow source of truth | HIGH | Explicit `CommandConfirmation` model/table |
| Natural command reintroduces create_task.title hack | HIGH | First-class `natural_command` payload |
| Approve executes stale provider proposal | HIGH | Revalidate proposed actions and guardrails on approval |
| Non-requester approval creates social issues | MEDIUM | v0 approval limited to initiator only |
| Mobile starts before backend contract stabilizes | HIGH | Mobile out of scope |
| `answered` scope creep | MEDIUM | Explicitly blocked |
| Provider semantic quality still imperfect | MEDIUM | Narrow supported actions and safe reject/clarify/confirm policy |
| Public API churn | MEDIUM | Contract-first with OpenAPI review and compatibility notes |

---

## 8. Expected Files

Codex must inspect actual repo conventions first, but likely files include:

```text
docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md
docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.execution.md

docs/contracts/http/commands.openapi.yaml
docs/_indexes/contracts-index.md
docs/planning/strategy/roadmap.md
docs/architecture/service-catalog.md

services/backend/src/main/java/com/hometusk/commands/domain/**
services/backend/src/main/java/com/hometusk/commands/dto/**
services/backend/src/main/java/com/hometusk/commands/controller/**
services/backend/src/main/java/com/hometusk/commands/service/**
services/backend/src/main/java/com/hometusk/commands/pipeline/**
services/backend/src/main/resources/db/migration/**
services/backend/src/test/**
```

Codex may choose a different exact file set after PLAN, but must justify deviations.

---

## 9. Exit Criteria

This initiative is complete when:

1. `natural_command` is accepted in public command contract.
2. Natural command request validation is implemented.
3. Natural command routes through existing command pipeline.
4. `needs_confirmation` response is accepted in public contract.
5. Pending confirmation persistence model exists.
6. Provider `confirm` creates pending confirmation and returns `needs_confirmation` without mutation.
7. Confirmation approve endpoint exists and executes only supported actions after authz, status, expiry, validation and guardrails.
8. Confirmation cancel endpoint exists and is idempotent/safe.
9. Confirmation expiry is enforced lazily or by documented mechanism.
10. Unsupported proposed actions cannot execute.
11. Existing structured command flows still work.
12. DecisionLog/confirmation traceability is covered by tests or documentation.
13. Backend/OpenAPI/migration tests pass.
14. No mobile/web UI changes are made.
15. No AI Platform repo changes are made.
16. Final recommendation for Mobile AI Command UX readiness is recorded.

---

## 10. Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | yes | Public `/commands` contract changes |
| backend_impact | yes | DTOs, domain, persistence, service, controller, pipeline |
| mobile_impact | no | Public contract changes may require future mobile work, but no UI/client implementation now |
| ai_platform_impact | no | Provider repo read-only |
| security_sensitive | yes | Approval/cancel, household authz, no-mutation confirmation |
| traceability_critical | yes | Confirmation lifecycle and AI decision audit |
| adr_needed | maybe | Pending confirmation persistence may need ADR-lite if architecture boundary changes |
| diagrams_needed | maybe | Confirmation lifecycle diagram useful if implementation adds new entity/table |
| migration_needed | likely | Pending confirmation persistence model |
| cross_repo | yes | AI Platform read-only input |

---

## 11. Anti-Scope-Creep

DO NOT:

- implement mobile/web UI;
- implement `answered`;
- implement broad natural planner actions;
- auto-execute natural completion/reschedule/linkage;
- allow non-requester approval unless explicitly accepted;
- use DecisionLog as the only pending confirmation state store;
- call AI Platform directly from mobile/web;
- change AI Platform repo;
- enable production rollout/config;
- treat provider confidence as execution permission.

---

## 12. Next Step After Gate A

Codex should:

1. Read this initiative.
2. Read the natural command contract spike package.
3. Inspect current command OpenAPI, DTOs, controller, service, pipeline, guardrails, ActionExecutor, DecisionLog and tests.
4. Produce PLAN with:
   - exact contract deltas;
   - persistence model;
   - approve/cancel endpoint choice;
   - provider confirm mapping strategy;
   - supported proposed action list;
   - tests;
   - migrations;
   - rollback;
   - risks and stop conditions.
5. Do not APPLY before Gate C.
