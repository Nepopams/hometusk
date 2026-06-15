# Initiative: INIT-2026Q3-ai-platform-2-1-contract-intake — AI Platform 2.1 Contract Intake & Safe Adapter Mapping

## Status

Draft (to be approved at Human Gate A)

## Initiative type

Backend Integration / External Contract Intake / AI Safety / Adapter Mapping / Traceability / Compatibility

## Owner

HomeTusk product engineering team.

## Target milestone

Before HomeTusk `natural_command` runtime, `needs_confirmation`, `answered`, or Mobile AI Command UX.

## Parent / Related initiatives

- HomeTusk AI Command Capability Audit: `docs/planning/initiatives/INIT-2026Q3-ai-command-capability-audit.md`
- HomeTusk AI Command Artifact Gate: `docs/planning/initiatives/INIT-2026Q3-ai-command-artifact-gate.md`
- HomeTusk Provider Domain Planner v1 Acceptance Review: `docs/planning/initiatives/INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review.md`
- AI Platform provider initiative: `vr_ai_platform/docs/planning/initiatives/INIT-2026Q3-domain-planner-v1-contract-and-50-scenario-eval.md`
- Future HomeTusk initiative: `natural_command` contract/runtime
- Future mobile initiative: Mobile AI Command UX v1

---

## Sources of Truth

### HomeTusk

- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Current command HTTP contract: `docs/contracts/http/commands.openapi.yaml`
- Current AI Platform integration docs: `docs/integration/ai-platform/**`
- Backend command pipeline: `services/backend/src/main/java/com/hometusk/commands/**`
- AI Platform decision adapter: `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/**`
- Decision log: `services/backend/src/main/java/com/hometusk/commands/domain/DecisionLog.java`
- Mobile command shell: `clients/mobile/src/features/command/**`

### AI Platform read-only input

- Provider contract version: `2.1.0`
- Provider schemas:
  - `vr_ai_platform/contracts/schemas/command.schema.json`
  - `vr_ai_platform/contracts/schemas/decision.schema.json`
- Provider handoff: `vr_ai_platform/docs/planning/workpacks/ST-056/hometusk-handoff.md`
- Provider 50-scenario eval: `vr_ai_platform/docs/planning/workpacks/ST-052/local-50-scenario-eval-report.json`
- Provider runtime:
  - `vr_ai_platform/graphs/core_graph.py`
  - `vr_ai_platform/routers/v2.py`

---

## 1. Problem / Opportunity

AI Platform now exposes a stronger provider contract than the HomeTusk integration layer currently understands.

Provider-side changes include:

- contract version `2.1.0`;
- first-class `reject`;
- schema-level `confirm`;
- optional `decision_outcome`;
- deterministic eval against 50 HomeTusk-owned scenarios;
- 0 blocker failure scenarios;
- no unsupported auto-execute;
- no cross-household references.

However, HomeTusk remains the product-service and execution authority. Provider success does not automatically mean HomeTusk can consume the new contract safely.

Current HomeTusk risks:

- HomeTusk integration snapshots may still reflect older AI Platform schema/mapping.
- `AiResponseSchemaValidator` may not accept provider `2.1.0`.
- `AiDecisionResponseMapper` may not handle `reject` or `confirm`.
- Provider `confirm` cannot yet map to a first-class HomeTusk `needs_confirmation` response.
- HomeTusk public `/commands` contract still has no `natural_command`.
- Mobile AI UX has no confirmation/answer cards.
- Existing structured command flows must not regress.

This initiative updates HomeTusk as a safe consumer of AI Platform `2.1.0`, without approving natural command runtime or mobile AI UX.

---

## 2. Outcome

HomeTusk can safely understand and classify AI Platform `2.1.0` provider decisions.

The intended outcome:

```text
AI Platform 2.1.0 snapshot imported/documented
→ HomeTusk adapter understands execute / clarify / reject / confirm
→ reject maps to safe HomeTusk rejection
→ confirm maps to safe non-execution HOLD/rejection until needs_confirmation exists
→ existing command flows keep working
→ no natural_command runtime yet
```

The initiative should end with a clear recommendation:

