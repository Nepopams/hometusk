# Initiative: INIT-2026Q3-ai-command-artifact-gate — AI Command Artifact Gate for Domain Planner v1

## Status

Draft (to be approved at Human Gate A)

## Initiative type

Product Architecture / Contract Discovery / Eval Design / Privacy Gate / Cross-repo Planning

## Owner

HomeTusk product engineering team.

## Target milestone

Before AI Platform Domain Planner v1 APPLY, before HomeTusk `natural_command`, and before Mobile AI Command UX.

## Parent / Related initiatives

- Completed discovery baseline: `docs/planning/initiatives/INIT-2026Q3-ai-command-capability-audit.md`
- Completed discovery execution index: `docs/planning/initiatives/INIT-2026Q3-ai-command-capability-audit.execution.md`
- Research pack: `docs/research/ai-command-capabilities/README.md`
- External research comparison: `docs/research/ai-command-capabilities/external-research-comparison-2026-06-15.md`
- Final audit recommendation: `docs/research/ai-command-capabilities/recommendation.md`
- Related mobile refactor: `docs/planning/initiatives/INIT-2026Q3-mobile-client-refactor-foundation.md`
- Future provider initiative: AI Platform Domain Planner v1 — Narrow Household Command Corridor
- Future consumer initiative: HomeTusk `natural_command` contract
- Future client initiative: Mobile AI Command UX v1

---

## Sources of Truth

- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- HomeTusk Commands Contract: `docs/contracts/http/commands.openapi.yaml`
- HomeTusk AI Platform External Contract: `docs/contracts/external/ai-platform.decision.openapi.yaml`
- HomeTusk AI Platform Integration Docs: `docs/integration/ai-platform/v1/**`
- AI Command Capability Audit Pack: `docs/research/ai-command-capabilities/**`
- Backend Command Pipeline: `services/backend/src/main/java/com/hometusk/commands/**`
- Mobile Command Feature: `clients/mobile/src/features/command/**`
- Provider repository for read-only context: `Nepopams/vr_ai_platform`

---

## 1. Problem / Opportunity

The AI Command Capability Audit closed with **LIMITED-GO**: HomeTusk should proceed only toward a narrow AI Platform Domain Planner v1 corridor for simple task creation and shopping item addition.

The audit and external research comparison agree on the target direction:

- build a command layer, not generic AI chat;
- keep HomeTusk as validation, guardrails, execution, and audit authority;
- keep AI Platform as planner/decision provider;
- use a single domain planner before considering multi-agent architecture;
- keep the first trust corridor narrow;
- treat confidence as an audit/routing signal, not execution permission;
- require golden scenarios, evals, traceability, and privacy boundaries before broad Mobile AI Command UX.

However, the audit also identified blockers that make direct Domain Planner v1 implementation premature:

- decision/action taxonomy is proposed but not yet accepted as an implementation gate;
- golden scenarios are markdown seed scenarios, not machine-readable fixtures;
- eval rubric and deterministic grader expectations are not formalized;
- privacy, retention, provider governance, and allowed context boundaries need explicit artifact-gate review;
- `natural_command` is only a gap/direction, not a draft accepted contract;
- `confirm` and `answer` are target outcomes but not implemented or contractually accepted;
- HomeTusk AI Platform integration docs have known drift around legacy `/decision`, wrapper schemas, runtime snake_case envelope, and reject/action semantics;
- Mobile AI state model is not yet accepted.

This initiative turns the completed research baseline into an implementation-ready artifact package before any provider-side or HomeTusk runtime work begins.

---

## 2. Outcome (what changes for product/team)

No production feature is delivered.

The outcome is an accepted artifact gate package that can be handed to the future AI Platform Domain Planner v1 initiative and later HomeTusk `natural_command` work.

After this initiative, the team should have:

