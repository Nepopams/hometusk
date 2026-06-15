# Initiative: INIT-2026Q3-ai-command-capability-audit — AI Command Capability Audit & Golden Scenarios

## Status

Draft (to be approved at Human Gate A)

## Initiative type

Discovery / Research Spike / Cross-repo Audit / AI Platform Integration / Product Safety

## Owner

HomeTusk product engineering team.

## Target milestone

Before AI Platform Domain Planner v1 and before Mobile AI Command Center.

## Parent / Related initiatives

- Related mobile foundation: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.md`
- Related mobile refactor: `docs/planning/initiatives/INIT-2026Q3-mobile-client-refactor-foundation.md`
- Related voice work: `docs/planning/initiatives/INIT-2026Q3-voice-command-chat-mvp.md`
- Related AI Platform integration: `docs/integration/ai-platform/**`
- Future candidate: AI Platform Domain Planner v1
- Future candidate: HomeTusk `natural_command` contract
- Future candidate: Mobile AI Command UX v1

---

## Sources of Truth

- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- REST Contracts: `docs/contracts/**`
- AI Platform Integration Snapshot: `docs/integration/ai-platform/**`
- Mobile Client: `clients/mobile/`
- Mobile Command Feature: `clients/mobile/src/features/command/**`
- Backend Command Pipeline: `services/backend/src/main/java/com/hometusk/commands/**`
- Backend Context / Guardrails: `services/backend/src/main/java/com/hometusk/commands/pipeline/**`
- External research input: AI-command capabilities deep research report, 2026-06-15, to be summarized into `docs/research/ai-command-capabilities/**` during this initiative.
- Provider repository for read-only audit: `Nepopams/vr_ai_platform`

---

## 1. Problem / Opportunity

HomeTusk now has a native mobile client, voice/ASR foundations, a command-driven backend pipeline, AI Platform integration, schema validation, guardrails, task and shopping domains, and task ↔ shopping linkage.

The next product direction is mobile-first AI command control: the user should write or speak a natural household command, and HomeTusk should safely turn it into a domain outcome.

However, current AI-command maturity is uncertain and likely insufficient for a production-grade Mobile AI Command Center:

- ASR appears useful as a capture mechanism: voice → editable transcript draft → explicit user Send.
- Current decisioning appears narrow and partially rule-based.
- Current agent registry / assist / shadow / partial-trust paths appear experimental rather than mature household planning.
- Existing mobile Command tab is a deterministic shell over `/commands`, not a true natural command UX.
- The product still lacks a formally accepted decision taxonomy, action taxonomy, golden scenarios, and trust corridor for AI-command execution.

The risk is building a polished mobile AI surface over a weak planner. That would create the appearance of intelligence without reliable household operations.

This initiative establishes an evidence-based capability baseline before any Domain Planner v1, `natural_command` contract, or Mobile AI Command Center work begins.

---

## 2. Outcome (what changes for product/team)

No user-visible product feature is delivered by this initiative.

The outcome is decision-grade documentation:

- what AI-command capabilities HomeTusk actually needs;
- what the current HomeTusk ↔ AI Platform stack can and cannot do;
- what scenarios define acceptable AI-command behavior;
- which outcomes and actions are canonical for v0;
- what must stay deterministic in HomeTusk;
- what AI Platform should own;
- whether we can proceed to Domain Planner v1;
- whether Mobile AI Command Center is blocked, partially unblocked, or still premature.

The expected final decision is one of:

```text
GO          — proceed to AI Platform Domain Planner v1.
LIMITED-GO  — proceed only for a narrow corridor such as shopping/simple tasks.
NO-GO       — fix foundational AI Platform capabilities before product UX work.
```

---

## 3. Scope (Now / Next / Later)

### NOW — Capability audit and golden scenarios

#### Current-state code audit

Inspect HomeTusk and AI Platform as a cross-repo system.

HomeTusk audit:

- command contracts;
- command pipeline;
- AI Platform decision provider integration;
- context builder;
- guardrails;
- action executor;
- DecisionLog / traceability;
- command result shape;
- mobile command feature after refactor.

AI Platform audit, read-only:

- `/decide` route;
- command and decision schemas;
- Router V1/V2;
- current planner behavior;
- assist mode;
- partial trust;
- agent registry;
- agent runner;
- existing evals/tests;
- current ASR boundary where relevant.

#### Decision taxonomy v0

Define canonical AI-command decision outcomes:

```text
execute
clarify
confirm
reject
answer
```

Map them to current HomeTusk response statuses where possible and explicitly document gaps.

#### Action taxonomy v0

Define candidate action types for HomeTusk AI-command v0, for example:

```text
create_task
complete_task
add_shopping_items
link_task_shopping
reschedule_task
answer_status
```

Classify each action by:

- additive / mutating / destructive;
- reversible / hard to reverse;
- single-object / multi-object;
- auto-execute allowed / clarify / confirm / reject;
- required context;
- HomeTusk validation rules.

#### Golden scenarios v0

Create an initial golden scenario catalog for natural household commands.

Minimum canonical scenarios:

1. Simple task: `убрать кухню сегодня вечером`
2. Shopping: `купи молоко и курицу`
3. Shopping with list/source: `добавь в ашан мусорные пакеты и редьку`
4. Task + shopping linkage: `к ужину купи молоко и курицу`
5. Assignment: `Пете завтра вынести мусор`
6. Reschedule: `перенеси уборку ванной на выходные`
7. Batch planning: `распредели уборку на выходные`
8. Ambiguous command: `надо подготовиться к гостям`
9. Unsafe / impossible command: `назначь всем по 20 задач сегодня ночью`
10. Status/query style: `что у нас сегодня по дому?`

Each scenario must define:

- input text;
- locale;
- timezone;
- required context;
- expected intent;
- expected entities;
- expected decision outcome;
- expected proposed actions;
- expected clarify / confirmation behavior;
- forbidden assumptions;
- HomeTusk responsibility;
- AI Platform responsibility;
- failure modes;
- UX recommendation.

The initial set should be small enough for review but structured so it can grow to 50–100 cases.

#### Capability matrix

Create a capability matrix with at least:

- ASR transcript;
- language normalization;
- intent classification;
- entity extraction;
- multi-item shopping extraction;
- date/time normalization;
- task creation planning;
- task completion matching;
- assignment recommendation;
- workload/fairness reasoning;
- zone grounding;
- shopping list grounding;
- task ↔ shopping linkage planning;
- multi-action planning;
- clarification question generation;
- confirmation decisioning;
- structured result explanation;
- confidence scoring;
- fallback/degradation;
- audit/traceability;
- golden scenario regression testing.

Columns:

- capability;
- current observed maturity;
- required maturity for good UX;
- product value;
- implementation risk;
- safety risk;
- recommended priority;
- owner: HomeTusk / AI Platform / both;
- evidence.

#### Gap analysis

Produce two gap documents:

1. AI Platform gap analysis: what provider capabilities are missing before Domain Planner v1.
2. HomeTusk contract gap analysis: what HomeTusk needs before `natural_command` and Mobile AI Command UX.

#### Recommendation

Produce a final recommendation:

- GO / LIMITED-GO / NO-GO;
- recommended next initiative;
- non-goals;
- trust corridor;
- risk notes;
- minimum contract changes required.

---

### NEXT — AI Platform Domain Planner v1

Not part of this initiative.

Likely follow-up if audit result is GO or LIMITED-GO:

- implement a single domain planner in AI Platform;
- support structured decision output;
- support narrow execution corridor;
- validate against golden scenarios;
- avoid multi-agent architecture as the first production brain.

---

### LATER — HomeTusk natural_command and Mobile AI UX

Not part of this initiative.

Likely future work:

- add `natural_command` contract to HomeTusk;
- map AI Platform planner decisions into HomeTusk command outcomes;
- add `confirm` and `answer` semantics where needed;
- build mobile structured result cards;
- add clarify chips/forms;
- add confirmation cards;
- add command timeline;
- add mobile voice draft into the same command flow.

---

## 4. In Scope (explicit)

- Documentation-first discovery and audit.
- Read-only inspection of `vr_ai_platform`.
- HomeTusk-owned product acceptance criteria.
- HomeTusk-owned golden scenarios.
- Decision taxonomy v0.
- Action taxonomy v0.
- Capability matrix.
- Current-state audit of HomeTusk AI-command integration.
- Current-state audit of AI Platform decisioning and agent capability.
- Gap analysis.
- Recommendation for next initiatives.
- Optional lightweight scripts for generating or validating scenario documentation, if justified.

---

## 5. Out of Scope (explicit)

- No production feature implementation.
- No backend API changes.
- No mobile UX changes.
- No AI Platform code changes.
- No AI Platform contract changes.
- No HomeTusk contract changes.
- No new agent implementation.
- No prompt tuning as a hidden implementation.
- No generic assistant chat design.
- No direct mobile → AI Platform integration.
- No multi-agent architecture build-out.
- No rollout or production configuration changes.

---

## 6. Cross-repo ownership and commit policy

This initiative is owned and committed in the HomeTusk repository.

Codex may inspect `vr_ai_platform` as read-only context if the repository is available locally or through connector tools.

Codex must not:

- modify files in `vr_ai_platform`;
- create commits in `vr_ai_platform`;
- reformat files in `vr_ai_platform`;
- vendor large AI Platform snapshots into HomeTusk unless explicitly approved;
- treat AI Platform implementation details as HomeTusk source of truth.

If AI Platform work is required, this initiative must create follow-up recommendations for a separate AI Platform initiative/PR.

---

## 7. Assumptions

- HomeTusk remains the product-service and final authority for domain execution.
- AI Platform remains an external decision/planner provider.
- Mobile must not call AI Platform directly.
- AI Platform output must be schema-validated before HomeTusk uses it.
- HomeTusk guardrails remain final authority for membership, authz, entity grounding, deadlines, workload, confirmation gates, idempotency, execution and audit.
- ASR is an input/capture capability, not an execution shortcut.
- Current AI Platform agents may be too weak for product-grade AI-command UX; this must be verified rather than assumed.

---

## 8. Success Metrics

This initiative succeeds when the team can answer:

1. What decision outcomes are canonical for HomeTusk AI-command v0?
2. What action types are allowed in the first trust corridor?
3. What scenarios define acceptable behavior?
4. What must be clarified, confirmed, rejected, answered, or executed?
5. What can current AI Platform actually do?
6. What gaps block Domain Planner v1?
7. What gaps block HomeTusk `natural_command`?
8. What gaps block Mobile AI Command UX?
9. Which repository owns each next piece of work?
10. What is the recommended next initiative?

---

## 9. Constraints / Guardrails

- Evidence over opinion: every maturity claim must cite code, contract, test, doc, or explicit assumption.
- Separate external research findings from repository-grounded findings.
- Do not use confidence score as execution permission in recommendations.
- Prefer narrow trust corridors over broad autonomy.
- Prefer single domain planner over multi-agent architecture until decomposability and tool overload justify more complexity.
- Prefer clarify/confirm over guessing.
- Keep HomeTusk as execution authority.
- Keep AI Platform as planner/decision provider.
- Keep mobile as command UX client, not AI execution client.

---

## 10. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Audit becomes vague opinion doc | HIGH | Require scenario tables, capability matrix, and code/contract evidence |
| Codex starts implementing planner changes | HIGH | Explicitly mark production code changes out of scope |
| Cross-repo work causes commit confusion | HIGH | HomeTusk write-only, AI Platform read-only in this initiative |
| Golden scenarios become too abstract | MEDIUM | Use real household commands and expected domain outcomes |
| Team jumps to mobile UI before planner quality | HIGH | Gate Mobile AI UX on Domain Planner v1 and natural_command contract |
| Multi-agent hype redirects architecture | MEDIUM | Require explicit evidence before recommending multi-agent |
| Privacy/security context is underspecified | HIGH | Include context minimization and forbidden data fields in target architecture |

---

## 11. Expected artifacts

Codex should create, at minimum:

```text
docs/research/ai-command-capabilities/
  README.md
  current-state-code-audit.md
  decision-taxonomy-v0.md
  action-taxonomy-v0.md
  golden-scenarios-v0.md
  capability-matrix.md
  platform-gap-analysis.md
  hometusk-contract-gap-analysis.md
  target-architecture-v0.md
  recommendation.md
```

Codex may also create:

```text
docs/planning/initiatives/INIT-2026Q3-ai-command-capability-audit.execution.md
```

only after Gate A approval, following the existing initiative execution pipeline.

---

## 12. Exit Criteria

The initiative is complete when:

1. Current HomeTusk AI-command integration is audited.
2. Current AI Platform decision/planner/agent capabilities are audited read-only.
3. Decision taxonomy v0 is proposed.
4. Action taxonomy v0 is proposed.
5. Golden scenarios v0 are documented.
6. Capability matrix is documented.
7. AI Platform gap analysis is documented.
8. HomeTusk contract gap analysis is documented.
9. Target architecture v0 is documented.
10. GO / LIMITED-GO / NO-GO recommendation is documented.
11. Next initiative recommendation is explicit.
12. No production code, contract, backend, mobile, or AI Platform files are changed.

---

## 13. Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | Audit only; may recommend future contract changes |
| adr_needed | no | May recommend ADR for future Domain Planner / natural_command |
| diagrams_needed | maybe | Optional architecture sketch in research docs |
| security_sensitive | yes | AI-command execution, authz, household context and privacy boundaries |
| traceability_critical | yes | Future decisions must be auditable and replayable |
| backend_impact | no | No implementation in this initiative |
| mobile_impact | no | No UI change in this initiative |
| ai_platform_impact | no | Read-only audit; future provider initiative likely |
| cross_repo | yes | HomeTusk write, AI Platform read-only |

---

## 14. Anti-Scope-Creep

DO NOT:

- implement `natural_command`;
- change `/commands`;
- change AI Platform schemas;
- change AI Platform router/planner/agents;
- add mobile AI UX;
- add voice on mobile;
- add generic assistant chat;
- add multi-agent orchestration;
- tune prompts as implementation;
- bypass HomeTusk guardrails;
- call AI Platform directly from mobile;
- commit to `vr_ai_platform`;
- make large code changes under the label of audit.

---

## 15. Next Step After Gate A

Codex should receive a planning prompt:

- read this initiative;
- read the external deep research report if available;
- inspect HomeTusk repository;
- inspect `vr_ai_platform` as read-only context if available;
- propose execution artifacts and research-doc structure;
- identify exact files to create;
- produce PLAN only;
- do not modify production code;
- do not modify AI Platform;
- do not create downstream implementation work until Human Gate C.
