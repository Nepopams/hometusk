# External Research Comparison - 2026-06-15

Status: follow-up comparison after initial audit baseline.

External input:

- `C:\Users\user\Downloads\deep-research-report (32).md`

Compared against:

- `docs/research/ai-command-capabilities/README.md`
- `docs/research/ai-command-capabilities/current-state-code-audit.md`
- `docs/research/ai-command-capabilities/decision-taxonomy-v0.md`
- `docs/research/ai-command-capabilities/action-taxonomy-v0.md`
- `docs/research/ai-command-capabilities/golden-scenarios-v0.md`
- `docs/research/ai-command-capabilities/capability-matrix.md`
- `docs/research/ai-command-capabilities/platform-gap-analysis.md`
- `docs/research/ai-command-capabilities/hometusk-contract-gap-analysis.md`
- `docs/research/ai-command-capabilities/target-architecture-v0.md`
- `docs/research/ai-command-capabilities/recommendation.md`

## Executive Verdict

The external research and the HomeTusk audit are strongly aligned.

They agree on the core product and architecture direction:

- HomeTusk should build a narrow command layer, not a generic AI chat.
- The canonical outcomes should be `execute`, `clarify`, `confirm`, `reject`,
  and `answer`.
- AI Platform should plan and propose; HomeTusk should validate, enforce
  guardrails, execute, and audit.
- A single domain planner is the right next AI Platform shape.
- Multi-agent architecture is premature for current HomeTusk household commands.
- The first trust corridor should stay narrow around simple task creation and
  shopping item capture.
- Confidence must not be treated as execution permission.
- Voice is an input/draft channel, not an execution shortcut.
- Golden scenarios, traces, and outcome-based evals are mandatory before broad
  Mobile AI Command UX.

The main difference is posture:

- the external research is broader, industry-backed, and more product/roadmap
  oriented;
- the HomeTusk audit is narrower, repository-verified, and more conservative
  about implementation readiness.

Assessment: keep the HomeTusk audit recommendation as **LIMITED-GO**, but use
the external research to strengthen the next artifact gate: `natural_command`
contract draft, larger golden scenario suite, eval rubric, privacy/retention
questions, and mobile AI state model.

## Evidence Weighting

| Evidence type | Weight for current-state claims | Weight for target direction | Notes |
| --- | --- | --- | --- |
| HomeTusk repo audit | Highest | High | Directly inspected contracts, backend, mobile, integration docs, and provider code. |
| External research report | Medium | High | Strong for industry patterns and roadmap framing; it explicitly says it did not verify the codebase. |
| Existing roadmap / initiative docs | High | High | Defines active HomeTusk gates and sequencing. |
| Provider repo evidence | High | Medium | Useful for current AI Platform behavior; does not define HomeTusk product acceptance alone. |

Rule: when the external report and repo evidence disagree about current
capability, repo evidence wins. When they differ about product sequencing, use
risk and gate posture to decide.

## Where We Match

| Topic | External research | HomeTusk audit | Assessment |
| --- | --- | --- | --- |
| Product shape | Build a command layer, not another AI chat. | Same: no generic assistant chat, command-to-action flow only. | Full alignment. |
| Decision taxonomy | `execute`, `clarify`, `confirm`, `reject`, `answer`. | Same five canonical outcomes. | Full alignment. |
| Execution authority | AI proposes; HomeTusk validates, executes, audits. | Same invariant across target architecture and recommendation. | Full alignment and matches AGENTS.md. |
| Planner strategy | Single domain planner LLM in AI Platform, surrounded by deterministic rails. | Recommended next initiative: AI Platform Domain Planner v1, narrow corridor. | Full alignment. |
| Multi-agent | Premature; useful only later for decomposable/tool-heavy work. | Do not use multi-agent production planner now. | Full alignment. |
| First corridor | Simple task creation and shopping capture first. | Auto-execute only `create_task` and `add_shopping_items` in narrow corridor. | Strong alignment. |
| Guardrails | Schema is not enough; semantic correctness needs domain gates. | Same: schema validation plus HomeTusk guardrails and domain services. | Full alignment. |
| Confidence | Routing/audit signal only, never permission token. | Same execution permission rule. | Full alignment. |
| Voice | Voice draft with user review; no autopilot. | Same: ASR capture/edit/send, no direct ASR-to-decision shortcut. | Full alignment. |
| Mobile UX | Structured result cards, clarify chips/forms, confirmation cards, timeline. | Same future mobile responsibilities, but blocked until contracts exist. | Direction aligned; timing differs. |
| Evals | Golden scenarios are product asset; outcome-based evals, not prompt vibes. | Same; HomeTusk-owned scenario catalog starts in `golden-scenarios-v0.md`. | Full alignment, external raises the bar. |
| Current capability | Current decisioning is narrow/rule-oriented; partial trust mostly shopping. | Verified in AI Platform code and gap analysis. | External assumptions are confirmed by repo evidence. |