- accepted decision/action taxonomy v0;
- a `natural_command` contract draft, explicitly not implemented;
- machine-readable golden scenario fixtures v0;
- eval rubric v0;
- privacy and retention questions for provider planning;
- mobile AI state matrix v0;
- provider planner readiness checklist;
- documented AI Platform integration doc drift and cleanup requirements;
- provider initiative brief for `vr_ai_platform`.

The initiative does **not** approve APPLY for Domain Planner v1, `natural_command`, or Mobile AI UX. It prepares the gate package required before those APPLY stages.

---

## 3. Scope (Now / Next / Later)

### NOW — Artifact / contract / eval gate package

Create a documentation package under:

```text
docs/research/ai-command-capabilities/domain-planner-v1-gate/
```

Expected artifacts:

```text
README.md
natural-command-contract-v0-draft.md
decision-action-taxonomy-accepted-v0.md
golden-scenarios-fixtures-v0/
eval-rubric-v0.md
privacy-and-retention-questions.md
mobile-ai-state-matrix-v0.md
provider-planner-readiness-checklist.md
hometusk-ai-platform-integration-doc-drift.md
provider-initiative-brief.md
```

#### 3.1 Accepted decision/action taxonomy

Promote the proposed taxonomy into an accepted artifact-gate baseline.

Decision outcomes:

```text
execute
clarify
confirm
reject
answer
```

Action corridor v0:

```text
auto-execute candidate:
  - create_task
  - add_shopping_items

clarify:
  - ambiguous task title
  - missing shopping item
  - missing or ambiguous list/source
  - ambiguous assignee/zone/deadline
  - incomplete household context

confirm:
  - task + shopping linkage
  - inferred non-requester assignment
  - broad/batch planning
  - reschedule
  - actions outside the narrow corridor but inside HomeTusk domain

reject:
  - unsafe or impossible actions
  - unsupported action types
  - cross-household or unverifiable references
  - direct AI execution bypassing HomeTusk

answer:
  - read-only status/query commands, blocked until answer response contract exists
```

This taxonomy remains an artifact-gate baseline. It must not modify runtime contracts in this initiative.

#### 3.2 `natural_command` contract draft

Draft a future HomeTusk-facing natural command contract.

The draft must be explicit that it is **not** an OpenAPI/runtime contract yet.

It should cover at least:

- request shape direction;
- payload fields:
  - text;
  - inputMode;
  - locale;
  - timezone;
  - referenceInstant;
  - optional asrTraceId;
  - selected household context assumptions;
- response/outcome shape direction:
  - execute;
  - clarify;
  - confirm;
  - reject;
  - answer;
- proposed action shape direction;
- trace/audit fields;
- compatibility notes with existing `POST /api/v1/commands`.

The draft must not change `docs/contracts/http/commands.openapi.yaml`.

#### 3.3 Machine-readable golden scenario fixtures

Convert the current seed golden scenarios into machine-readable fixtures.

Preferred structure:

```text
golden-scenarios-fixtures-v0/
  README.md
  context-fixtures-v0.yaml
  golden-scenarios-v0.yaml
```

Minimum scenario set:

- GS-001 simple task: `убрать кухню сегодня вечером`
- GS-002 shopping: `купи молоко и курицу`
- GS-003 shopping with list/source: `добавь в ашан мусорные пакеты и редьку`
- GS-004 task + shopping linkage: `к ужину купи молоко и курицу`
- GS-005 assignment: `Пете завтра вынести мусор`
- GS-006 reschedule: `перенеси уборку ванной на выходные`
- GS-007 batch planning: `распредели уборку на выходные`
- GS-008 ambiguous prep: `надо подготовиться к гостям`
- GS-009 unsafe overload: `назначь всем по 20 задач сегодня ночью`
- GS-010 status/query: `что у нас сегодня по дому?`

Each fixture should include:

- id;
- input text;
- locale;
- timezone;
- reference instant;
- context fixture id;
- expected intent;
- expected canonical outcome;
- expected proposed actions;
- expected clarify/confirm behavior;
- forbidden assumptions;
- HomeTusk responsibility;
- AI Platform responsibility;
- failure modes;
- UX recommendation.

