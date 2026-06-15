# Initiative: INIT-2026Q3-ai-command-artifact-gate — AI Command Artifact Gate for Domain Planner v1

## Status

Completed - artifact/contract/eval gate accepted on 2026-06-15.

Gate posture:

- Human Gate A: GO, selected as planning focus for this gate.
- Human Gate B: GO, committed docs-only artifact package scope.
- Human Gate C: GO, delegated approval for docs-only APPLY.
- Human Gate D: GO, artifact package accepted.
- No runtime or provider APPLY approval is implied.
- Docs-only artifact gate; no runtime code or contract changes in this
  initiative.
- Final inherited recommendation remains **LIMITED-GO**.
- Gate decisions and evidence are recorded in
  `docs/planning/initiatives/INIT-2026Q3-ai-command-artifact-gate.execution.md`.

## Initiative type

Product Architecture / Contract Discovery / Eval Design / Privacy Gate / Cross-repo Planning

## Owner

HomeTusk product engineering team.

## Target milestone

Before AI Platform Domain Planner v1 APPLY.

## Parent / Related initiatives

- Parent discovery baseline: `docs/planning/initiatives/INIT-2026Q3-ai-command-capability-audit.md`
- Research pack: `docs/research/ai-command-capabilities/**`
- Artifact gate package: `docs/research/ai-command-capabilities/domain-planner-v1-gate/**`
- External research comparison: `docs/research/ai-command-capabilities/external-research-comparison-2026-06-15.md`
- Future provider initiative: AI Platform Domain Planner v1 - Narrow Household Command Corridor
- Future HomeTusk initiative: `natural_command` contract and backend integration
- Future mobile initiative: Mobile AI Command UX v1

---

## Sources of Truth

- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- AI Command Capability Audit: `docs/research/ai-command-capabilities/README.md`
- Current-State Code Audit: `docs/research/ai-command-capabilities/current-state-code-audit.md`
- Decision Taxonomy v0: `docs/research/ai-command-capabilities/decision-taxonomy-v0.md`
- Action Taxonomy v0: `docs/research/ai-command-capabilities/action-taxonomy-v0.md`
- Golden Scenarios v0: `docs/research/ai-command-capabilities/golden-scenarios-v0.md`
- Capability Matrix: `docs/research/ai-command-capabilities/capability-matrix.md`
- AI Platform Gap Analysis: `docs/research/ai-command-capabilities/platform-gap-analysis.md`
- HomeTusk Contract Gap Analysis: `docs/research/ai-command-capabilities/hometusk-contract-gap-analysis.md`
- Target Architecture v0: `docs/research/ai-command-capabilities/target-architecture-v0.md`
- Recommendation: `docs/research/ai-command-capabilities/recommendation.md`
- External Research Comparison: `docs/research/ai-command-capabilities/external-research-comparison-2026-06-15.md`
- Domain Planner v1 Artifact Gate Package: `docs/research/ai-command-capabilities/domain-planner-v1-gate/README.md`
- HomeTusk Commands Contract: `docs/contracts/http/commands.openapi.yaml`
- HomeTusk AI Platform Integration: `docs/integration/ai-platform/**`
- Provider repository for read-only reference: `C:\Users\user\Documents\projects\VR_AI_Platform`

---

## 1. Problem / Opportunity

The AI Command Capability Audit established a useful direction but stopped short of implementation approval.

Current recommendation is **LIMITED-GO**:

- proceed only toward a narrow AI Platform Domain Planner v1 corridor;
- focus on simple task creation and shopping item addition;
- do not proceed yet to broad natural command execution, HomeTusk `natural_command`, Mobile AI Command Center, mixed task/shopping autonomous planning, or multi-agent production planner.

The audit and external comparison agree on product direction:

```text
HomeTusk needs a command layer, not a generic AI chat.
AI Platform should plan/propose.
HomeTusk should validate, guardrail, execute and audit.
Single domain planner is the next reasonable provider shape.
Multi-agent production architecture is premature.
```

However, implementation is still blocked by artifact gaps:

decision/action taxonomy is proposed but not accepted as an implementation baseline;
golden scenarios are markdown seed only, not machine-readable fixtures;
no eval rubric exists for planner acceptance;
privacy, retention and provider governance questions are not yet formalized;
natural_command exists only as a concept, not a draft contract package;
mobile AI state model is not accepted;
HomeTusk AI Platform integration docs have known drift against runtime behavior;
AI Platform provider initiative needs a precise, bounded brief.

