# Initiative: INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review — AI Provider Domain Planner v1 Evidence Review

## Status

Completed - HomeTusk acceptance review closed with **LIMITED-GO** on
2026-06-15.

Gate posture:

- Human Gate A: GO, selected as HomeTusk evidence review focus.
- Human Gate B: GO, committed docs-only acceptance package scope.
- Human Gate C: GO, delegated approval for docs-only APPLY.
- Human Gate D: GO, acceptance package accepted with LIMITED-GO.
- Provider repository was inspected read-only.
- No runtime/backend/mobile/OpenAPI/AI Platform changes are approved.
- Final evidence and decisions are recorded in
  `docs/planning/initiatives/INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review.execution.md`
  and
  `docs/research/ai-command-capabilities/provider-domain-planner-v1-acceptance/**`.

## Initiative type

Product Acceptance Gate / Cross-repo Evidence Review / Scenario Expansion / Contract Posture Decision / AI Safety

## Owner

HomeTusk product engineering team.

## Target milestone

Before any HomeTusk `natural_command` runtime implementation, Mobile AI Command UX, or provider contract change work.

## Parent / Related initiatives

- HomeTusk discovery baseline: `docs/planning/initiatives/INIT-2026Q3-ai-command-capability-audit.md`
- HomeTusk artifact gate: `docs/planning/initiatives/INIT-2026Q3-ai-command-artifact-gate.md`
- HomeTusk artifact package: `docs/research/ai-command-capabilities/domain-planner-v1-gate/**`
- Provider initiative to review: `vr_ai_platform/docs/planning/initiatives/INIT-2026Q3-domain-planner-v1-narrow-household-command-corridor.md`
- Provider closure handoff to review: `vr_ai_platform/docs/planning/epics/EP-016/domain-planner-v1-closure-handoff.md`
- Future candidate: Provider contract workpack for first-class `reject` / `confirm` / `answer`
- Future candidate: HomeTusk `natural_command` contract implementation
- Future candidate: Mobile AI Command UX v1

---

## Sources of Truth

### HomeTusk canonical sources

- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Commands contract: `docs/contracts/http/commands.openapi.yaml`
- AI Platform integration docs: `docs/integration/ai-platform/**`
- AI Command Capability Audit: `docs/research/ai-command-capabilities/**`
- Artifact gate package: `docs/research/ai-command-capabilities/domain-planner-v1-gate/**`

### Provider read-only sources

These files live in `vr_ai_platform` and must be inspected read-only:

- `docs/planning/initiatives/INIT-2026Q3-domain-planner-v1-narrow-household-command-corridor.md`
- `docs/planning/initiatives/INIT-2026Q3-domain-planner-v1-narrow-household-command-corridor.execution.md`
- `docs/planning/epics/EP-016/domain-planner-v1-closure-handoff.md`
- `docs/planning/workpacks/ST-048/review-report.md`
- `docs/planning/workpacks/ST-049/local-seed-eval-report.json`
- `docs/planning/workpacks/ST-050/review-report.md`
- `docs/planning/workpacks/ST-051/review-report.md`
- `docs/adr/ADR-009-domain-planner-v1-narrow-corridor.md`
- `docs/guides/domain-planner-v1-privacy-retention.md`
- `scripts/evaluate_domain_planner_seed.py`
- `tests/test_domain_planner_v1_corridor.py`
- `graphs/core_graph.py`
- `routers/v2.py`

---

## 1. Problem / Opportunity

AI Platform completed the provider-side Domain Planner v1 narrow household command corridor.

Provider closure indicates:

- simple `create_task` decisions are schema-valid;
- multi-item shopping decisions are schema-valid through repeated `propose_add_shopping_item` actions;
- unsupported, ambiguous, confirm-required, contextual, or unsafe scenarios do not auto-execute;
- ASR remains transcription-only;
- no HomeTusk runtime, backend, mobile, OpenAPI, integration, or direct client path was changed.

However, HomeTusk product acceptance is explicitly separate.

Provider-side closure does **not** automatically approve:

- HomeTusk `natural_command` runtime implementation;
- Mobile AI Command UX;
- production rollout;
- first-class `reject`, `confirm`, or `answer` semantics;
- broad mixed task/shopping autonomous planning.

Known residual concerns remain:

- provider used current-schema mapping for `reject` as `status=error`, `action=clarify`;
- provider has no first-class `confirm`;
- provider has no first-class `answer`;
- provider has no direct plural `add_shopping_items` action enum;
- seed eval has only 10 scenarios;
- provider evidence reported non-blocker buckets such as wrong intent and item-boundary loss where the outcome was safe/no-execute;
- production privacy/retention posture is still incomplete for future external LLM or raw text retention scenarios.

This initiative is the HomeTusk-side product acceptance gate for the provider evidence.

---

## 2. Outcome

The initiative produces a HomeTusk acceptance decision for the provider-side Domain Planner v1 evidence.

