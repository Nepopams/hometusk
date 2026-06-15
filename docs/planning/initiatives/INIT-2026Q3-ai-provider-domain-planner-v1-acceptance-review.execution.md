# INIT-2026Q3 AI Provider Domain Planner v1 Acceptance Review Execution Notes

**Status:** Completed; Gate A GO; Gate B GO; Gate C GO; Gate D GO; final decision LIMITED-GO
**Date:** 2026-06-15
**Initiative:** `docs/planning/initiatives/INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review.md`
**Roadmap:** `docs/planning/strategy/roadmap.md`
**Delegation:** Human gates for this initiative are delegated to Codex. Every GO / NO-GO / HOLD decision is recorded here with evidence, risks, and rationale.

---

## Intake Summary

| Field | Decision |
| --- | --- |
| Request type | Product acceptance gate / docs-only cross-repo evidence review |
| Scope anchor | `INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review` |
| Workflow step | `intake -> planning -> PLAN -> Gate C -> docs-only APPLY -> review gate -> Gate D` |
| Primary result required | HomeTusk acceptance decision for provider Domain Planner v1 evidence |
| Primary boundary | HomeTusk writes only to this repository; AI Platform repository is read-only context |
| Final decision vocabulary | GO / LIMITED-GO / NO-GO / HOLD |

## Sources of Truth Read

### HomeTusk

| Artifact | Path |
| --- | --- |
| Active repo rules | `AGENTS.md`, `docs/planning/AGENTS.md` |
| Workflow | `docs/CODEX-WORKFLOW.md` |
| Product goal | `docs/planning/strategy/product-goal.md` |
| Roadmap | `docs/planning/strategy/roadmap.md` |
| MVP scope | `docs/planning/releases/MVP.md` |
| DoR / DoD | `docs/_governance/dor.md`, `docs/_governance/dod.md` |
| Active initiative | `docs/planning/initiatives/INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review.md` |
| Parent artifact gate | `docs/planning/initiatives/INIT-2026Q3-ai-command-artifact-gate.md` |
| Research baseline | `docs/research/ai-command-capabilities/README.md`, `docs/research/ai-command-capabilities/recommendation.md`, `docs/research/ai-command-capabilities/external-research-comparison-2026-06-15.md` |
| Artifact gate package | `docs/research/ai-command-capabilities/domain-planner-v1-gate/**` |
| Current commands contract | `docs/contracts/http/commands.openapi.yaml` |
| Integration drift references | `docs/integration/ai-platform/**` |

### Provider Read-Only Evidence

Provider repository path inspected read-only:

```text
C:/Users/user/Documents/projects/VR_AI_Platform
```

Provider revision inspected:

```text
b1ca7235dfacb1faee35e042d6a072976c640d35
```

Provider files inspected:

- `docs/planning/initiatives/INIT-2026Q3-domain-planner-v1-narrow-household-command-corridor.md`
- `docs/planning/initiatives/INIT-2026Q3-domain-planner-v1-narrow-household-command-corridor.execution.md`
- `docs/planning/epics/EP-016/domain-planner-v1-closure-handoff.md`
- `docs/planning/epics/EP-016/domain-planner-v1-provider-mapping.md`
- `docs/planning/workpacks/ST-048/review-report.md`
- `docs/planning/workpacks/ST-049/review-report.md`
- `docs/planning/workpacks/ST-049/local-seed-eval-report.json`
- `docs/planning/workpacks/ST-050/review-report.md`
- `docs/planning/workpacks/ST-051/review-report.md`
- `docs/adr/ADR-009-domain-planner-v1-narrow-corridor.md`
- `docs/guides/domain-planner-v1-privacy-retention.md`
- `contracts/schemas/command.schema.json`
- `contracts/schemas/decision.schema.json`
- `scripts/evaluate_domain_planner_seed.py`
- `tests/test_domain_planner_v1_corridor.py`
- `graphs/core_graph.py`
- `routers/v2.py`

## Triage Classification

| Field | Value |
| --- | --- |
| Change type | docs/process/product acceptance |
| Planning level | initiative acceptance gate |
| Runtime impact | none |
| Contract impact | none in this initiative; future contract work likely |
| Data impact | no runtime data path change; privacy posture reviewed |
| Security sensitive | yes |
| Traceability critical | yes |
| Cross-repo | yes; provider read-only |

## Flags