The current 10 scenarios are seed only. The gate package must state that Domain Planner v1 acceptance should expand to at least 50 scenarios before broad rollout or auto-exec beyond the narrow corridor.

#### 3.4 Eval rubric v0

Define evaluation criteria for provider planner and HomeTusk integration readiness.

Minimum rubric dimensions:

- schema validity;
- decision outcome correctness;
- intent correctness;
- entity extraction correctness;
- item boundary preservation;
- no forbidden assumptions;
- clarify over guessing;
- unsupported action not auto-executed;
- unsafe broad assignment rejected;
- no cross-household leakage;
- no mutation for answer-style commands;
- trace fields present;
- deterministic graders for critical outcomes;
- LLM-as-judge only for soft UX qualities, if used at all.

Include future operational metrics:

- execution precision;
- wrong-object mutation rate;
- unauthorized execution rate;
- clarification resolution rate;
- confirmation accept/cancel rate;
- user correction and undo/revert rate;
- p50/p95 latency;
- drift by planner/model/prompt version.

#### 3.5 Privacy and retention questions

Define questions and constraints that must be answered before provider implementation or broader rollout.

Must cover:

- allowed context fields;
- forbidden context fields;
- raw audio handling;
- raw text/transcript retention;
- provider prompt/response retention;
- region/provider/model provenance;
- prompt versioning;
- model/planner versioning;
- audit payload retention;
- household data minimization;
- cross-household leakage prevention;
- device token/auth token/invite token exclusion;
- whether any zero-data-retention-like requirement exists.

#### 3.6 Mobile AI state matrix v0

Define future mobile states without implementing them.

Minimum states:

- composer;
- processing;
- executed result card;
- clarify card;
- confirmation card;
- rejected card;
- answered card;
- degraded card;
- timeline entry.

For each state, define:

- backend outcome required;
- required payload shape;
- user action;
- retry/edit/continue behavior;
- safety notes.

This artifact should explicitly state that mobile must not call AI Platform directly and must not treat ASR transcript as execution permission.

#### 3.7 Provider planner readiness checklist

Create a checklist for the future provider-side initiative in `vr_ai_platform`.

Minimum readiness items:

- adopt or map accepted HomeTusk decision taxonomy;
- support `execute`, `clarify`, `reject`, and optionally `confirm` for the narrow v1 corridor;
- support `create_task` and multi-item `add_shopping_items`;
- define timezone-aware date/time proposal behavior;
- preserve schema validation;
- preserve fallback/degradation behavior;
- run HomeTusk golden scenario fixtures;
- return trace id / decision version / confidence as audit fields;
- avoid broad multi-agent planner as first production brain;
- keep AI Platform mutation-free.

#### 3.8 Integration doc drift note

Document current HomeTusk integration doc drift and required cleanup before runtime work.

Known drift areas include:

- legacy `/decision` versus current `/v1/decide`;
- wrapper schemas versus runtime upstream snake_case envelope;
- wrapper schema action coverage versus runtime `propose_add_shopping_item` mapping;
- reject/error taxonomy mismatch;
- stale internal proposed action schema.

This initiative may document cleanup requirements but must not change runtime contracts.

#### 3.9 Provider initiative brief

Prepare a short brief that can be copied into the future AI Platform initiative.

It should include:

- goal;
- narrow corridor;
- accepted input artifacts;
- required provider outputs;
- non-goals;
- expected tests/evals;
- HomeTusk ownership boundaries;
- no direct mutation by provider.

---

### NEXT — AI Platform Domain Planner v1

Not part of this initiative.

Likely follow-up after this artifact gate:

- created in `vr_ai_platform`;
- provider-owned implementation;
- consumes HomeTusk artifact package as input;
- produces structured decisions for narrow task/shopping corridor;
- passes HomeTusk golden fixtures as provider regression.