This initiative closes those artifact gaps before any provider-side or HomeTusk runtime APPLY work.

## 2. Outcome

This initiative produces an implementation-ready artifact gate package.

After completion, the team should be able to start a separate AI Platform Domain Planner v1 initiative with clear inputs:

accepted decision/action taxonomy;
draft natural_command contract direction;
machine-readable golden scenario fixtures;
eval rubric and deterministic grading expectations;
privacy and retention questions;
mobile AI state matrix;
provider readiness checklist;
HomeTusk integration-doc drift summary;
provider initiative brief.

This initiative does not implement the planner, natural_command, backend response types, mobile UI, or AI Platform code.

## 3. Scope (Now / Next / Later)

### NOW — Artifact Gate Package

Create:

```text
docs/research/ai-command-capabilities/domain-planner-v1-gate/
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

README

Must summarize:

artifact gate purpose;
LIMITED-GO posture;
what is accepted;
what remains draft;
what is blocked;
what the next provider initiative may consume;
what must not be implemented yet.
decision-action-taxonomy-accepted-v0.md

Turn proposed taxonomy into an accepted baseline for the narrow corridor.

Must include:

canonical decision outcomes:
execute;
clarify;
confirm;
reject;
answer;
current support status;
mapping to current HomeTusk statuses;
mapping to current AI Platform statuses/actions;
first trust corridor;
execution permission rule;
explicit statement that confidence is not permission.

Expected accepted narrow corridor:

auto-execute:
  - create_task
  - add_shopping_items

clarify:
  - ambiguous task/list/zone/member/date
  - missing default shopping list
  - incomplete household context

confirm:
  - inferred non-requester assignment
  - task + shopping linkage
  - reschedule
  - batch planning
  - broad workload redistribution

reject:
  - unsafe/impossible requests
  - unsupported actions
  - cross-household references
  - unverifiable entities
  - direct AI execution bypassing HomeTusk

answer:
  - design-only until read-only answer contract exists
natural-command-contract-v0-draft.md

Draft only. No OpenAPI change.

Must propose:

future HomeTusk-facing request shape;
future provider-facing decision shape;
context snapshot rules;
allowed capabilities;
source/input-mode fields;
locale/timezone/reference-instant fields;
audit/tracing fields;
compatibility notes with existing create_task and complete_task.

It must explicitly state:

not accepted as runtime contract yet;
not implemented in this initiative;
future implementation requires contract impact gate;
mobile/web must still call HomeTusk, not AI Platform.
golden-scenarios-fixtures-v0/

Convert seed scenarios into machine-readable fixtures.

Minimum:

golden-scenarios-v0.yaml
context-fixtures-v0.yaml
README.md

The first fixture set must include the 10 existing canonical scenarios:

убрать кухню сегодня вечером
купи молоко и курицу
добавь в ашан мусорные пакеты и редьку
к ужину купи молоко и курицу
Пете завтра вынести мусор
перенеси уборку ванной на выходные
распредели уборку на выходные
надо подготовиться к гостям
назначь всем по 20 задач сегодня ночью
что у нас сегодня по дому?

Each fixture should include:

id;
input text;
locale;
timezone;
reference instant;
context fixture id;
expected intent;
expected entities;
expected decision outcome;
expected actions;
expected clarify;
expected confirm;
forbidden assumptions;
HomeTusk responsibility;
AI Platform responsibility;
failure modes;
UX recommendation.

The fixture package must explicitly state that 10 scenarios are only the seed set. Before Domain Planner v1 acceptance, the suite should grow to at least 50 scenarios.

eval-rubric-v0.md

Define deterministic acceptance criteria for provider planner evaluation.

Must cover:

schema validity;
decision outcome correctness;
intent correctness;
entity extraction correctness;
item boundary preservation;
date/time ambiguity handling;
no unsupported auto-execute;
no forbidden assumptions;
no cross-household leakage;
clarify over guessing;
reject unsafe broad assignments;
no mutation for answer-style commands;
trace completeness;
failure bucket taxonomy;
which checks can be deterministic;
which checks may be judged manually or later by LLM-as-judge for soft dimensions only.
privacy-and-retention-questions.md

Define open questions and minimum privacy boundary for future planner work.

Must include:

what HomeTusk may send to AI Platform;
what must not be sent;
raw audio prohibition;
device/auth/invite token prohibition;
email and private comment handling;
cross-household prohibition;
prompt/response retention questions;
region/provider/model provenance questions;
prompt version and planner version retention;
audit and DecisionLog expectations.
mobile-ai-state-matrix-v0.md

Define future mobile state model without implementation.

Must include states:

composer;
processing;
executed card;
clarify card;
confirmation card;
rejected card;
answered card;
degraded card;
timeline entry.

For each state, define:

backend response dependency;
user-visible behavior;
required fields;
blocked fields;
local persistence;
failure/retry behavior.

Must state that mobile AI UX remains blocked until backend contract supports required states.

provider-planner-readiness-checklist.md

Define what AI Platform Domain Planner v1 must support.

Minimum:

consume HomeTusk golden fixtures;
support accepted taxonomy mapping;
support execute, clarify, reject;
optionally support confirm as non-executing output;
support create_task;
support multi-item add_shopping_items;
propose date/time with timezone or clarify;
preserve schema validation;
avoid direct mutation;
avoid broad multi-agent planner;
provide planner version / decision version / trace id;
expose deterministic eval output for HomeTusk acceptance.
hometusk-ai-platform-integration-doc-drift.md

Summarize known drift:

legacy /decision vs current /v1/decide;
wrapper camelCase schema vs runtime upstream snake_case envelope;
wrapper schema not reflecting current provider actions;
reject documentation vs upstream action/status reality;
internal add_shopping_item execution vs public command type mismatch.

Must classify each drift item as:

blocking before provider planner;
blocking before HomeTusk integration;
non-blocking documentation cleanup.
provider-initiative-brief.md

Prepare a concise brief for the future AI Platform initiative.

Must include:

recommended initiative name;
repository owner: vr_ai_platform;
HomeTusk artifacts to consume;
provider scope;
provider non-goals;
expected contract outputs;
expected eval outputs;
HomeTusk acceptance gates;
cross-repo commit policy.
### NEXT — AI Platform Domain Planner v1

Not part of this initiative.

Likely provider-side follow-up if artifact gate is accepted:

AI Platform Domain Planner v1 - Narrow Household Command Corridor

Expected provider scope:

implement or adapt planner to accepted taxonomy;
support simple task and shopping corridor;
run HomeTusk golden fixtures;
keep schema validation;
avoid multi-agent production brain.
### LATER — HomeTusk natural_command and Mobile AI UX

Not part of this initiative.

Future HomeTusk work may include:

natural_command request contract;
needs_confirmation response;
answered response;
provider adapter mapping;
DecisionLog expansion;
mobile structured cards;
clarify chips/forms;
confirmation cards;
command timeline.
## 4. In Scope
Docs-only artifact gate.
Accepted taxonomy doc.
Draft natural_command contract doc.
Machine-readable seed golden fixtures.
Eval rubric.
Privacy/retention questions.
Mobile AI state matrix.
Provider readiness checklist.
Integration doc drift summary.
Provider initiative brief.
Roadmap/planning updates if needed.
## 5. Out of Scope
No backend implementation.
No Java code changes.
No TypeScript/mobile implementation.
No OpenAPI changes.
No AI Platform code changes.
No AI Platform schema changes.
No runtime contract changes.
No prompt tuning.
No generic assistant chat.
No multi-agent production planner.
No direct mobile to AI Platform calls.
No Domain Planner v1 implementation initiative in this patch unless explicitly approved later.
## 6. Cross-repo Ownership

This initiative is HomeTusk-owned and committed in the HomeTusk repository.

AI Platform is provider-side follow-up work and must be handled in vr_ai_platform through a separate initiative/branch/PR.

Codex may inspect vr_ai_platform as read-only context if available.

Codex must not:

modify AI Platform files;
create commits in AI Platform;
reformat AI Platform files;
vendor large AI Platform snapshots into HomeTusk;
treat AI Platform implementation as HomeTusk source of truth.

HomeTusk owns:

product golden scenarios;
accepted decision/action taxonomy;
trust corridor;
safety/UX requirements;
consumer contract direction;
final acceptance gates.

AI Platform owns:

provider planner implementation;
provider schema/runtime implementation;
provider eval runner;
provider-side contract outputs.
## 7. Assumptions
AI Command Capability Audit is accepted as research baseline with LIMITED-GO.
External deep research is secondary evidence for product strategy and industry patterns.
Repo-grounded audit remains canonical for current-state implementation claims.
HomeTusk remains execution authority.
AI Platform remains planner/decision provider.
Mobile/web remain clients of HomeTusk, not AI Platform.
Confidence must not be used as permission to execute.
The first planner corridor is intentionally narrow.
## 8. Success Metrics

This initiative succeeds when:

Accepted decision/action taxonomy exists.
Draft natural command contract exists.
Golden scenarios have machine-readable seed fixtures.
Eval rubric exists.
Privacy/retention questions are explicit.
Mobile AI state matrix exists.
Provider readiness checklist exists.
Integration doc drift is documented and classified.
Provider initiative brief exists.
Recommendation remains LIMITED-GO.
No runtime code or contracts are changed.
The next provider-side initiative can start from clear inputs.
## 9. Constraints / Guardrails
Keep recommendation LIMITED-GO.
Do not broaden to full GO.
Do not implement.
Do not modify runtime contracts.
Do not modify AI Platform.
Do not start Mobile AI UX.
Do not hide prompt tuning or code changes under docs.
Prefer clarify/confirm over guessing.
Preserve HomeTusk final authority.
Preserve traceability and audit requirements.
Treat fixtures as product acceptance assets.
## 10. Risks & Mitigations
Risk	Impact	Mitigation
Artifact gate turns into implementation	HIGH	Docs-only scope and no runtime files
Taxonomy remains ambiguous	HIGH	Accepted taxonomy file with explicit mappings
Fixtures are too weak	MEDIUM	Seed now, require 50+ before provider acceptance
Provider initiative starts with stale docs	HIGH	Integration doc drift file
Privacy questions are deferred	HIGH	Privacy/retention artifact required
Mobile UX starts too early	HIGH	Mobile state matrix only, no UI work
Multi-agent path returns through provider work	MEDIUM	Provider checklist forbids broad multi-agent brain for v1
## 11. Expected Files

Minimum expected files:

docs/planning/initiatives/INIT-2026Q3-ai-command-artifact-gate.md

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

Optional after Gate A:

docs/planning/initiatives/INIT-2026Q3-ai-command-artifact-gate.execution.md
## 12. Exit Criteria

The initiative is complete when:

All expected artifact gate files exist.
The decision/action taxonomy is explicitly accepted for the narrow v0 corridor.
The natural command contract draft is explicit but not implemented.
Seed golden scenarios are machine-readable.
Eval rubric is explicit enough for provider work.
Privacy/retention questions are documented.
Mobile AI state matrix is documented.
Provider readiness checklist is actionable.
Integration doc drift is classified.
Provider initiative brief can be handed to AI Platform.
No production/runtime/API/mobile/AI Platform code changed.
Roadmap or planning docs are updated if required.
Final recommendation remains LIMITED-GO.
## 13. Flags
Flag	Value	Notes
contract_impact	no	Draft only; future implementation will have contract impact
adr_needed	maybe	Future Domain Planner / natural_command boundary may need ADR
diagrams_needed	maybe	Target architecture exists; can be expanded if useful
security_sensitive	yes	Household context, AI provider, privacy, retention
traceability_critical	yes	Planner version, decision log, replay and eval evidence
backend_impact	no	Docs-only
mobile_impact	no	State matrix only
ai_platform_impact	no	Provider follow-up only
cross_repo	yes	HomeTusk write, AI Platform read-only
data_impact	no now	Future planner work must resolve privacy/retention
## 14. Anti-Scope-Creep

DO NOT:

implement Domain Planner v1;
implement HomeTusk natural_command;
add needs_confirmation;
add answered;
modify OpenAPI;
modify Java/TypeScript code;
modify mobile command UI;
modify AI Platform code or schemas;
create a production multi-agent architecture;
tune prompts;
broaden LIMITED-GO to GO;
bypass HomeTusk guardrails;
permit direct mobile to AI Platform calls.
## 15. Next Step After Gate D

Start a separate provider-side planning track:

`AI Platform Domain Planner v1 - Narrow Household Command Corridor`

Use:

- `docs/research/ai-command-capabilities/domain-planner-v1-gate/provider-initiative-brief.md`
- `docs/research/ai-command-capabilities/domain-planner-v1-gate/provider-planner-readiness-checklist.md`
- `docs/research/ai-command-capabilities/domain-planner-v1-gate/golden-scenarios-fixtures-v0/`

Keep the next track separate from HomeTusk runtime APPLY. HomeTusk
`natural_command`, Mobile AI UX, and any OpenAPI/backend/mobile changes remain
blocked until their own contract gate, workpack, PLAN, Gate C, APPLY, and
review gate.