```text
GO / LIMITED-GO / NO-GO / HOLD
```

for the next HomeTusk step:

- `natural_command` contract spike;
- `needs_confirmation` contract;
- AI Platform semantic-quality follow-up;
- or no runtime work yet.

---

## 3. Scope

### NOW — AI Platform 2.1 Contract Intake

#### 3.1 Upstream snapshot and integration docs

Create or update HomeTusk integration package for AI Platform `2.1.0`.

Expected shape:

```text
docs/integration/ai-platform/v2.1/
  README.md
  upstream/
    contracts/
      VERSION
      schemas/
        command.schema.json
        decision.schema.json
  mapping/
    hometusk-to-aiplatform.md
    aiplatform-to-hometusk.md
  examples/
    decision-execute-create-task.json
    decision-execute-shopping-items.json
    decision-clarify.json
    decision-reject.json
    decision-confirm.json
  compatibility.md
```

If the repository convention prefers another versioned path, Codex may choose it, but must document why.

The docs must explicitly state:

- AI Platform `2.1.0` is provider contract input;
- HomeTusk remains execution authority;
- mobile/web must not call AI Platform directly;
- provider tests are necessary but not sufficient for HomeTusk acceptance;
- `answer` remains blocked;
- `confirm` is schema-supported but not HomeTusk-runtime-supported yet.

#### 3.2 Contract drift cleanup

Update or supersede stale AI Platform integration docs.

Known drift to resolve:

- legacy `/decision` versus current `/v1/decide`;
- older wrapper camelCase schemas versus runtime upstream snake_case envelope;
- lack of provider `reject` / `confirm` mapping;
- old `reject_mapped_to_error` language;
- provider repeated singular shopping actions versus HomeTusk `add_shopping_items` taxonomy.

Do not mutate AI Platform upstream docs. Only update HomeTusk-owned integration docs/snapshots.

#### 3.3 Backend adapter compatibility

Update HomeTusk AI Platform client/adapter if needed:

- request capabilities;
- response schema validation;
- provider response mapping;
- DecisionLog raw payload handling;
- graceful behavior for unsupported provider outcomes.

Candidate code areas:

```text
services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionRequest.java
services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionResponseMapper.java
services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiResponseSchemaValidator.java
services/backend/src/main/java/com/hometusk/commands/pipeline/decision/AiPlatformDecisionProvider.java
services/backend/src/main/java/com/hometusk/commands/pipeline/DecisionLogWriter.java
```

Codex must inspect actual files before modifying.

#### 3.4 Provider `reject` mapping

Map first-class provider `reject` to a safe HomeTusk result.

Expected behavior:

```text
provider action=reject
provider decision_outcome=reject
→ HomeTusk DecisionResult.Reject
→ CommandResponse.status = rejected
→ no action execution
→ DecisionLog records raw provider payload, provider decision id, trace id, reason/code
```

Required mapping fields:

- provider code;
- provider reason;
- provider ui_message if present;
- trace id;
- decision id;
- decision version;
- schema version;
- confidence;
- raw provider payload.

If HomeTusk current rejected response cannot carry all fields, record extra fields in DecisionLog and document response limitations.

#### 3.5 Provider `confirm` mapping

HomeTusk does not yet have `needs_confirmation`.

Therefore provider `confirm` must **not execute**.

Required behavior for this initiative:

```text
provider action=confirm
provider decision_outcome=confirm
→ no action execution
→ no mutation
→ DecisionLog records proposed actions and confirmation payload
→ API returns controlled safe result
```

Codex must choose one of these strategies and justify it:

```text
A. Map provider confirm to HomeTusk rejected with code AI_CONFIRMATION_UNSUPPORTED.
B. Map provider confirm to needs_input with policyName AI_CONFIRMATION_REQUIRED, without executing.
C. Feature-flag confirm capability off and treat any unexpected confirm as rejected.
```

Default recommendation:

```text
A + C:
- do not advertise confirm capability by default;
- if confirm still arrives, map to rejected / AI_CONFIRMATION_UNSUPPORTED;
- preserve raw provider payload in DecisionLog;
- defer needs_confirmation to future contract initiative.
```