## Where We Diverge

### 1. Recommendation Label: `GO` vs `LIMITED-GO`

External research says GO for `AI Platform Domain Planner v1` and
`HomeTusk natural_command contract` if the corridor is additive/reversible and
rollout uses golden scenarios, shadow mode, and audit.

HomeTusk audit says **LIMITED-GO**: proceed only to a narrow Domain Planner v1
for simple task creation and shopping item addition; do not start broad
`natural_command`, Mobile AI Command Center, or mixed autonomous planning yet.

Evaluation:

- This is not a hard contradiction.
- The external `GO` is conditional and roadmap-level.
- The HomeTusk `LIMITED-GO` is implementation-gate-level and grounded in current
  code/contract gaps.

Decision: keep **LIMITED-GO** for current initiative closure. Rephrase future
planning as: GO to artifact/contract discovery and provider planner work, not
GO to product rollout.

### 2. Size of Golden Scenario Suite

External research recommends 50-100 scenarios for contract v0 and 200+ before
rollout of planner-driven auto-exec flows.

HomeTusk audit created 10 seed golden scenarios and says the first executable
suite should start with 10-20, then grow to 50-100 after taxonomy stabilizes.

Evaluation:

- External research sets the correct maturity target.
- HomeTusk audit created the correct initial seed for review.

Decision: treat the current 10 scenarios as seed only. Before Domain Planner v1
acceptance, expand to at least 50 scenarios with failure buckets:

- ASR noise;
- colloquial Russian;
- ambiguous member/list/task;
- unique vs ambiguous reschedule;
- status query;
- unsafe batch;
- shopping item split;
- task plus shopping linkage.

### 3. `natural_command` Contract Timing

External research proposes concrete `NaturalCommandRequest` and
`NaturalCommandDecision` schema sketches now.

HomeTusk audit identifies `natural_command` as a gap and recommends a later
contract/artifact gate before implementation.

Evaluation:

- External schema sketch is useful as input.
- It is not accepted HomeTusk contract yet.
- Current HomeTusk public command contract still supports only structured
  `create_task` and `complete_task`.

Decision: create a contract draft artifact next, but do not implement it without
contract governance and Gate C.

### 4. Read-Only `answer_status`

External research recommends status/query cards early because they are
low-mutation and reduce navigation cost.

HomeTusk audit marks `answer` as blocked until a read-only answer contract
exists.

Evaluation:

- Direction is aligned: answer must be read-only and grounded.
- External research is more aggressive on priority.
- HomeTusk audit is stricter on contract readiness.

Decision: promote `answer_status` contract design earlier, but keep runtime
implementation blocked until `answered` response shape and source read-model
rules are accepted.

### 5. Assignment Auto-Execution

External research says a direct named assignment such as "Пете завтра вынести
мусор" can execute if member match is unique and actor has rights.

HomeTusk audit recommends confirmation before non-requester assignment until
policy accepts direct naming.

Evaluation:

- This is a real policy divergence.
- In HomeTusk, household assignment has social and workload implications.
- Current actor permissions, fairness/workload policy, and consent semantics are
  not fully formalized for natural assignment.

Decision: keep HomeTusk's stricter confirm posture for inferred or
non-requester assignment. Reconsider auto-exec only after permissions and
workload/fairness rules are explicit.

### 6. Reschedule and Completion Corridor

External research allows exact-match completion and unique reschedule as
possible partial-trust actions.

HomeTusk audit keeps `complete_task` and `reschedule_task` outside v0
auto-exec for natural text because provider actions and exact matching are not
ready.

Evaluation:

- External recommendation is a reasonable later target.
- Current HomeTusk/provider evidence does not support it yet.

Decision: defer natural reschedule/completion auto-exec until task matching,
undo/recovery, and confirmation policy are implemented and covered by
scenarios.

### 7. Metrics and Eval Design

External research provides a richer operating model:

- JSON schema validity;
- execution precision;
- unauthorized execution rate;
- clarification resolution rate;
- confirmation acceptance/cancel rate;
- wrong-object mutation rate;
- user correction and undo/revert rate;
- p50/p95 latency;
- drift by prompt/model version;
- deterministic graders for critical outcomes;
- LLM-as-judge only for soft dimensions.

HomeTusk audit covers golden scenarios and traceability but does not yet define
a full metric rubric.

Evaluation:

- External research fills a real gap.

Decision: add an eval rubric/failure taxonomy before Domain Planner v1 APPLY.

### 8. Privacy, Retention, and Provider Governance