| Flag | Value | Rationale |
| --- | --- | --- |
| `contract_impact` | `no` | This initiative records a decision only; no OpenAPI/schema changes. |
| `adr_needed` | `no` | Existing provider ADR is reviewed; no HomeTusk ADR is changed. |
| `diagrams_needed` | `no` | Existing provider diagram can be referenced; no HomeTusk diagram is changed. |
| `security_sensitive` | `yes` | Provider prompt/response retention, raw text handling, household context, and AI execution boundaries are reviewed. |
| `traceability_critical` | `yes` | Acceptance depends on provider trace/version/eval evidence and HomeTusk audit expectations. |
| `backend_impact` | `no` | No Java/backend work. |
| `mobile_impact` | `no` | No mobile UI/API work. |
| `ai_platform_impact` | `no` | Provider repository is read-only for this initiative. |
| `cross_repo` | `yes` | HomeTusk writes acceptance artifacts; AI Platform is evidence only. |
| `data_impact` | `no now` | No production data path changes; privacy HOLD items remain for later work. |

## Gate A Decision - GO

**Decision:** GO for HomeTusk acceptance-review scope.

**Evidence:**

- Roadmap now selects `INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review` as the current NOW focus.
- The initiative scope is docs-only and explicitly excludes Java/backend, TypeScript/mobile, OpenAPI, provider schema, provider code, prompt tuning, rollout, and direct mobile-to-AI-Platform calls.
- Provider evidence is available read-only in `C:/Users/user/Documents/projects/VR_AI_Platform`.
- Parent HomeTusk artifact gate is closed with LIMITED-GO and defines the accepted taxonomy, eval rubric, privacy questions, and 50-scenario threshold.

**Rationale:**

The initiative is source-backed, bounded, and required before any future HomeTusk `natural_command`, Mobile AI Command UX, provider contract follow-up, or runtime APPLY.

**Risks accepted:**

- Provider seed evidence covers only 10 scenarios.
- Provider current-schema `reject` mapping is semantically rough.
- Provider has no first-class `confirm` or `answer`.
- Product-owned expanded scenarios still need to be created and later run by provider.

## Gate B Decision - GO

**Decision:** GO for committed docs-only acceptance package.

**Committed files:**

```text
docs/planning/initiatives/INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review.execution.md
docs/planning/initiatives/INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review.md
docs/planning/strategy/roadmap.md
docs/research/ai-command-capabilities/provider-domain-planner-v1-acceptance/
  README.md
  provider-evidence-review.md
  provider-eval-evidence-index.md
  expanded-golden-scenarios-v1/
    README.md
    context-fixtures-v1.yaml
    golden-scenarios-v1.yaml
  reject-confirm-answer-contract-posture.md
  natural-command-readiness-decision.md
  recommendation.md
```

**Evidence:**

- Expected artifacts are listed in the initiative.
- DoR/DoD requirements for runtime tests do not apply because this is docs-only; verification will be artifact existence, YAML parse/count checks, grep checks for scope boundaries, and diff hygiene.
- Provider evidence includes closure handoff, ST-048 through ST-051 review reports, eval JSON, provider ADR/privacy posture, and schema/code evidence.

**Rationale:**

The package is small-batch, demonstrable, and reversible. It creates HomeTusk-owned acceptance assets without changing behavior.

## Codex PLAN

1. Create the execution notes with Gate A/B/C decisions and approved file list.
2. Create the acceptance package under `docs/research/ai-command-capabilities/provider-domain-planner-v1-acceptance/`.
3. Summarize provider evidence without copying sensitive logs or raw provider debug output.
4. Index provider evidence with trust level, sufficiency, and unresolved questions.
5. Expand HomeTusk-owned machine-readable scenarios from 10 seed scenarios to at least 50 cases.
6. Record contract posture for `reject`, `confirm`, `answer`, and repeated singular shopping actions.
7. Record natural command readiness decision and final recommendation.
8. Update roadmap and initiative status after the final decision.
9. Run read-only review gate and record Gate D.

## Gate C Decision - GO

**Decision:** GO for docs-only APPLY limited to the committed files above.

**Evidence:**

- PLAN is decision-complete and names exact files.
- No runtime, contract, backend, mobile, provider, or upstream snapshot files are approved.
- Provider repo remains read-only.
- Stop conditions are explicit below.

**Rationale:**

Human gates are delegated for this initiative. APPLY is limited to planning/research artifacts and does not alter external behavior.

## Stop Conditions