Do not implement `needs_confirmation` in this initiative.

#### 3.6 Capability negotiation

Review what HomeTusk sends in AI Platform capabilities.

Decide and document:

- Should HomeTusk send `reject` capability?
- Should HomeTusk send `confirm` capability?
- Should `confirm` be feature-flagged?
- Can provider reject unsafe commands even when `reject` is not advertised?
- How does HomeTusk handle unknown future capability values?

Expected default posture:

```text
send:
  - start_job
  - propose_create_task
  - propose_add_shopping_item
  - clarify
  - reject

do not send by default:
  - confirm

feature flag:
  - confirm capability may be enabled later only after HomeTusk needs_confirmation contract exists
```

Codex may revise after code inspection, but must record rationale.

#### 3.7 Compatibility tests

Add backend tests using provider `2.1.0` fixtures.

Minimum test cases:

1. Existing `create_task` AI decision still executes.
2. Existing `propose_add_shopping_item` still executes through internal action executor.
3. Existing `clarify` still maps to `needs_input`.
4. New provider `reject` maps to HomeTusk `rejected`.
5. New provider `confirm` does not execute and maps to controlled safe result.
6. Unknown provider action still rejects safely.
7. Invalid provider schema still rejects safely.
8. Provider failure still degrades/falls back according to existing policy.
9. DecisionLog contains raw provider payload for reject/confirm.
10. No HomeTusk domain mutation happens for reject/confirm.
11. Backward compatibility with provider `2.0`-style responses remains intact, if supported today.

#### 3.8 Integration smoke fixtures

Create fixture files for HomeTusk tests and docs.

Suggested path:

```text
services/backend/src/test/resources/ai-platform/v2.1/
  decision-execute-create-task.json
  decision-execute-shopping-items.json
  decision-clarify.json
  decision-reject.json
  decision-confirm.json
  decision-invalid-unknown-action.json
```

or equivalent repo convention.

#### 3.9 Traceability and observability

Ensure documentation/tests cover:

- provider decision id;
- provider trace id;
- schema version;
- decision version;
- confidence;
- raw decision payload;
- mapping result;
- degraded/fallback behavior;
- no execution for reject/confirm.

No sensitive raw household data should be added to logs beyond current DecisionLog policy.

#### 3.10 Roadmap and service catalog

Update if needed:

- `docs/planning/strategy/roadmap.md`
- `docs/architecture/service-catalog.md`
- AI Platform integration README/indexes.

---

## 4. Explicit Out of Scope

Do not implement:

- HomeTusk `natural_command`;
- `/commands` public contract changes;
- `needs_confirmation`;
- `answered`;
- Mobile AI Command UX;
- mobile confirmation cards;
- mobile answer cards;
- status/query answer;
- natural reschedule auto-execute;
- natural completion auto-execute;
- task-shopping linkage auto-execute;
- non-requester assignment auto-execute;
- direct mobile → AI Platform;
- AI Platform code changes;
- production rollout/config changes.

Do not change AI Platform repository.

Do not treat provider `2.1.0` acceptance as product GO.

---

## 5. Assumptions

- AI Platform provider contract `2.1.0` is the next upstream contract to consume.
- HomeTusk remains final execution authority.
- Provider `reject` is safe to consume as non-mutating rejection.
- Provider `confirm` is not safe to expose as runtime confirmation until HomeTusk has a first-class `needs_confirmation` contract.
- `answer` remains blocked.
- Existing structured commands must remain stable.
- Existing manual fallback/degraded behavior must remain stable.
- Existing mobile command shell must remain unchanged.

---

## 6. Success Metrics

### Contract intake

- AI Platform `2.1.0` snapshot exists in HomeTusk integration docs.
- Mapping docs explain `execute`, `clarify`, `reject`, `confirm`.
- Stale `/decision` mapping is updated or explicitly superseded.

### Backend safety

- Provider `reject` never executes actions.
- Provider `confirm` never executes actions.
- Unknown provider actions remain safe.
- Invalid provider responses remain safe.
- Existing execute/clarify behavior does not regress.

### Traceability

- DecisionLog captures provider raw payload for `reject` and `confirm`.
- Trace ids and decision ids are preserved where available.
- Mapping result is auditable.