External research is more explicit about data minimization, prompt retention,
region, model provenance, ZDR-like constraints, raw audio, device tokens, and
large household histories.

HomeTusk audit includes a data boundary but does not fully define retention and
provider governance.

Evaluation:

- External research adds important security/privacy questions.

Decision: add privacy/retention questions to the artifact gate for
`natural_command` and Domain Planner v1.

### 9. Source Quality

External research includes embedded citation markers from a prior research
session, but the local markdown file does not contain resolvable source URLs.
It also states that the HomeTusk and AI Platform codebases were not
independently verified.

HomeTusk audit cites local code, contracts, docs, and provider files inspected
directly.

Evaluation:

- Use the external report as secondary research and product strategy input.
- Do not use it to override repo-grounded current-state findings.

Decision: keep HomeTusk audit as the canonical current-state baseline.

## Updated Assessment

The external research increases confidence in our existing direction. It does
not invalidate the HomeTusk initiative result.

Updated posture:

```text
Current initiative result: ACCEPT
Recommendation label: LIMITED-GO remains
Next work: artifact/contract/eval gate before implementation
```

The best combined interpretation:

1. **Now:** close the audit baseline and adopt external research as secondary
   evidence.
2. **Next:** create a Domain Planner v1 / `natural_command` artifact gate.
3. **Before APPLY:** define contract v0, eval rubric, privacy rules, and at
   least 50 product-owned golden scenarios.
4. **Only then:** implement narrow planner and HomeTusk adapter changes.
5. **Later:** build Mobile AI Command UX with structured cards, clarify chips,
   confirmation cards, degraded states, and timeline.

## Recommended Changes to Our Backlog

### Must Adopt

- Keep single domain planner as next AI Platform direction.
- Keep HomeTusk as final execution authority.
- Expand golden scenarios beyond the current seed.
- Add machine-readable scenario fixtures.
- Add an eval rubric with deterministic graders.
- Add privacy/retention/provider-governance questions.
- Draft `natural_command` request/decision schemas as artifact-gate material.
- Add first-class `confirm` and `answer` contract design before Mobile AI UX.

### Should Adopt

- Add status/query cards to early contract discussion as read-only `answer`.
- Define a mobile AI state matrix:
  - result card;
  - clarify card;
  - confirmation card;
  - rejected card;
  - degraded card;
  - timeline entry.
- Add model/planner version and prompt version into trace requirements.
- Add failure buckets for ASR noise and Russian morphology.

### Keep Conservative

- Do not auto-execute non-requester assignment yet.
- Do not auto-execute natural reschedule yet.
- Do not treat exact-match completion as planner-ready until matching and undo
  policies exist.
- Do not start multi-agent production architecture.
- Do not start Mobile AI Command Center before contract and eval gates.
- Do not let mobile call AI Platform directly.

## Suggested Next Artifact

Create:

```text
docs/planning/initiatives/INIT-2026Q3-ai-domain-planner-v1.md
```

or an artifact-gate package under:

```text
docs/research/ai-command-capabilities/domain-planner-v1-gate/
```

Minimum contents:

- `natural-command-contract-v0-draft.md`
- `decision-action-taxonomy-accepted-v0.md`
- `golden-scenarios-fixtures-v0/`
- `eval-rubric-v0.md`
- `privacy-and-retention-questions.md`
- `mobile-ai-state-matrix-v0.md`
- `provider-planner-readiness-checklist.md`

## Impact Flags

| Flag | Value | Notes |
| --- | --- | --- |
| contract_impact | no for this comparison; yes for recommended follow-up | This report changes docs only. |
| data_impact | no for this comparison; yes for future privacy gate | External research raises retention/minimization questions. |
| adr_needed | maybe | Future Domain Planner / `natural_command` boundary likely deserves ADR. |
| diagrams_needed | maybe | Existing target architecture can be expanded when contract is drafted. |
| security_sensitive | yes | AI command execution, household context, retention, and provider governance. |
| traceability_critical | yes | Planner versioning, decision log, replay, and eval evidence are central. |
| backend_impact | no now | Future `natural_command`, `confirm`, and `answer` would affect backend contracts. |
| mobile_impact | no now | Future Mobile AI UX requires state/card contract. |
| ai_platform_impact | no now | Future Domain Planner v1 likely provider-owned. |

## Bottom Line

The two bodies of work mostly say the same thing from different angles.

The external report gives broader product and industry justification. The
HomeTusk audit gives verified implementation reality. Together they support a
stronger but still gated path:

```text
LIMITED-GO now -> contract/eval/privacy gate -> narrow Domain Planner v1 ->
HomeTusk natural_command -> Mobile AI UX -> later advanced planning
```