The final outcome must be one of:

```text
GO          — provider evidence is accepted as sufficient input for a HomeTusk follow-up.
LIMITED-GO  — provider evidence is accepted only for a narrower next step.
NO-GO       — provider evidence is insufficient; provider must rework before HomeTusk proceeds.
HOLD        — missing evidence blocks the decision.
```

The initiative must also decide the next HomeTusk move:

- provider contract workpack first;
- HomeTusk `natural_command` contract spike first;
- scenario expansion first;
- or no runtime work until additional provider evidence exists.

---

## 3. Scope (Now / Next / Later)

### NOW — HomeTusk evidence review and acceptance gate

#### Provider evidence review

Review and summarize provider-side closure evidence:

- initiative status and gate decisions;
- runtime changes and non-changes;
- tests and eval outputs;
- seed eval metrics;
- non-blocker failure buckets;
- privacy/retention posture;
- contract/schema changes or absence thereof;
- ASR boundary preservation;
- HomeTusk non-impact evidence.

#### Provider eval evidence index

Create a HomeTusk-owned index of provider evidence.

Must include:

- provider artifact path;
- provider revision or import date if available;
- evidence type;
- trust level;
- whether it is sufficient for HomeTusk acceptance;
- unresolved questions.

#### Scenario expansion to at least 50 cases

Expand HomeTusk product-owned golden scenarios from the seed set to at least 50 machine-readable scenarios.

The scenario expansion must cover at minimum:

- simple task creation variations;
- multi-item shopping variations;
- shopping item quantity/unit variations;
- shopping list/source ambiguity;
- Russian colloquial phrasing;
- ASR-like noisy transcript variants;
- ambiguous member/list/task references;
- non-requester assignment;
- task + shopping linkage;
- reschedule requests;
- completion requests;
- status/query commands;
- unsafe batch assignment;
- cross-household / unverifiable references;
- unsupported commands.

The expanded suite is a HomeTusk acceptance asset. It does not require provider-side implementation in this initiative.

#### Contract posture decision: `reject`, `confirm`, `answer`

Decide whether HomeTusk can proceed with current-schema provider mapping or whether provider contract work is required first.

At minimum, decide:

- Is `reject_mapped_to_error` acceptable for HomeTusk `natural_command` v0?
- Is first-class `reject` required before runtime integration?
- Is first-class `confirm` required before assignment, reschedule, or task + shopping linkage?
- Is first-class `answer` required before status/query commands?
- Is direct plural `add_shopping_items` required or can repeated singular actions remain acceptable?

#### Natural command readiness decision

Produce a recommendation for HomeTusk `natural_command` next step:

- GO / LIMITED-GO / NO-GO / HOLD for contract spike;
- allowed v0 corridor;
- blocked scenarios;
- required provider work first, if any;
- required HomeTusk contract artifacts;
- required mobile readiness artifacts.

#### Roadmap / planning update

Update roadmap and/or planning docs if needed to reflect:

- provider-side Domain Planner v1 evidence reviewed;
- HomeTusk acceptance decision;
- next selected candidate, if known.

---

### NEXT — Follow-up based on acceptance decision

Potential follow-ups:

1. Provider contract workpack for first-class `reject`, `confirm`, and/or `answer`.
2. HomeTusk `natural_command` contract spike.
3. HomeTusk integration-doc cleanup for AI Platform mapping drift.
4. Mobile AI Command UX state/card design.
5. Additional provider planner work against 50-scenario suite.

### LATER

- Mobile AI Command UX v1.
- Voice command on mobile through editable transcript.
- Mixed task + shopping linked planning.
- Assignment/fairness recommendation.
- Reschedule/completion natural command support.
- Read-only answer/status cards.
- Broader planner or agent work only if scenario evidence justifies it.

---

## 4. In Scope

- Docs-only HomeTusk acceptance gate.
- Read-only provider evidence review.
- HomeTusk evidence index.
- Expanded HomeTusk golden scenario fixtures, minimum 50 scenarios.
- Contract posture decision for `reject`, `confirm`, `answer`, and plural shopping semantics.
- Natural command readiness recommendation.
- Roadmap/planning updates if needed.

---

## 5. Out of Scope

- No Java/backend implementation.
- No TypeScript/mobile implementation.
- No OpenAPI changes.
- No HomeTusk runtime contract changes.
- No AI Platform changes.
- No provider contract/schema changes.
- No mobile AI UX.
- No `natural_command` runtime implementation.
- No production rollout.
- No prompt tuning.
- No direct mobile to AI Platform calls.
- No broad multi-agent architecture.

---

## 6. Cross-repo ownership

This initiative is HomeTusk-owned and committed in the HomeTusk repository.

AI Platform repository is read-only context.

Codex must not:

- modify `vr_ai_platform` files;
- create commits in `vr_ai_platform`;
- reformat provider files;
- copy raw provider logs with sensitive text into HomeTusk;
- treat provider-side tests as sufficient for HomeTusk product acceptance.

HomeTusk owns:

- product acceptance decision;
- expanded product golden scenarios;
- contract posture decision;
- natural command readiness recommendation;
- final GO / LIMITED-GO / NO-GO / HOLD.

AI Platform owns:

- provider implementation;
- provider schema/runtime changes;
- provider-side eval runner;
- provider privacy/retention posture.

---

## 7. Assumptions

- Provider initiative is closed and evidence is available in `vr_ai_platform`.
- Provider did not change HomeTusk files.
- Provider did not change contracts/schemas/public API unless documented otherwise.
- Provider seed eval shows zero blocker failures on 10 seed scenarios.
- HomeTusk still requires broader product-owned coverage before runtime acceptance.
- HomeTusk remains final execution authority.
- AI Platform remains external planner/provider.

---

## 8. Success Metrics

This initiative succeeds when:

1. Provider closure evidence is reviewed and summarized.
2. Provider evidence is indexed with trust level and unresolved questions.
3. HomeTusk golden scenarios are expanded to at least 50 machine-readable cases.
4. Contract posture for `reject`, `confirm`, `answer`, and plural shopping is documented.
5. Natural command readiness decision is documented.
6. Final recommendation is explicit: GO / LIMITED-GO / NO-GO / HOLD.
7. No runtime/backend/mobile/OpenAPI/provider files are changed.
8. Next initiative recommendation is clear.

---

## 9. Constraints / Guardrails

- Evidence over demo behavior.
- Do not equate provider Gate D with HomeTusk product acceptance.
- Do not use confidence as execution permission.
- Prefer clarify/confirm over guessing.
- Keep HomeTusk as execution authority.
- Keep AI Platform as planner/provider.
- Keep mobile/web as HomeTusk clients, not AI Platform clients.
- Do not broaden beyond LIMITED-GO without scenario evidence.
- Scenario expansion must include negative, ambiguous, unsafe, and non-happy paths.

---

## 10. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Provider evidence is accepted too broadly | HIGH | Explicit HomeTusk acceptance gate and contract posture decision |
| 10 seed scenarios overfit provider behavior | HIGH | Expand to at least 50 product-owned scenarios |
| Current-schema reject workaround leaks into product UX | HIGH | Decide first-class reject requirement before runtime work |
| Missing confirm blocks safe mixed planning | HIGH | Decide confirm requirement before assignment/linkage/reschedule |
| Natural command starts before scenario coverage | HIGH | Gate natural command readiness on expanded scenarios |
| Privacy HOLD items are ignored | HIGH | Evidence review must include privacy/retention posture |
| Cross-repo commit confusion | MEDIUM | HomeTusk write-only, provider read-only |

---

## 11. Expected artifacts

Create:

```text
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

Optional after Gate A:

```text
docs/planning/initiatives/INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review.execution.md
```

---

## 12. Exit Criteria

The initiative is complete when:

1. All expected artifacts exist.
2. Provider evidence review is complete.
3. Expanded scenario fixture contains at least 50 scenarios.
4. Contract posture decision is explicit.
5. Natural command readiness recommendation is explicit.
6. Final decision is recorded as GO / LIMITED-GO / NO-GO / HOLD.
7. No runtime/backend/mobile/OpenAPI/AI Platform files are changed.
8. Next recommended initiative is explicit.

---

## 13. Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | Decision only; future work may have contract impact |
| adr_needed | no | May recommend future ADR/contract work |
| diagrams_needed | no | Existing provider diagram can be referenced |
| security_sensitive | yes | AI command execution, provider logs, household context |
| traceability_critical | yes | Provider evidence and scenario expansion drive acceptance |
| backend_impact | no | Docs-only |
| mobile_impact | no | No UI changes |
| ai_platform_impact | no | Provider repo read-only |
| cross_repo | yes | HomeTusk write, AI Platform read-only |
| data_impact | no now | Privacy decision only; no runtime data path change |

---

## 14. Anti-Scope-Creep

DO NOT:

- implement HomeTusk `natural_command`;
- modify `/commands`;
- add `needs_confirmation`;
- add `answered`;
- modify OpenAPI;
- modify Java/TypeScript runtime;
- modify mobile UI;
- modify AI Platform;
- tune prompts;
- treat provider evidence as product acceptance without review;
- broaden to full GO without scenario evidence;
- bypass HomeTusk guardrails;
- permit direct mobile to AI Platform calls.

---

## 15. Next Step After Gate A

Codex should receive a planning prompt:

- read this initiative;
- read HomeTusk artifact gate package;
- inspect provider closure evidence read-only;
- propose docs-only execution plan;
- identify exact files to create/update;
- keep runtime code and contracts unchanged;
- produce PLAN first;
- do not APPLY before Gate C.