### Verification

- Relevant backend unit/integration tests pass.
- Full backend test suite passes or deviations are documented.
- No mobile/web changes.
- No OpenAPI public `/commands` changes unless explicitly approved; default expectation is no public contract change.

---

## 7. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Provider `confirm` accidentally executes | HIGH | Map confirm to controlled non-execution; do not advertise confirm capability by default |
| Provider `reject` treated as unknown action | HIGH | Update schema validator and mapper; add tests |
| Backward compatibility with provider 2.0 breaks | MEDIUM | Add legacy fixture tests where supported |
| HomeTusk starts natural_command too early | HIGH | Explicitly out of scope; roadmap gate |
| Mobile UX starts without backend contract | HIGH | Explicitly out of scope |
| Integration docs remain stale | MEDIUM | Versioned v2.1 docs and supersede old mapping |
| Reject/confirm raw payload leaks sensitive details | MEDIUM | DecisionLog policy review; no extra logs beyond current policy |
| Capability negotiation becomes ambiguous | MEDIUM | Document send/receive policy and add tests |

---

## 8. Expected Files

Codex must inspect repo conventions first, but likely files include:

```text
docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.md
docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.execution.md

docs/integration/ai-platform/v2.1/**
docs/integration/ai-platform/README.md
docs/planning/strategy/roadmap.md
docs/architecture/service-catalog.md

services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/**
services/backend/src/main/java/com/hometusk/commands/pipeline/decision/**
services/backend/src/test/**
services/backend/src/test/resources/ai-platform/v2.1/**
```

Codex may choose a smaller or different set after PLAN, but must justify deviations.

---

## 9. Exit Criteria

This initiative is complete when:

1. AI Platform `2.1.0` provider contract is documented/snapshotted in HomeTusk.
2. HomeTusk integration mapping for `reject` and `confirm` is explicit.
3. Provider `reject` maps to safe HomeTusk rejection.
4. Provider `confirm` maps to safe non-execution.
5. Existing execute/clarify behavior remains compatible.
6. Unknown/invalid provider responses remain safe.
7. Capability negotiation policy is documented.
8. DecisionLog traceability for `reject` and `confirm` is tested or documented.
9. Backend tests cover new provider response shapes.
10. No HomeTusk `natural_command` runtime is added.
11. No `needs_confirmation` or `answered` public API is added.
12. No mobile/web UX changes are made.
13. No AI Platform repository changes are made.
14. Final recommendation for next step is recorded:
    - `natural_command` contract spike;
    - `needs_confirmation` contract;
    - more provider semantic work;
    - or HOLD.

---

## 10. Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | yes-external-provider-snapshot; no-public-HomeTusk-API expected | Consumes AI Platform 2.1; should not change `/commands` |
| backend_impact | yes | Adapter/schema validation/mapping/tests |
| mobile_impact | no | Explicitly out of scope |
| ai_platform_impact | no | Provider repo read-only |
| security_sensitive | yes | AI decisions, rejection/confirmation, raw provider payload |
| traceability_critical | yes | DecisionLog and provider trace ids |
| adr_needed | maybe | If mapping policy is architecture-relevant |
| diagrams_needed | maybe | If integration flow changes materially |
| cross_repo | yes | AI Platform read-only input |

---

## 11. Anti-Scope-Creep

DO NOT:

- implement `natural_command`;
- add `needs_confirmation`;
- add `answered`;
- add Mobile AI Command UX;
- add voice command on mobile;
- add direct mobile → AI Platform;
- alter AI Platform repo;
- change production config;
- execute provider `confirm`;
- silently treat `confirm` as `execute`;
- broaden LIMITED-GO into GO.

---

## 12. Next Step After Gate A

Codex should:

1. Read this initiative.
2. Read HomeTusk AI Platform integration docs.
3. Inspect backend AI Platform adapter code.
4. Inspect provider `2.1.0` schemas/read-only handoff.
5. Produce PLAN with:
   - exact files;
   - contract impact;
   - mapping strategy;
   - capability strategy;
   - tests;
   - rollback;
   - risks.
6. Do not APPLY before Gate C.