Stop and switch to HOLD if any of the following becomes necessary:

- Modify `docs/contracts/**`, `docs/integration/ai-platform/v1/upstream/**`, Java, TypeScript, mobile, provider files, or runtime schemas.
- Treat provider Gate D as automatic HomeTusk product acceptance.
- Broaden recommendation beyond evidence-supported LIMITED-GO without passing the expanded 50-scenario suite.
- Copy raw provider logs or sensitive text into HomeTusk.
- Approve `natural_command`, `needs_confirmation`, `answered`, direct mobile-to-AI-Platform calls, or production rollout.

## Gate D Decision

**Decision:** GO for docs-only initiative closure with final recommendation
**LIMITED-GO**.

**Review result:** GO.

### Must-fix

None.

### Should-fix / Follow-up

- Provider must run the expanded 50-scenario suite before any HomeTusk runtime
  acceptance claim.
- First-class `reject` and non-executing `confirm` require provider contract
  governance before HomeTusk runtime integration.
- `answer` remains blocked until HomeTusk defines a grounded read-only answer
  contract.
- Provider prompt/response retention remains HOLD for any future external LLM
  or raw text retention path.

### Evidence

Expected artifact package exists:

- `docs/research/ai-command-capabilities/provider-domain-planner-v1-acceptance/README.md`
- `docs/research/ai-command-capabilities/provider-domain-planner-v1-acceptance/provider-evidence-review.md`
- `docs/research/ai-command-capabilities/provider-domain-planner-v1-acceptance/provider-eval-evidence-index.md`
- `docs/research/ai-command-capabilities/provider-domain-planner-v1-acceptance/expanded-golden-scenarios-v1/README.md`
- `docs/research/ai-command-capabilities/provider-domain-planner-v1-acceptance/expanded-golden-scenarios-v1/context-fixtures-v1.yaml`
- `docs/research/ai-command-capabilities/provider-domain-planner-v1-acceptance/expanded-golden-scenarios-v1/golden-scenarios-v1.yaml`
- `docs/research/ai-command-capabilities/provider-domain-planner-v1-acceptance/reject-confirm-answer-contract-posture.md`
- `docs/research/ai-command-capabilities/provider-domain-planner-v1-acceptance/natural-command-readiness-decision.md`
- `docs/research/ai-command-capabilities/provider-domain-planner-v1-acceptance/recommendation.md`

Verification evidence:

- YAML parse passed for `context-fixtures-v1.yaml` and
  `golden-scenarios-v1.yaml`.
- Expanded suite has 6 context fixtures.
- Expanded suite has 50 scenarios, 50 unique scenario ids, and declared
  `scenario_count: 50`.
- Every scenario has the required acceptance fields:
  `id`, `input_text`, `locale`, `timezone`, `reference_instant`,
  `context_fixture_id`, `expected_intent`, `expected_entities`,
  `expected_decision_outcome`, `expected_actions`, `expected_clarify`,
  `expected_confirm`, `forbidden_assumptions`, `hometusk_responsibility`,
  `ai_platform_responsibility`, `failure_modes`, and `ux_recommendation`.
- Coverage tags include all initiative-required areas:
  simple task creation, multi-item shopping, quantity/unit shopping,
  list/source ambiguity, Russian colloquial phrasing, ASR noise, ambiguous
  references, non-requester assignment, task-shopping linkage, reschedule,
  completion, status/query, unsafe batch, cross-household/unverifiable, and
  unsupported commands.
- `git diff --check` passed with line-ending warnings only.
- Provider repository status remained clean during HomeTusk review:
  `git -C C:/Users/user/Documents/projects/VR_AI_Platform status --short`
  returned no changes.

### Rationale

The initiative exit criteria are satisfied:

1. Provider closure evidence is reviewed and summarized.
2. Provider evidence is indexed with trust level and unresolved questions.
3. HomeTusk golden scenarios are expanded to 50 machine-readable cases.
4. Contract posture for `reject`, `confirm`, `answer`, and plural shopping is
   documented.
5. Natural command readiness is documented.
6. Final decision is explicit: LIMITED-GO.
7. Runtime/backend/mobile/OpenAPI/provider files were not changed.
8. Next recommended action is explicit: provider contract + 50-scenario eval
   workpack.

### Recommendation

Close this HomeTusk acceptance-review initiative as **LIMITED-GO**. Start a
separate provider-side contract/eval follow-up before any HomeTusk runtime or
mobile work.