---

### LATER — HomeTusk `natural_command` and Mobile AI UX

Not part of this initiative.

Future HomeTusk work only after Domain Planner v1 and contract governance:

- add `natural_command` to HomeTusk contract if approved;
- add `needs_confirmation` / `answered` response contracts if approved;
- update backend adapter and guardrails;
- update mobile command feature to render structured cards, clarify chips, confirmation cards, answer cards, degraded states, and timeline.

---

## 4. In Scope (explicit)

- Docs-only artifact gate.
- Accepted taxonomy document.
- Natural command contract draft.
- Machine-readable golden scenario fixtures.
- Eval rubric.
- Privacy/retention/provider governance questions.
- Mobile AI state matrix.
- Provider readiness checklist.
- Integration doc drift note.
- Provider initiative brief.
- Optional concise execution index for this initiative after Gate A.

---

## 5. Out of Scope (explicit)

- No production code changes.
- No backend implementation.
- No mobile implementation.
- No web implementation.
- No OpenAPI changes.
- No HomeTusk runtime contract changes.
- No AI Platform code changes.
- No AI Platform schema changes.
- No prompt tuning.
- No Domain Planner implementation.
- No `natural_command` implementation.
- No `needs_confirmation` or `answered` implementation.
- No Mobile AI Command UX implementation.
- No multi-agent production architecture.
- No direct mobile → AI Platform integration.

---

## 6. Cross-repo ownership and commit policy

This initiative is committed in the HomeTusk repository.

Codex may inspect `vr_ai_platform` as read-only context if available.

Codex must not:

- modify files in `vr_ai_platform`;
- commit to `vr_ai_platform`;
- vendor large provider snapshots into HomeTusk;
- change provider schemas;
- treat provider implementation details as HomeTusk product acceptance by themselves.

The future AI Platform Domain Planner v1 work must be a separate provider-side initiative or PR.

---

## 7. Assumptions

- AI Command Capability Audit is accepted as the current baseline.
- Final recommendation remains **LIMITED-GO**.
- HomeTusk remains the product-service and execution authority.
- AI Platform remains planner/decision provider.
- Mobile and web remain clients of HomeTusk, not AI Platform.
- External research is secondary product/industry evidence.
- Repo-grounded audit remains canonical for current-state claims.
- Domain Planner v1 should start with a narrow task/shopping corridor.

---

## 8. Success Metrics

The initiative succeeds when the team can hand one artifact package to the future provider-side Domain Planner v1 work and answer:

1. What taxonomy must provider output map to?
2. What actions are accepted for v0?
3. Which actions can auto-execute, clarify, confirm, reject, or answer?
4. What does a natural command request/decision contract look like directionally?
5. What exact golden fixtures should provider run?
6. What is a pass/fail for provider decisions?
7. What privacy/retention questions must be answered before implementation or rollout?
8. What future mobile states must backend contracts support?
9. What integration documentation drift blocks implementation?
10. What exact scope should the AI Platform Domain Planner v1 provider initiative take?

---

## 9. Constraints / Guardrails

- Keep recommendation as LIMITED-GO.
- Do not turn draft contracts into runtime contracts.
- Do not add implementation.
- Prefer clarify/confirm over guessing.
- Treat confidence as audit/routing signal only.
- Keep HomeTusk as final authority for validation, guardrails, execution, and audit.
- Keep provider stateless and mutation-free.
- Keep mobile/web away from direct AI Platform calls.
- Use machine-readable fixtures before implementation.
- Make privacy and retention explicit before broader AI-command rollout.

---

## 10. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Artifact gate becomes implementation work | HIGH | Explicit out-of-scope; docs-only; no runtime contract changes |
| Draft contract is mistaken for accepted OpenAPI | HIGH | Label as draft; no OpenAPI modification |
| Golden fixtures are too small | MEDIUM | Mark 10 as seed; require expansion to 50+ before broad rollout |
| Provider initiative starts from stale docs | HIGH | Add integration doc drift artifact and readiness checklist |
| Privacy questions are deferred too long | HIGH | Make privacy/retention document required in gate package |
| Mobile AI UX starts before backend outcomes exist | HIGH | Mobile state matrix documents dependency on backend outcomes |
| Multi-agent hype re-enters scope | MEDIUM | Provider brief explicitly recommends single domain planner first |

---

## 11. Expected artifacts

Create:

```text
docs/research/ai-command-capabilities/domain-planner-v1-gate/
  README.md
  natural-command-contract-v0-draft.md
  decision-action-taxonomy-accepted-v0.md
  golden-scenarios-fixtures-v0/
    README.md
    context-fixtures-v0.yaml
    golden-scenarios-v0.yaml
  eval-rubric-v0.md
  privacy-and-retention-questions.md
  mobile-ai-state-matrix-v0.md
  provider-planner-readiness-checklist.md
  hometusk-ai-platform-integration-doc-drift.md
  provider-initiative-brief.md
```

Optionally create after Gate A:

```text
docs/planning/initiatives/INIT-2026Q3-ai-command-artifact-gate.execution.md
```

Do not create implementation epics/stories/workpacks until this initiative is approved and Codex produces a plan consistent with the existing pipeline.

---

## 12. Exit Criteria

This initiative is complete when:

1. Gate package exists under `docs/research/ai-command-capabilities/domain-planner-v1-gate/`.
2. Decision/action taxonomy v0 is accepted for artifact-gate purposes.
3. Natural command contract draft exists and is clearly marked non-runtime.
4. Golden scenarios are available as machine-readable fixtures.
5. Eval rubric v0 is documented.
6. Privacy/retention questions are documented.
7. Mobile AI state matrix is documented.
8. Provider planner readiness checklist is documented.
9. Integration doc drift and cleanup requirements are documented.
10. Provider initiative brief is documented.
11. No production code changed.
12. No OpenAPI/runtime contract changed.
13. No AI Platform code or schema changed.
14. Recommendation remains LIMITED-GO.
15. Next provider-side Domain Planner v1 scope is explicit but not implemented.

---

## 13. Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | Draft artifacts only; future implementation will have contract impact |
| adr_needed | maybe | Future `natural_command` / planner boundary likely deserves ADR |
| diagrams_needed | maybe | Optional architecture diagram expansion allowed in gate package |
| security_sensitive | yes | AI-command execution, household context, provider retention, privacy |
| traceability_critical | yes | Planner versioning, DecisionLog, replay, eval evidence |
| backend_impact | no | No runtime backend changes |
| mobile_impact | no | No mobile implementation; state matrix only |
| ai_platform_impact | no | Read-only provider context; future provider initiative likely |
| cross_repo | yes | HomeTusk write, AI Platform read-only |
| data_impact | no | No runtime data flow changes; privacy questions only |

---

## 14. Anti-Scope-Creep

DO NOT:

- modify Java or TypeScript runtime code;
- modify OpenAPI;
- modify AI Platform files;
- add `natural_command` implementation;
- add `needs_confirmation` implementation;
- add `answered` implementation;
- implement Domain Planner v1;
- build mobile AI cards;
- tune prompts;
- add multi-agent orchestration;
- broaden recommendation from LIMITED-GO to GO;
- treat draft schemas as accepted runtime contracts;
- start provider work in this repository.

---

## 15. Next Step After Gate A

Codex should receive a planning prompt:

- read this initiative;
- read the AI Command Capability Audit research pack;
- read external research comparison;
- inspect current HomeTusk integration docs;
- inspect AI Platform as read-only context if available;
- propose exact gate package files and contents;
- produce PLAN only;
- do not modify production code;
- do not modify OpenAPI/contracts;
- do not modify AI Platform;
- do not implement Domain Planner v1;
- wait for Human Gate C before APPLY.
